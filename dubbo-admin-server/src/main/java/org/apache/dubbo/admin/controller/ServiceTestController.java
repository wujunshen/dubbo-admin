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

package org.apache.dubbo.admin.controller;

import com.google.gson.Gson;
import org.apache.dubbo.admin.annotation.Authority;
import org.apache.dubbo.admin.common.utils.Constants;
import org.apache.dubbo.admin.common.utils.ConvertUtils;
import org.apache.dubbo.admin.common.utils.ServiceTestUtils;
import org.apache.dubbo.admin.model.domain.MethodMetadata;
import org.apache.dubbo.admin.model.dto.ServiceTestDTO;
import org.apache.dubbo.admin.service.ProviderService;
import org.apache.dubbo.admin.service.impl.GenericServiceImpl;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/** @author wujunshen */
@Authority(needLogin = true)
@RestController
@RequestMapping("/api/{env}/test")
public class ServiceTestController {
  @Resource private GenericServiceImpl genericService;
  @Resource private ProviderService providerService;

  @PostMapping
  public Object test(@RequestBody ServiceTestDTO serviceTestDTO) {
    return genericService.invoke(
        serviceTestDTO.getService(),
        serviceTestDTO.getMethod(),
        serviceTestDTO.getParameterTypes(),
        serviceTestDTO.getParams());
  }

  @GetMapping(value = "/method")
  public MethodMetadata methodDetail(
      @RequestParam String application, @RequestParam String service, @RequestParam String method) {
    Map<String, String> info = ConvertUtils.serviceName2Map(service);
    MetadataIdentifier identifier =
        new MetadataIdentifier(
            info.get(Constants.INTERFACE_KEY),
            info.get(Constants.VERSION_KEY),
            info.get(Constants.GROUP_KEY),
            Constants.PROVIDER_SIDE,
            application);
    String metadata = providerService.getProviderMetaData(identifier);
    MethodMetadata methodMetadata = null;
    if (metadata != null) {
      Gson gson = new Gson();
      FullServiceDefinition serviceDefinition =
          gson.fromJson(metadata, FullServiceDefinition.class);
      List<MethodDefinition> methods = serviceDefinition.getMethods();
      if (methods != null) {
        for (MethodDefinition m : methods) {
          if (ServiceTestUtils.sameMethod(m, method)) {
            methodMetadata = ServiceTestUtils.generateMethodMeta(serviceDefinition, m);
            break;
          }
        }
      }
    }
    return methodMetadata;
  }
}
