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
import org.apache.dubbo.admin.common.utils.Tool;
import org.apache.dubbo.admin.model.domain.Consumer;
import org.apache.dubbo.admin.model.domain.Provider;
import org.apache.dubbo.admin.model.dto.ServiceDTO;
import org.apache.dubbo.admin.model.dto.ServiceDetailDTO;
import org.apache.dubbo.admin.service.ConsumerService;
import org.apache.dubbo.admin.service.ProviderService;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** @author wujunshen */
@Authority(needLogin = true)
@RestController
@RequestMapping("/api/{env}")
public class ServiceController {
  @Resource private ProviderService providerService;
  @Resource private ConsumerService consumerService;
  private Gson gson;

  @GetMapping(value = "/service")
  public Page<ServiceDTO> searchService(
      @RequestParam String pattern,
      @RequestParam String filter,
      @PathVariable String env,
      Pageable pageable) {
    final Set<ServiceDTO> serviceDtoList = providerService.getServiceDtoList(pattern, filter, env);

    final int total = serviceDtoList.size();
    final List<ServiceDTO> content =
        serviceDtoList.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .collect(Collectors.toList());

    return new PageImpl<>(content, pageable, total);
  }

  @GetMapping(value = "/service/{service}")
  public ServiceDetailDTO serviceDetail(@PathVariable String service) {
    service = service.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
    String group = Tool.getGroup(service);
    String version = Tool.getVersion(service);
    String interfaze = Tool.getInterface(service);
    List<Provider> providers = providerService.findByService(service);

    List<Consumer> consumers = consumerService.findByService(service);

    String application = null;
    if (providers != null && !providers.isEmpty()) {
      application = providers.get(0).getApplication();
    }
    MetadataIdentifier identifier =
        new MetadataIdentifier(interfaze, version, group, Constants.PROVIDER_SIDE, application);
    String metadata = providerService.getProviderMetaData(identifier);
    ServiceDetailDTO serviceDetailDTO = new ServiceDetailDTO();
    serviceDetailDTO.setConsumers(consumers);
    serviceDetailDTO.setProviders(providers);
    if (metadata != null) {
      FullServiceDefinition serviceDefinition =
          gson.fromJson(metadata, FullServiceDefinition.class);
      serviceDetailDTO.setMetadata(serviceDefinition);
    }
    serviceDetailDTO.setConsumers(consumers);
    serviceDetailDTO.setProviders(providers);
    serviceDetailDTO.setService(service);
    serviceDetailDTO.setApplication(application);
    return serviceDetailDTO;
  }

  @GetMapping(value = "/services")
  public Set<String> allServices() {
    return new HashSet<>(providerService.findServices());
  }

  @GetMapping(value = "/applications")
  public Set<String> allApplications() {
    return providerService.findApplications();
  }
}
