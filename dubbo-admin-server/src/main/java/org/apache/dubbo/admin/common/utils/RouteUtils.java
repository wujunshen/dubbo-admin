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

import org.apache.dubbo.admin.model.domain.Route;
import org.apache.dubbo.admin.model.dto.AccessDTO;
import org.apache.dubbo.admin.model.dto.ConditionRouteDTO;
import org.apache.dubbo.admin.model.dto.TagRouteDTO;
import org.apache.dubbo.admin.model.store.RoutingRule;
import org.apache.dubbo.admin.model.store.TagRoute;
import org.apache.dubbo.common.utils.StringUtils;

import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Router rule can be divided into two parts, When Condition and Then Condition <br>
 * When/Then Confition is expressed in a style of (KV) pair, the V part of the condition pair can
 * contain multiple values (a list) <br>
 * The meaning of Rule: If a request matches When Condition, then use Then Condition to filter
 * providers (only providers match Then Condition will be returned). <br>
 * The process of using Conditions to match consumers and providers is called `Filter`. When
 * Condition are used to filter ConsumersController, while Then Condition are used to filter
 * ProvidersController. RouteUtils performs like this: If a Consumer matches When Condition, then
 * only return the ProvidersController matches Then Condition. This means RouteUtils should be
 * applied to current Consumer and the providers returned are filtered by RouteUtils.<br>
 *
 * <p>An example of ConditionRoute Rule：<code>
 * key1 = value11,value12 & key2 = value21 & key2 != value22 => key3 = value3 & key4 = value41,vlaue42 & key5 !=value51
 * </code>。 The part before <code>=></code> is called When Condition, it's a KV pair; the follower
 * part is Then Condition, also a KV pair. V part in KV can have more than one value, separated by
 * ','<br>
 * <br>
 *
 * <p>Value object, thread safe.
 */
public class RouteUtils {
  private static Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");
  private static Pattern CONDITION_SEPERATOR = Pattern.compile("(.*)=>(.*)");
  private static Pattern valueListSeparator = Pattern.compile("\\s*,\\s*");
  final Map<String, MatchPair> whenCondition;
  final Map<String, MatchPair> thenCondition;
  private volatile String tostring = null;

  // FIXME
  private RouteUtils(Map<String, MatchPair> when, Map<String, MatchPair> then) {
    for (Entry<String, MatchPair> entry : when.entrySet()) {
      entry.getValue().freeze();
    }
    for (Entry<String, MatchPair> entry : then.entrySet()) {
      entry.getValue().freeze();
    }

    // NOTE: Both When Condition and Then Condition can be null
    this.whenCondition = when;
    this.thenCondition = then;
  }

  public static Map<String, MatchPair> parseRule(String rule) throws ParseException {
    Map<String, MatchPair> condition = new HashMap<>();
    if (StringUtils.isBlank(rule)) {
      return condition;
    }
    // K-V pair, contains matches part and mismatches part
    MatchPair pair = null;
    // V part has multiple values
    Set<String> values = null;
    final Matcher matcher = ROUTE_PATTERN.matcher(rule);
    // match one by one
    while (matcher.find()) {
      String separator = matcher.group(1);
      String content = matcher.group(2);
      // The expression starts
      if (separator == null || separator.length() == 0) {
        pair = new MatchPair();
        condition.put(content, pair);
      }
      // The KV starts
      else if ("&".equals(separator)) {
        if (condition.get(content) == null) {
          pair = new MatchPair();
          condition.put(content, pair);
        } else {
          condition.put(content, pair);
        }

      }
      // The Value part of KV starts
      else if ("=".equals(separator)) {
        if (pair == null) {
          throw new ParseException(
              "Illegal route rule \""
                  + rule
                  + "\", The error char '"
                  + separator
                  + "' at index "
                  + matcher.start()
                  + " before \""
                  + content
                  + "\".",
              matcher.start());
        }

        values = pair.matches;
        values.add(content);
      }
      // The Value part of KV starts
      else if ("!=".equals(separator)) {
        if (pair == null) {
          throw new ParseException(
              "Illegal route rule \""
                  + rule
                  + "\", The error char '"
                  + separator
                  + "' at index "
                  + matcher.start()
                  + " before \""
                  + content
                  + "\".",
              matcher.start());
        }

        values = pair.unmatches;
        values.add(content);
      }
      // The Value part of KV has multiple values, separated by ','
      else if (",".equals(separator)) {
        // separated by ','
        if (values == null || values.size() == 0) {
          throw new ParseException(
              "Illegal route rule \""
                  + rule
                  + "\", The error char '"
                  + separator
                  + "' at index "
                  + matcher.start()
                  + " before \""
                  + content
                  + "\".",
              matcher.start());
        }
        values.add(content);
      } else {
        throw new ParseException(
            "Illegal route rule \""
                + rule
                + "\", The error char '"
                + separator
                + "' at index "
                + matcher.start()
                + " before \""
                + content
                + "\".",
            matcher.start());
      }
    }
    return condition;
  }

