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
package org.apache.dubbo.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.admin.common.utils.CoderUtils;
import org.apache.dubbo.admin.common.utils.Constants;
import org.apache.dubbo.admin.common.utils.Tool;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** @author wujunshen */
@Slf4j
@Component
public class RegistryServerSync implements InitializingBean, DisposableBean, NotifyListener {
  private static final URL SUBSCRIBE =
      new URL(
          Constants.ADMIN_PROTOCOL,
          NetUtils.getLocalHost(),
          0,
          "",
          Constants.INTERFACE_KEY,
          Constants.ANY_VALUE,
          Constants.GROUP_KEY,
          Constants.ANY_VALUE,
          Constants.VERSION_KEY,
          Constants.ANY_VALUE,
          Constants.CLASSIFIER_KEY,
          Constants.ANY_VALUE,
          Constants.CATEGORY_KEY,
          Constants.PROVIDERS_CATEGORY
              + ","
              + Constants.CONSUMERS_CATEGORY
              + ","
              + Constants.ROUTERS_CATEGORY
              + ","
              + Constants.CONFIGURATORS_CATEGORY,
          Constants.ENABLED_KEY,
          Constants.ANY_VALUE,
          Constants.CHECK_KEY,
          String.valueOf(false));

  /** Make sure ID never changed when the same url notified many times */
  private final ConcurrentHashMap<String, String> urlIdsMapper = new ConcurrentHashMap<>();

  /** ConcurrentMap<category, ConcurrentMap<serviceName, Map<MD5, URL>>> registryCache */
  private final ConcurrentMap<String, ConcurrentMap<String, Map<String, URL>>> registryCache =
      new ConcurrentHashMap<>();

  @Resource private Registry registry;

  public ConcurrentMap<String, ConcurrentMap<String, Map<String, URL>>> getRegistryCache() {
    return registryCache;
  }

  @Override
  public void afterPropertiesSet() {
    log.info("Init Dubbo Admin Sync Cache...");
    registry.subscribe(SUBSCRIBE, this);
  }

  @Override
  public void destroy() {
    registry.unsubscribe(SUBSCRIBE, this);
  }

  /**
   * Notification of of any service with any type (override、subscribe、route、provider) is full.
   *
   * @param urls
   */
  @Override
  public void notify(List<URL> urls) {
    if (CollectionUtils.isEmpty(urls)) {
      return;
    }
    // Map<category, Map<serviceName, Map<Long, URL>>>
    final Map<String, Map<String, Map<String, URL>>> categories = new ConcurrentHashMap<>(8);
    String interfaceName = null;
    for (URL url : urls) {
      interfaceName = handlerInUrls(categories, interfaceName, url);
    }
    if (categories.isEmpty()) {
      return;
    }
    final String name = interfaceName;
    categories.entrySet().forEach(categoryEntry -> putAllServices(name, categoryEntry));
  }

  private void putAllServices(
      String interfaceName, Map.Entry<String, Map<String, Map<String, URL>>> categoryEntry) {
    String category = categoryEntry.getKey();
    ConcurrentMap<String, Map<String, URL>> services = registryCache.get(category);
    if (services == null) {
      services = new ConcurrentHashMap<>(8);
      registryCache.put(category, services);
    } else {
      // Fix map can not be cleared when service is unregistered: when a unique
      // “group/service:version” service is unregistered, but we still have the same
      // services with different version or group, so empty protocols can not be invoked.
      Set<String> keys = new HashSet<>(services.keySet());
      for (String key : keys) {
        if (Tool.getInterface(key).equals(interfaceName)
            && !categoryEntry.getValue().entrySet().contains(key)) {
          services.remove(key);
        }
      }
    }
    services.putAll(categoryEntry.getValue());
  }

  private String handlerInUrls(
      Map<String, Map<String, Map<String, URL>>> categories, String interfaceName, URL url) {
    String category = url.getParameter(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
    // NOTE: group and version in empty protocol is *
    if (Constants.EMPTY_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
      ConcurrentMap<String, Map<String, URL>> services = registryCache.get(category);
      if (services != null) {
        String group = url.getParameter(Constants.GROUP_KEY);
        String version = url.getParameter(Constants.VERSION_KEY);
        // NOTE: group and version in empty protocol is *
        if (!Constants.ANY_VALUE.equals(group) && !Constants.ANY_VALUE.equals(version)) {
          services.remove(url.getServiceKey());
        } else {
          for (Map.Entry<String, Map<String, URL>> serviceEntry : services.entrySet()) {
            String service = serviceEntry.getKey();
            if (Tool.getInterface(service).equals(url.getServiceInterface())
                && (Constants.ANY_VALUE.equals(group)
                    || StringUtils.isEquals(group, Tool.getGroup(service)))
                && (Constants.ANY_VALUE.equals(version)
                    || StringUtils.isEquals(version, Tool.getVersion(service)))) {
              services.remove(service);
            }
          }
        }
      }
    } else {
      if (StringUtils.isEmpty(interfaceName)) {
        interfaceName = url.getServiceInterface();
      }
      Map<String, Map<String, URL>> services = categories.get(category);
      if (services == null) {
        services = new ConcurrentHashMap<>(8);
        categories.put(category, services);
      }
      String service = url.getServiceKey();
      Map<String, URL> ids = services.get(service);
      if (ids == null) {
        ids = new ConcurrentHashMap<>(8);
        services.put(service, ids);
      }

      // Make sure we use the same ID for the same URL
      if (urlIdsMapper.containsKey(url.toFullString())) {
        ids.put(urlIdsMapper.get(url.toFullString()), url);
      } else {
        String md5 = CoderUtils.md516Bit(url.toFullString());
        ids.put(md5, url);
        urlIdsMapper.putIfAbsent(url.toFullString(), md5);
      }
    }
    return interfaceName;
  }
}
