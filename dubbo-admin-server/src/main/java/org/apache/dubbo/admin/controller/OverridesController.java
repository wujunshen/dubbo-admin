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
import org.apache.dubbo.admin.model.dto.DynamicConfigDTO;
import org.apache.dubbo.admin.service.OverrideService;
import org.apache.dubbo.admin.service.ProviderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.admin.common.utils.Constants.OLD_DUBBO_VERSION;

/** @author wujunshen */
@Authority(needLogin = true)
@RestController
@RequestMapping("/api/{env}/rules/override")
public class OverridesController {
  @Resource private OverrideService overrideService;
  @Resource private ProviderService providerService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public boolean createOverride(@RequestBody DynamicConfigDTO overrideDTO) {
    String serviceName = overrideDTO.getService();
    String application = overrideDTO.getApplication();
    if (StringUtils.isEmpty(serviceName) && StringUtils.isEmpty(application)) {
      throw new ParamValidationException("serviceName and application are Empty!");
    }
    if (StringUtils.isNotEmpty(application)
        && providerService.findVersionInApplication(application).equals(OLD_DUBBO_VERSION)) {
      throw new VersionValidationException(
          "dubbo 2.6 does not support application scope dynamic config");
    }
    overrideService.saveOverride(overrideDTO);
    return true;
  }

  @PutMapping(value = "/{id}")
  public boolean updateOverride(
      @PathVariable String id, @RequestBody DynamicConfigDTO overrideDTO) {
    DynamicConfigDTO old =
        overrideService.findOverride(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    if (old == null) {
      throw new ResourceNotFoundException("Unknown ID!");
    }
    overrideService.updateOverride(overrideDTO);
    return true;
  }

  @GetMapping
  public List<DynamicConfigDTO> searchOverride(
      @RequestParam(required = false) String service,
      @RequestParam(required = false) String application) {
    DynamicConfigDTO override;
    List<DynamicConfigDTO> result = new ArrayList<>();
    if (StringUtils.isNotBlank(service)) {
      override = overrideService.findOverride(service);
    } else if (StringUtils.isNotBlank(application)) {
      override = overrideService.findOverride(application);
    } else {
      throw new ParamValidationException("Either Service or application is required.");
    }
    if (override != null) {
      result.add(override);
    }
    return result;
  }

  @GetMapping(value = "/{id}")
  public DynamicConfigDTO detailOverride(@PathVariable String id) {
    DynamicConfigDTO override =
        overrideService.findOverride(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    if (override == null) {
      throw new ResourceNotFoundException("Unknown ID!");
    }

    return override;
  }

  @DeleteMapping(value = "/{id}")
  public boolean deleteOverride(@PathVariable String id) {
    overrideService.deleteOverride(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    return true;
  }

  @PutMapping(value = "/enable/{id}")
  public boolean enableRoute(@PathVariable String id) {
    overrideService.enableOverride(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    return true;
  }

  @PutMapping(value = "/disable/{id}")
  public boolean disableRoute(@PathVariable String id) {
    overrideService.disableOverride(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    return true;
  }
}