  public static RouteUtils parse(String whenRule, String thenRule) throws ParseException {
    if (thenRule == null || thenRule.trim().length() == 0) {
      throw new ParseException("Illegal route rule without then express", 0);
    }
    Map<String, MatchPair> when = parseRule(whenRule.trim());
    Map<String, MatchPair> then = parseRule(thenRule.trim());
    return new RouteUtils(when, then);
  }

  public static RouteUtils parse(String rule) throws ParseException {
    if (StringUtils.isBlank(rule)) {
      throw new ParseException("Illegal blank route rule", 0);
    }

    final Matcher matcher = CONDITION_SEPERATOR.matcher(rule);
    if (!matcher.matches()) {
      throw new ParseException("condition seperator => not found!", 0);
    }

    return parse(matcher.group(1), matcher.group(2));
  }

  public static TagRoute convertTagRoute2Store(TagRouteDTO tagRoute) {
    TagRoute store = new TagRoute();
    store.setKey(tagRoute.getApplication());
    store.setEnabled(tagRoute.getEnabled());
    store.setForce(tagRoute.getForce());
    store.setPriority(tagRoute.getPriority());
    store.setRuntime(tagRoute.getRuntime());
    store.setTags(tagRoute.getTags());
    return store;
  }

  public static TagRouteDTO convertTagRoute2Display(TagRoute tagRoute) {
    TagRouteDTO tagRouteDTO = new TagRouteDTO();
    tagRouteDTO.setApplication(tagRoute.getKey());
    tagRouteDTO.setRuntime(tagRoute.isRuntime());
    tagRouteDTO.setPriority(tagRoute.getPriority());
    tagRouteDTO.setTags(tagRoute.getTags());
    tagRouteDTO.setForce(tagRoute.isForce());
    tagRouteDTO.setEnabled(tagRoute.isEnabled());
    return tagRouteDTO;
  }

  public static RoutingRule insertConditionRule(
      RoutingRule existRule, ConditionRouteDTO conditionRoute) {
    if (existRule == null) {
      existRule = new RoutingRule();
      if (StringUtils.isNotEmpty(conditionRoute.getApplication())) {
        existRule.setKey(conditionRoute.getApplication());
        existRule.setScope(Constants.APPLICATION);
      } else {
        existRule.setKey(conditionRoute.getService().replace("/", "*"));
        existRule.setScope(Constants.SERVICE);
      }
    }
    existRule.setConditions(conditionRoute.getConditions());
    existRule.setEnabled(conditionRoute.getEnabled());
    existRule.setForce(conditionRoute.getForce());
    existRule.setRuntime(conditionRoute.getRuntime());
    existRule.setPriority(conditionRoute.getPriority());
    return existRule;
  }

  public static List<String> convertToBlackWhiteList(AccessDTO accessDTO) {
    if (accessDTO == null) {
      return null;
    }

    Set<String> whiteList = accessDTO.getWhitelist();
    Set<String> blackList = accessDTO.getBlacklist();
    List<String> conditions = new ArrayList<>();
    if (whiteList != null && !whiteList.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("host != ");
      for (String white : whiteList) {
        sb.append(white).append(",");
      }
      sb.deleteCharAt(sb.length() - 1);
      sb.append(" =>");
      conditions.add(sb.toString());
    }
    if (blackList != null && !blackList.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("host = ");
      for (String black : blackList) {
        sb.append(black).append(",");
      }
      sb.deleteCharAt(sb.length() - 1);
      sb.append(" =>");
      conditions.add(sb.toString());
    }
    return conditions;
  }

