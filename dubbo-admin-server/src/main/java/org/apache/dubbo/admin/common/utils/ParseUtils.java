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
package org.apache.dubbo.admin.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String parsing tools related to interpolation, including Glob mode, Query string, Service URL
 * processing.
 *
 * @author wujunshen
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParseUtils {
  public static final String METHOD_SPLIT = ",";
  private static final Pattern VARIABLE_PATTERN =
      Pattern.compile("\\$\\s*\\{?\\s*([._0-9a-zA-Z]+)\\s*\\}?");
  private static final Pattern QUERY_PATTERN = Pattern.compile("([&=]?)\\s*([^&=\\s]+)");

  /**
   * Execute interpolation (variable insertion).
   *
   * @param expression Expression string containing variables. Variable names in expressions can also be enclosed in <code> {} </ code>。
   * @param params     Variable set. Variable names can include <code>. </ Code>, <code> _ </ code> characters.
   * @return After the completion of the interpolation string. Such as: <code> <pre> xxx $ {name} zzz -> xxxjerryzzz </ pre> </ code> (where the variable name = "jerry")
   * @throws IllegalStateException The variables used in the expression string are not in the variable set
   */
  // FIXME Is it reasonable to throw an IllegalStateException??
  public static String interpolate(String expression, Map<String, String> params) {
    if (expression == null || expression.length() == 0) {
      throw new IllegalArgumentException("glob pattern is empty!");
    }
    if (expression.indexOf('$') < 0) {
      return expression;
    }
    Matcher matcher = VARIABLE_PATTERN.matcher(expression);
    StringBuffer sb = new StringBuffer();
    // match one by one
    while (matcher.find()) {
      String key = matcher.group(1);
      String value = params == null ? null : params.get(key);
      if (value == null) {
        value = "";
      }
      matcher.appendReplacement(sb, value);
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  public static List<String> interpolate(List<String> expressions, Map<String, String> params) {
    List<String> ret = new ArrayList<>();

    if (null == expressions || expressions.isEmpty()) {
      return ret;
    }

    for (String expr : expressions) {
      ret.add(interpolate(expr, params));
    }

    return ret;
  }

  /**
   * Parse Query String into Map. For strings that have only Key, key3 = </ code> is ignored.
   *
   * @param keyPrefix In the output of the Map Key plus a unified prefix.
   * @param query Query String，For example: <code>key1=value1&key2=value2</code>
   * @return When Query String is <code>key1=value1&key2=value2</code>, and prefix is <code>pre.
   *     </code>, then <code>Map{pre.key1=value1, pre.key=value2}</code> will be returned.
   */
  // FIXME Is it reasonable to throw an IllegalStateException??
  public static Map<String, String> parseQuery(String keyPrefix, String query) {
    if (query == null) {
      return  new ConcurrentHashMap<>(8);
    }
    if (keyPrefix == null) {
      keyPrefix = "";
    }

    Matcher matcher = QUERY_PATTERN.matcher(query);
    Map<String, String> routeQuery =  new ConcurrentHashMap<>(8);
    String key = null;
    // Match one by one
    while (matcher.find()) {
      String separator = matcher.group(1);
      String content = matcher.group(2);
      if (separator == null || separator.length() == 0 || "&".equals(separator)) {
        if (key != null) {
          throw new IllegalStateException(
              "Illegal query string \""
                  + query
                  + "\", The error char '"
                  + separator
                  + "' at index "
                  + matcher.start()
                  + " before \""
                  + content
                  + "\".");
        }
        key = content;
      } else if ("=".equals(separator)) {
        if (key == null) {
          throw new IllegalStateException(
              "Illegal query string \""
                  + query
                  + "\", The error char '"
                  + separator
                  + "' at index "
                  + matcher.start()
                  + " before \""
                  + content
                  + "\".");
        }
        routeQuery.put(keyPrefix + key, content);
        key = null;
      } else {
        if (key == null) {
          throw new IllegalStateException(
              "Illegal query string \""
                  + query
                  + "\", The error char '"
                  + separator
                  + "' at index "
                  + matcher.start()
                  + " before \""
                  + content
                  + "\".");
        }
      }
    }
    return routeQuery;
  }

  public static Map<String, String> parseQuery(String query) {
    return parseQuery("", query);
  }
}
