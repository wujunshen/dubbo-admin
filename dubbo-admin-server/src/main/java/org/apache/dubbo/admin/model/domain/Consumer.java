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
package org.apache.dubbo.admin.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.dubbo.admin.common.utils.Constants;
import org.apache.dubbo.admin.common.utils.Tool;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Consumer
 *
 * @author wujunshen
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Consumer extends AbstractEntity {
  private static final long serialVersionUID = -1140894843784583237L;
  /** The name of the service referenced by the consumer */
  private String service;

  private String parameters;
  /** route result */
  private String result;
  /** address of consumer */
  private String address;
  /** Consumer connected registry address */
  private String registry;
  /** application name */
  private String application;
  /** user name of consumer */
  private String username;
  /** Service call statistics */
  private String statistics;
  /** Date statistics was recorded */
  private Date collected;

  private Override override;

  private List<Override> overrides;

  private List<Route> conditionRoutes;

  private List<Provider> providers;

  private Date expired;
  /** Time to live in milliseconds */
  private long alived;

  public Consumer(Long id) {
    super(id);
  }

  @java.lang.Override
  public String toString() {
    return "Consumer [service="
        + service
        + ", parameters="
        + parameters
        + ", result="
        + result
        + ", address="
        + address
        + ", registry="
        + registry
        + ", application="
        + application
        + ", username="
        + username
        + ", statistics="
        + statistics
        + ", collected="
        + collected
        + ", conditionRoutes="
        + conditionRoutes
        + ", overrides="
        + overrides
        + ", expired="
        + expired
        + ", alived="
        + alived
        + "]";
  }

  public URL toUrl() {
    String group = Tool.getGroup(service);
    String version = Tool.getVersion(service);
    String interfaze = Tool.getInterface(service);
    Map<String, String> param = StringUtils.parseQueryString(parameters);
    param.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
    if (group != null) {
      param.put(Constants.GROUP_KEY, group);
    }
    if (version != null) {
      param.put(Constants.VERSION_KEY, version);
    }
    return URL.valueOf(
        Constants.CONSUMER_PROTOCOL
            + "://"
            + address
            + "/"
            + interfaze
            + "?"
            + StringUtils.toQueryString(param));
  }
}