  public static List<String> filterBlackWhiteListFromConditions(List<String> conditions) {
    List<String> result = new ArrayList<>();
    if (conditions == null || conditions.isEmpty()) {
      return result;
    }
    for (String condition : conditions) {
      if (isBlackList(condition)) {
        result.add(condition);
      } else if (isWhiteList(condition)) {
        result.add(condition);
      }
    }
    return result;
  }

  public static List<String> filterConditionRuleFromConditions(List<String> conditions) {
    List<String> result = new ArrayList<>();
    if (conditions == null || conditions.isEmpty()) {
      return result;
    }
    for (String condition : conditions) {
      if (!isBlackList(condition) && !isWhiteList(condition)) {
        result.add(condition);
      }
    }
    return result;
  }

  public static AccessDTO convertToAccessDTO(
      List<String> blackWhiteList, String scope, String key) {
    if (blackWhiteList == null) {
      return null;
    }
    AccessDTO accessDTO = new AccessDTO();
    if (scope.equals(Constants.APPLICATION)) {
      accessDTO.setApplication(key);
    } else {
      accessDTO.setService(key);
    }
    if (blackWhiteList != null) {
      for (String condition : blackWhiteList) {
        if (condition.contains("host != ")) {
          // white list
          condition =
              org.apache.commons.lang3.StringUtils.substringBetween(condition, "host !=", " =>")
                  .trim();
          accessDTO.setWhitelist(new HashSet<>(Arrays.asList(condition.split(","))));
        }
        if (condition.contains("host = ")) {
          // black list
          condition =
              org.apache.commons.lang3.StringUtils.substringBetween(condition, "host =", " =>")
                  .trim();
          accessDTO.setBlacklist(new HashSet<>(Arrays.asList(condition.split(","))));
        }
      }
    }
    return accessDTO;
  }

  public static Route convertAccessDto2Route(AccessDTO accessDTO) {
    Route route = new Route();
    route.setService(accessDTO.getService());
    route.setForce(true);
    route.setFilterRule("false");
    route.setEnabled(true);

    Map<String, MatchPair> when = new HashMap<>();
    MatchPair matchPair = new MatchPair(new HashSet<>(), new HashSet<>());
    when.put(Route.KEY_CONSUMER_HOST, matchPair);

    if (accessDTO.getWhitelist() != null) {
      matchPair.getUnmatches().addAll(accessDTO.getWhitelist());
    }
    if (accessDTO.getBlacklist() != null) {
      matchPair.getMatches().addAll(accessDTO.getBlacklist());
    }

    StringBuilder sb = new StringBuilder();
    RouteUtils.contidionToString(sb, when);
    route.setMatchRule(sb.toString());
    return route;
  }

  public static ConditionRouteDTO createConditionRouteFromRule(RoutingRule routingRule) {
    ConditionRouteDTO conditionRouteDTO = new ConditionRouteDTO();
    if (routingRule.getScope().equals(Constants.SERVICE)) {
      conditionRouteDTO.setService(routingRule.getKey());
    } else {
      conditionRouteDTO.setApplication(routingRule.getKey());
    }
    conditionRouteDTO.setConditions(
        RouteUtils.filterConditionRuleFromConditions(routingRule.getConditions()));
    conditionRouteDTO.setPriority(routingRule.getPriority());
    conditionRouteDTO.setEnabled(routingRule.getEnabled());
    conditionRouteDTO.setForce(routingRule.getForce());
    conditionRouteDTO.setRuntime(routingRule.getRuntime());
    return conditionRouteDTO;
  }

  public static Route convertBlackWhiteList2Route(
      List<String> blackWhiteList, String scope, String key) {
    AccessDTO accessDTO = convertToAccessDTO(blackWhiteList, scope, key);
    return convertAccessDto2Route(accessDTO);
  }

  // TODO ToString out of the current list is out of order, should we sort?
  static void join(StringBuilder sb, Set<String> valueSet) {
    boolean isFirst = true;
    for (String s : valueSet) {
      if (isFirst) {
        isFirst = false;
      } else {
        sb.append(",");
      }

      sb.append(s);
    }
  }

  // FIXME Remove such method calls
  public static String join(Set<String> valueSet) {
    StringBuilder sb = new StringBuilder(128);
    join(sb, valueSet);
    return sb.toString();
  }

