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

import java.util.List;

/** @author wujunshen */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Route extends AbstractEntity {
  public static final String ALL_METHOD = "*";
  public static final String KEY_METHOD = "method";

  /** WHEN KEY */
  public static final String KEY_CONSUMER_APPLICATION = "consumer.application";

  public static final String KEY_CONSUMER_GROUP = "consumer.cluster";
  public static final String KEY_CONSUMER_VERSION = "consumer.version";
  public static final String KEY_CONSUMER_HOST = "host";
  public static final String KEY_CONSUMER_METHODS = "consumer.methods";
  public static final String KEY_PROVIDER_APPLICATION = "provider.application";
  /** THEN KEY */
  public static final String KEY_PROVIDER_GROUP = "provider.cluster";

  public static final String KEY_PROVIDER_PROTOCOL = "provider.protocol";
  public static final String KEY_PROVIDER_VERSION = "provider.version";
  public static final String KEY_PROVIDER_HOST = "provider.host";
  public static final String KEY_PROVIDER_PORT = "provider.port";
  private static final long serialVersionUID = -7630589008164140656L;
  /** default 0 */
  private long parentId;

  private String name;

  private String service;

  private String rule;

  private String matchRule;

  private String filterRule;

  private Integer priority;

  private String userName;

  private Boolean enabled;

  private Boolean force;

  private Boolean dynamic;

  private Boolean runtime;

  private List<Route> children;

  public void setRule(String rule) {
    this.rule = rule.trim();
    String[] rules = rule.split("=>");
    if (rules.length != 2) {
      if (rule.endsWith("=>")) {
        this.matchRule = rules[0].trim();
        this.filterRule = "";
      } else {
        throw new IllegalArgumentException("Illegal Route Condition Rule");
      }
    } else {
      this.matchRule = rules[0].trim();
      this.filterRule = rules[1].trim();
    }
  }

  public void setMatchRule(String matchRule) {
    if (matchRule != null) {
      this.matchRule = matchRule.trim();
    } else {
      this.matchRule = matchRule;
    }
  }

  public String getFilterRule() {
    return filterRule;
  }

  public void setFilterRule(String filterRule) {
    if (filterRule != null) {
      this.filterRule = filterRule.trim();
    } else {
      this.filterRule = filterRule;
    }
  }

  @java.lang.Override
  public String toString() {
    return "Route [parentId="
        + parentId
        + ", name="
        + name
        + ", serviceName="
        + service
        + ", matchRule="
        + matchRule
        + ", filterRule="
        + filterRule
        + ", priority="
        + priority
        + ", username="
        + userName
        + ", enabled="
        + enabled
        + "]";
  }

  public URL toUrl() {
    String group = Tool.getGroup(service);
    String version = Tool.getVersion(service);
    String interfaze = Tool.getInterface(service);
    return URL.valueOf(
        Constants.ROUTE_PROTOCOL
            + "://"
            + Constants.ANY_HOST_VALUE
            + "/"
            + interfaze
            + "?"
            + Constants.CATEGORY_KEY
            + "="
            + Constants.ROUTERS_CATEGORY
            + "&router=condition&runtime="
            + getRuntime()
            + "&enabled="
            + getEnabled()
            + "&priority="
            + getPriority()
            + "&force="
            + getForce()
            + "&dynamic="
            + getDynamic()
            + "&name="
            + getName()
            + "&"
            + Constants.RULE_KEY
            + "="
            + URL.encode(getMatchRule() + " => " + getFilterRule())
            + (group == null ? "" : "&" + Constants.GROUP_KEY + "=" + group)
            + (version == null ? "" : "&" + Constants.VERSION_KEY + "=" + version));
  }
}
