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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.admin.annotation.Authority;
import org.apache.dubbo.admin.common.exception.ParamValidationException;
import org.apache.dubbo.admin.common.exception.ResourceNotFoundException;
import org.apache.dubbo.admin.common.exception.VersionValidationException;
import org.apache.dubbo.admin.common.utils.Constants;
import org.apache.dubbo.admin.model.dto.AccessDTO;
import org.apache.dubbo.admin.model.dto.ConditionRouteDTO;
import org.apache.dubbo.admin.service.ProviderService;
import org.apache.dubbo.admin.service.RouteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.dubbo.admin.common.utils.Constants.OLD_DUBBO_VERSION;

/** @author wujunshen */
@Slf4j
@Authority(needLogin = true)
@RestController
@RequestMapping("/api/{env}/rules/access")
public class AccessesController {
  @Resource private RouteService routeService;
  @Resource private ProviderService providerService;

  @GetMapping
  public List<AccessDTO> searchAccess(
      @RequestParam(required = false) String service,
      @RequestParam(required = false) String application) {
    if (StringUtils.isBlank(service) && StringUtils.isBlank(application)) {
      throw new ParamValidationException("Either service or application is required");
    }
    List<AccessDTO> accessDtoList = new ArrayList<>();
    AccessDTO accessDTO;
    if (StringUtils.isNotBlank(application)) {
      accessDTO = routeService.findAccess(application);
    } else {
      accessDTO = routeService.findAccess(service);
    }
    if (accessDTO != null) {
      accessDTO.setEnabled(true);
      accessDtoList.add(accessDTO);
    }
    return accessDtoList;
  }

  @GetMapping(value = "/{id}")
  public AccessDTO detailAccess(@PathVariable String id) {
    return routeService.findAccess(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
  }

  @DeleteMapping(value = "/{id}")
  public void deleteAccess(@PathVariable String id) {
    routeService.deleteAccess(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void createAccess(@RequestBody AccessDTO accessDTO) {
    if (StringUtils.isBlank(accessDTO.getService())
        && StringUtils.isBlank(accessDTO.getApplication())) {
      throw new ParamValidationException("Either Service or application is required.");
    }
    String application = accessDTO.getApplication();
    if (StringUtils.isNotEmpty(application)
        && OLD_DUBBO_VERSION.equals(providerService.findVersionInApplication(application))) {
      throw new VersionValidationException(
          "dubbo 2.6 does not support application scope blackWhite list config");
    }
    if (accessDTO.getBlacklist() == null && accessDTO.getWhitelist() == null) {
      throw new ParamValidationException("One of Blacklist/Whitelist is required.");
    }
    routeService.createAccess(accessDTO);
  }

  @PutMapping(value = "/{id}")
  public void updateAccess(@PathVariable String id, @RequestBody AccessDTO accessDTO) {
    ConditionRouteDTO route =
        routeService.findConditionRoute(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    if (Objects.isNull(route)) {
      throw new ResourceNotFoundException("Unknown ID!");
    }
    routeService.updateAccess(accessDTO);
  }
}
