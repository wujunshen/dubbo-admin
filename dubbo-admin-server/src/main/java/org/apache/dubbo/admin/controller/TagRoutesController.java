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
import org.apache.dubbo.admin.model.dto.TagRouteDTO;
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
@RequestMapping("/api/{env}/rules/route/tag")
public class TagRoutesController {
  @Resource private RouteService routeService;
  @Resource private ProviderService providerService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public boolean createRule(@RequestBody TagRouteDTO routeDTO) {
    String app = routeDTO.getApplication();
    if (StringUtils.isEmpty(app)) {
      throw new ParamValidationException("app is Empty!");
    }
    if (providerService.findVersionInApplication(app).equals(OLD_DUBBO_VERSION)) {
      throw new VersionValidationException("dubbo 2.6 does not support tag route");
    }
    routeService.createTagRoute(routeDTO);
    return true;
  }

  @PutMapping(value = "/{id}")
  public boolean updateRule(@PathVariable String id, @RequestBody TagRouteDTO routeDTO) {
    id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
    String app = routeDTO.getApplication();
    if (providerService.findVersionInApplication(app).equals(OLD_DUBBO_VERSION)) {
      throw new VersionValidationException("dubbo 2.6 does not support tag route");
    }
    if (routeService.findTagRoute(id) == null) {
      throw new ResourceNotFoundException("can not find tag route, Id: " + id);
    }
    routeService.updateTagRoute(routeDTO);
    return true;
  }

  @GetMapping
  public List<TagRouteDTO> searchRoutes(@RequestParam String application) {
    if (StringUtils.isBlank(application)) {
      throw new ParamValidationException("application is required.");
    }
    List<TagRouteDTO> result = new ArrayList<>();
    String version = OLD_DUBBO_VERSION;
    try {
      version = providerService.findVersionInApplication(application);
    } catch (ParamValidationException e) {
      // ignore
    }
    if (version.equals(OLD_DUBBO_VERSION)) {
      return result;
    }

    TagRouteDTO tagRoute = routeService.findTagRoute(application);
    if (tagRoute != null) {
      result.add(tagRoute);
    }
    return result;
  }

  @GetMapping(value = "/{id}")
  public TagRouteDTO detailRoute(@PathVariable String id) {
    TagRouteDTO tagRoute =
        routeService.findTagRoute(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    if (tagRoute == null) {
      throw new ResourceNotFoundException("Unknown ID!");
    }
    return tagRoute;
  }

  @DeleteMapping(value = "/{id}")
  public boolean deleteRoute(@PathVariable String id) {
    routeService.deleteTagRoute(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    return true;
  }

  @PutMapping(value = "/enable/{id}")
  public boolean enableRoute(@PathVariable String id) {
    routeService.enableTagRoute(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    return true;
  }

  @PutMapping(value = "/disable/{id}")
  public boolean disableRoute(@PathVariable String id) {
    routeService.disableTagRoute(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    return true;
  }
}
