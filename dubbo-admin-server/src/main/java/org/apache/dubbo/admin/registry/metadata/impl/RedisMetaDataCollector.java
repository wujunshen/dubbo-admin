/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.admin.registry.metadata.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.dubbo.admin.registry.metadata.MetaDataCollector;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.rpc.RpcException;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.*;

/** @author wujunshen */
@Slf4j
public class RedisMetaDataCollector implements MetaDataCollector {
  private static final String META_DATA_STORE_TAG = ".metaData";
  Set<HostAndPort> jedisClusterNodes;
  private URL url;
  private JedisPool pool;
  private int timeout;
  private String password;

  @Override
  public URL getUrl() {
    return url;
  }

  @Override
  public void setUrl(URL url) {
    this.url = url;
  }

  @Override
  public void init() {
    timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
    password = url.getPassword();
    if (url.getParameter(CLUSTER_KEY, false)) {
      jedisClusterNodes = new HashSet<>();
      String[] addresses = COMMA_SPLIT_PATTERN.split(url.getAddress());
      for (String address : addresses) {
        URL tmpUrl = url.setAddress(address);
        jedisClusterNodes.add(new HostAndPort(tmpUrl.getHost(), tmpUrl.getPort()));
      }
    } else {
      pool =
          new JedisPool(
              new JedisPoolConfig(), url.getHost(), url.getPort(), timeout, url.getPassword());
    }
  }

  @Override
  public String getProviderMetaData(MetadataIdentifier key) {
    return doGetMetaData(key);
  }

  @Override
  public String getConsumerMetaData(MetadataIdentifier key) {
    return doGetMetaData(key);
  }

  private String doGetMetaData(MetadataIdentifier identifier) {
    String result = null;
    if (url.getParameter(CLUSTER_KEY, false)) {
      try (JedisCluster jedisCluster =
          new JedisCluster(
              jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig())) {
        result =
            jedisCluster.get(
                identifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY)
                    + MetadataIdentifier.META_DATA_STORE_TAG);
      } catch (Throwable e) {
        log.error(
            "Failed to get " + identifier + " from redis cluster, cause: " + e.getMessage(), e);
        throw new RpcException(
            "Failed to get " + identifier + " from redis cluster, cause: " + e.getMessage(), e);
      }
    } else {
      try (Jedis jedis = pool.getResource()) {
        result =
            jedis.get(
                identifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY)
                    + META_DATA_STORE_TAG);
      } catch (Throwable e) {
        log.error("Failed to get " + identifier + " from redis, cause: " + e.getMessage(), e);
        throw new RpcException(
            "Failed to get " + identifier + " from redis, cause: " + e.getMessage(), e);
      }
    }
    return result;
  }
}
