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

import java.util.Map;

/** @author wujunshen */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Override extends AbstractEntity {
  private static final long serialVersionUID = 114828505391757846L;

  private String service;

  private String params;

  private String application;

  private String address;

  private String username;

  private Boolean enabled;

  public Override(long id) {
    super(id);
  }

  @java.lang.Override
  public String toString() {
    return "Override [service="
        + service
        + ", params="
        + params
        + ", application="
        + application
        + ", address="
        + address
        + ", username="
        + username
        + ", enabled="
        + enabled
        + "]";
  }

  public boolean isDefault() {
    return (getAddress() == null
            || getAddress().length() == 0
            || Constants.ANY_VALUE.equals(getAddress())
            || Constants.ANY_HOST_VALUE.equals(getAddress()))
        && (getApplication() == null
            || getApplication().length() == 0
            || Constants.ANY_VALUE.equals(getApplication()));
  }

  public URL toUrl() {
    String group = Tool.getGroup(service);
    String version = Tool.getVersion(service);
    String interfaze = Tool.getInterface(service);
    StringBuilder sb = new StringBuilder();
    sb.append(Constants.OVERRIDE_PROTOCOL);
    sb.append("://");
    if (!StringUtils.isBlank(address) && !Constants.ANY_VALUE.equals(address)) {
      sb.append(address);
    } else {
      sb.append(Constants.ANY_HOST_VALUE);
    }
    sb.append("/");
    sb.append(interfaze);
    sb.append("?");
    Map<String, String> param = StringUtils.parseQueryString(params);
    param.put(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY);
    param.put(Constants.ENABLED_KEY, String.valueOf(getEnabled()));
    param.put(Constants.DYNAMIC_KEY, "false");
    if (!StringUtils.isBlank(application) && !Constants.ANY_VALUE.equals(application)) {
      param.put(Constants.APPLICATION_KEY, application);
    }
    if (group != null) {
      param.put(Constants.GROUP_KEY, group);
    }
    if (version != null) {
      param.put(Constants.VERSION_KEY, version);
    }
    sb.append(StringUtils.toQueryString(param));
    return URL.valueOf(sb.toString());
  }
}
