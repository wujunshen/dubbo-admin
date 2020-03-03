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

import lombok.Data;
import org.apache.dubbo.admin.annotation.Authority;
import org.apache.dubbo.admin.common.util.Sha256Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wujunshen
 */
@RestController
@RequestMapping("/api/{env}/user")
public class UserController {
    /**
     * key:token value:user info
     */
    public static Map<String, User> tokenMap = new ConcurrentHashMap<>();

    @Value("${admin.root.user.name:}")
    private String rootUserName;

    @Value("${admin.root.user.password:}")
    private String rootUserPassword;

    @GetMapping(value = "/login")
    public String login(@RequestParam String userName, @RequestParam String password) {
        if (!rootUserName.equals(userName)
                || !rootUserPassword.equals(Sha256Utils.getSha256(password))) {
            return null;
        }
        UUID uuid = UUID.randomUUID();
        String token = uuid.toString();
        User user = new User();
        user.setUserName(userName);
        user.setLastUpdateTime(System.currentTimeMillis());
        tokenMap.put(token, user);
        return token;
    }

    @Authority(needLogin = true)
    @DeleteMapping(value = "/logout")
    public boolean logout() {
        HttpServletRequest request =
                ((ServletRequestAttributes)
                        Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                        .getRequest();
        String token = request.getHeader("Authorization");
        return null != tokenMap.remove(token);
    }

    @Scheduled(cron = "0 5 * * * ?")
    public void clearExpiredToken() {
        tokenMap
                .entrySet()
                .removeIf(
                        entry ->
                                entry.getValue() == null
                                        || System.currentTimeMillis() - entry.getValue().getLastUpdateTime()
                                        > 1000 * 60 * 15);
    }

    @Data
    public static class User {
        private String userName;
        private long lastUpdateTime;
    }
}
