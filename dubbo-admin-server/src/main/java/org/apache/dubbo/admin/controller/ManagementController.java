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

import org.apache.dubbo.admin.annotation.Authority;
import org.apache.dubbo.admin.common.exception.ParamValidationException;
import org.apache.dubbo.admin.common.exception.ResourceNotFoundException;
import org.apache.dubbo.admin.common.utils.Constants;
import org.apache.dubbo.admin.model.dto.ConfigDTO;
import org.apache.dubbo.admin.service.ManagementService;
import org.apache.dubbo.admin.service.ProviderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** @author wujunshen */
@Authority(needLogin = true)
@RestController
@RequestMapping("/api/{env}/manage")
public class ManagementController {
  private static Pattern classNamePattern =
      Pattern.compile("([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*");

  @Resource private ManagementService managementService;
  @Resource private ProviderService providerService;

  @PostMapping(value = "/config")
  @ResponseStatus(HttpStatus.CREATED)
  public boolean createConfig(@RequestBody ConfigDTO config, @PathVariable String env) {
    managementService.setConfig(config);
    return true;
  }

  @PutMapping(value = "/config/{key}")
  public boolean updateConfig(@PathVariable String key, @RequestBody ConfigDTO configDTO) {
    if (key == null) {
      throw new ParamValidationException("Unknown ID!");
    }
    String exitConfig = managementService.getConfig(key);
    if (exitConfig == null) {
      throw new ResourceNotFoundException("Unknown ID!");
    }
    return managementService.updateConfig(configDTO);
  }

  @GetMapping(value = "/config/{key}")
  public List<ConfigDTO> getConfig(@PathVariable String key) {
    Set<String> query = new HashSet<>();
    List<ConfigDTO> configDtoList = new ArrayList<>();
    if (key.equals(Constants.ANY_VALUE)) {
      query = providerService.findApplications();
      query.add(Constants.GLOBAL_CONFIG);
    } else {
      query.add(key);
    }
    for (String q : query) {
      String config = managementService.getConfig(q);
      if (config == null) {
        continue;
      }
      ConfigDTO configDTO = new ConfigDTO();
      configDTO.setKey(q);
      configDTO.setConfig(config);
      configDTO.setPath(managementService.getConfigPath(q));
      if (Constants.GLOBAL_CONFIG.equals(q)) {
        configDTO.setScope(Constants.GLOBAL_CONFIG);
      } else if (classNamePattern.matcher(q).matches()) {
        configDTO.setScope(Constants.SERVICE);
      } else {
        configDTO.setScope(Constants.APPLICATION);
      }
      configDtoList.add(configDTO);
    }
    return configDtoList;
  }

  @DeleteMapping(value = "/config/{key}")
  public boolean deleteConfig(@PathVariable String key) {
    return managementService.deleteConfig(key);
  }
}
