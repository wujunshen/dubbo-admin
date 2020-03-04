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

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.admin.annotation.Authority;
import org.apache.dubbo.admin.common.exception.ParamValidationException;
import org.apache.dubbo.admin.common.exception.ResourceNotFoundException;
import org.apache.dubbo.admin.common.exception.VersionValidationException;
import org.apache.dubbo.admin.common.utils.Constants;
import org.apache.dubbo.admin.model.dto.ConditionRouteDTO;
import org.apache.dubbo.admin.service.ProviderService;
import org.apache.dubbo.admin.service.RouteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.admin.common.utils.Constants.OLD_DUBBO_VERSION;

/** @author wujunshen */
@Authority(needLogin = true)
@RestController
@RequestMapping("/api/{env}/rules/route/condition")
public class ConditionRoutesController {
  @Resource private RouteService routeService;
  @Resource private ProviderService providerService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public boolean createRule(@RequestBody ConditionRouteDTO routeDTO) {
    String serviceName = routeDTO.getService();
    String app = routeDTO.getApplication();
    if (StringUtils.isEmpty(serviceName) && StringUtils.isEmpty(app)) {
      throw new ParamValidationException("serviceName and app is Empty!");
    }
    if (StringUtils.isNotEmpty(app)
        && providerService.findVersionInApplication(app).equals(OLD_DUBBO_VERSION)) {
      throw new VersionValidationException(
          "dubbo 2.6 does not support application scope routing rule");
    }
    routeService.createConditionRoute(routeDTO);
    return true;
  }

  @PutMapping(value = "/{id}")
  public boolean updateRule(
      @PathVariable String id, @RequestBody ConditionRouteDTO newConditionRoute) {
    id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
    ConditionRouteDTO oldConditionRoute = routeService.findConditionRoute(id);
    if (oldConditionRoute == null) {
      throw new ResourceNotFoundException("can not find route rule for: " + id);
    }
    routeService.updateConditionRoute(newConditionRoute);
    return true;
  }

  @GetMapping
  public List<ConditionRouteDTO> searchRoutes(
      @RequestParam(required = false) String application,
      @RequestParam(required = false) String service) {
    ConditionRouteDTO conditionRoute;
    List<ConditionRouteDTO> result = new ArrayList<>();
    if (StringUtils.isNotBlank(application)) {
      conditionRoute = routeService.findConditionRoute(application);
    } else if (StringUtils.isNotBlank(service)) {
      conditionRoute = routeService.findConditionRoute(service);
    } else {
      throw new ParamValidationException("Either Service or application is required.");
    }
    if (conditionRoute != null && conditionRoute.getConditions() != null) {
      result.add(conditionRoute);
    }
    return result;
  }

  @GetMapping(value = "/{id}")
  public ConditionRouteDTO detailRoute(@PathVariable String id) {
    ConditionRouteDTO conditionRoute =
        routeService.findConditionRoute(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    if (conditionRoute == null || conditionRoute.getConditions() == null) {
      throw new ResourceNotFoundException("Unknown ID!");
    }
    return conditionRoute;
  }

  @DeleteMapping(value = "/{id}")
  public boolean deleteRoute(@PathVariable String id) {
    routeService.deleteConditionRoute(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    return true;
  }

  @PutMapping(value = "/enable/{id}")
  public boolean enableRoute(@PathVariable String id) {
    routeService.enableConditionRoute(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    return true;
  }

  @PutMapping(value = "/disable/{id}")
  public boolean disableRoute(@PathVariable String id) {
    routeService.disableConditionRoute(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    return true;
  }
}
