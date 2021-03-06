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
import org.apache.dubbo.admin.model.dto.WeightDTO;
import org.apache.dubbo.admin.service.OverrideService;
import org.apache.dubbo.admin.service.ProviderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.admin.common.utils.Constants.OLD_DUBBO_VERSION;
import static org.apache.dubbo.admin.common.utils.Constants.UNKNOWN_ID;

/** @author wujunshen */
@Authority(needLogin = true)
@RestController
@RequestMapping("/api/{env}/rules/weight")
public class WeightController {
  @Resource private OverrideService overrideService;
  @Resource private ProviderService providerService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public boolean createWeight(@RequestBody WeightDTO weightDTO) {
    if (StringUtils.isBlank(weightDTO.getService())
        && StringUtils.isBlank(weightDTO.getApplication())) {
      throw new ParamValidationException("Either Service or application is required.");
    }
    String application = weightDTO.getApplication();
    if (StringUtils.isNotEmpty(application)
        && this.providerService.findVersionInApplication(application).equals(OLD_DUBBO_VERSION)) {
      throw new VersionValidationException(
          "dubbo 2.6 does not support application scope blackWhite list config");
    }
    overrideService.saveWeight(weightDTO);
    return true;
  }

  @PutMapping(value = "/{id}")
  public boolean updateWeight(@PathVariable String id, @RequestBody WeightDTO weightDTO) {
    if (id == null) {
      throw new ParamValidationException(UNKNOWN_ID);
    }
    WeightDTO weight =
        overrideService.findWeight(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    if (weight == null) {
      throw new ResourceNotFoundException(UNKNOWN_ID);
    }
    overrideService.updateWeight(weightDTO);
    return true;
  }

  @GetMapping
  public List<WeightDTO> searchWeight(
      @RequestParam(required = false) String service,
      @RequestParam(required = false) String application) {
    if (StringUtils.isBlank(service) && StringUtils.isBlank(application)) {
      throw new ParamValidationException("Either service or application is required");
    }
    WeightDTO weightDTO;
    if (StringUtils.isNotBlank(application)) {
      weightDTO = overrideService.findWeight(application);
    } else {
      weightDTO = overrideService.findWeight(service);
    }
    List<WeightDTO> weightDtoList = new ArrayList<>();
    if (weightDTO != null) {
      weightDtoList.add(weightDTO);
    }

    return weightDtoList;
  }

  @GetMapping(value = "/{id}")
  public WeightDTO detailWeight(@PathVariable String id) {
    WeightDTO weightDTO =
        overrideService.findWeight(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    if (weightDTO == null) {
      throw new ResourceNotFoundException(UNKNOWN_ID);
    }
    return weightDTO;
  }

  @DeleteMapping(value = "/{id}")
  public boolean deleteWeight(@PathVariable String id) {
    overrideService.deleteWeight(id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR));
    return true;
  }
}
