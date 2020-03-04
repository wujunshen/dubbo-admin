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
import org.apache.dubbo.admin.common.utils.ConvertUtils;
import org.apache.dubbo.common.URL;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.admin.common.utils.Constants.ENABLED;

/**
 * Provider
 *
 * @author wujunshen
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Provider extends AbstractEntity {
  private static final long serialVersionUID = 5981342400350878171L;

  /** The name of the service provided by the provider */
  private String service;
  /** Provider's address for service */
  private String url;
  /** Provider provides service parameters */
  private String parameters;
  /** Provider address */
  private String address;
  /** The provider's registry address */
  private String registry;
  /** provider was registered dynamically */
  private boolean dynamic;
  /** provider enabled or not */
  private Boolean enabled;
  /** provider weight */
  private int weight;
  /** application name */
  private String application;
  /** operator */
  private String username;
  /** time to expire */
  private Date expired;
  /** time to live in milliseconds */
  private long alived;

  private Override override;

  private List<Override> overrides;

  public Provider(Long id) {
    super(id);
  }

  public URL toUrl() {
    Map<String, String> serviceName2Map = ConvertUtils.serviceName2Map(getService());

    String u = getUrl();
    URL url = URL.valueOf(u + "?" + getParameters());

    url = url.addParameters(serviceName2Map);

    boolean dynamic = isDynamic();
    if (!dynamic) {
      url = url.addParameter(Constants.DYNAMIC_KEY, false);
    }
    boolean enabled = getEnabled();
    if (enabled != url.getParameter(ENABLED, true)) {
      if (enabled) {
        url = url.removeParameter(ENABLED);
      } else {
        url = url.addParameter(ENABLED, false);
      }
    }

    return url;
  }
}
