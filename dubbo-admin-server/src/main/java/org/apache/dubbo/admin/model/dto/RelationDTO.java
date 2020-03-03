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
package org.apache.dubbo.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** relation about node for relation graph
 * @author wujunshen*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationDTO {
  public static final Categories CONSUMER_CATEGORIES = new Categories(0, "consumer", "consumer");
  public static final Categories PROVIDER_CATEGORIES = new Categories(1, "provider", "provider");
  public static final Categories CONSUMER_AND_PROVIDER_CATEGORIES =
      new Categories(2, "consumer and provider", "consumer and provider");
  public static final List<Categories> CATEGORIES_LIST =
      Arrays.asList(CONSUMER_CATEGORIES, PROVIDER_CATEGORIES, CONSUMER_AND_PROVIDER_CATEGORIES);
  private List<Categories> categories;
  private List<Node> nodes;
  private List<Link> links;

  public RelationDTO(List<Node> nodes, List<Link> links) {
    this.categories = CATEGORIES_LIST;
    this.nodes = nodes;
    this.links = links;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Categories {
    private Integer index;
    private String name;
    private String base;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Node {

    private Integer index;
    private String name;
    private int category;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Node node = (Node) o;
      return category == node.category && index.equals(node.index) && name.equals(node.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(index, name, category);
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Link {
    private int source;
    private int target;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Link link = (Link) o;
      return source == link.source && target == link.target;
    }

    @Override
    public int hashCode() {
      return Objects.hash(source, target);
    }

    @Override
    public String toString() {
      return "Link{" + "source=" + source + ", target=" + target + '}';
    }
  }
}