  // TODO At present, the multiple Key of Condition is in disorder. Should we sort it?
  public static void contidionToString(StringBuilder sb, Map<String, MatchPair> condition) {
    boolean isFirst = true;
    for (Entry<String, MatchPair> entry : condition.entrySet()) {
      String keyName = entry.getKey();
      MatchPair p = entry.getValue();

      @SuppressWarnings("unchecked")
      Set<String>[] setArray = new Set[] {p.matches, p.unmatches};
      String[] opArray = {" = ", " != "};

      for (int i = 0; i < setArray.length; ++i) {
        if (setArray[i].isEmpty()) {
          continue;
        }
        if (isFirst) {
          isFirst = false;
        } else {
          sb.append(" & ");
        }

        sb.append(keyName);
        sb.append(opArray[i]);
        join(sb, setArray[i]);
      }
    }
  }

  private static boolean isBlackList(String address) {
    return (address.startsWith("host = ") && address.endsWith(" =>"));
  }

  private static boolean isWhiteList(String address) {
    return (address.startsWith("host != ") && address.endsWith(" =>"));
  }

  @Override
  public String toString() {
    if (tostring != null) {
      return tostring;
    }
    StringBuilder sb = new StringBuilder(512);
    contidionToString(sb, whenCondition);
    sb.append(" => ");
    contidionToString(sb, thenCondition);
    return tostring = sb.toString();
  }

  // Automatic generation with Eclipse
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((thenCondition == null) ? 0 : thenCondition.hashCode());
    result = prime * result + ((whenCondition == null) ? 0 : whenCondition.hashCode());
    return result;
  }

  // Automatic generation with Eclipse
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RouteUtils other = (RouteUtils) obj;
    if (thenCondition == null) {
      if (other.thenCondition != null) {
        return false;
      }
    } else if (!thenCondition.equals(other.thenCondition)) {
      return false;
    }
    if (whenCondition == null) {
      return other.whenCondition == null;
    } else {
      return whenCondition.equals(other.whenCondition);
    }
  }

  public static class MatchPair {
    Set<String> matches = new HashSet<>();
    Set<String> unmatches = new HashSet<>();

    public MatchPair() {}

    public MatchPair(Set<String> matches, Set<String> unmatches) {
      if (matches == null || unmatches == null) {
        throw new IllegalArgumentException("argument of MatchPair is null!");
      }

      this.matches = matches;
      this.unmatches = unmatches;
    }

    public Set<String> getMatches() {
      return matches;
    }

    public Set<String> getUnmatches() {
      return unmatches;
    }

    public MatchPair copy() {
      MatchPair ret = new MatchPair();
      ret.matches.addAll(matches);
      ret.unmatches.addAll(unmatches);
      return ret;
    }

    void freeze() {
      boolean freezed = false;
      if (freezed) {
        return;
      }
      synchronized (this) {
        if (freezed) {
          return;
        }
        matches = Collections.unmodifiableSet(matches);
        unmatches = Collections.unmodifiableSet(unmatches);
      }
    }

    /**
     * Whether a given value is matched by the {@link MatchPair}. return {@code false}, if
     *
     * <ol>
     *   <li>value is in unmatches
     *   <li>matches is not null, but value is not in matches.
     * </ol>
     *
     * otherwise, return<code>true</code>。
     */
    public boolean pass(String sample) {
      if (unmatches.contains(sample)) {
        return false;
      }
      if (matches.isEmpty()) {
        return true;
      }
      return matches.contains(sample);
    }

    @Override
    public String toString() {
      return String.format("{matches=%s,unmatches=%s}", matches.toString(), unmatches.toString());
    }

    // Automatic generation with Eclipse
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((matches == null) ? 0 : matches.hashCode());
      result = prime * result + ((unmatches == null) ? 0 : unmatches.hashCode());
      return result;
    }

    // Automatic generation with Eclipse
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MatchPair other = (MatchPair) obj;
      if (matches == null) {
        if (other.matches != null) {
          return false;
        }
      } else if (!matches.equals(other.matches)) {
        return false;
      }
      if (unmatches == null) {
        return other.unmatches == null;
      } else {
        return unmatches.equals(other.unmatches);
      }
    }
  }
}
