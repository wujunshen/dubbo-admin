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
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.io.Bytes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** @author wujunshen */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CoderUtils {
  private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();
  private static MessageDigest md;

  static {
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      log.error(e.getMessage(), e);
    }
  }

  public static String md516Bit(String input) {
    String hash = md532Bit(input);
    if (hash == null) {
      return null;
    }
    return hash.substring(8, 24);
  }

  public static String md532Bit(String input) {
    if (input == null || input.length() == 0) {
      return null;
    }
    md.update(input.getBytes());
    byte[] digest = md.digest();
    return convertToString(digest);
  }

  public static String md532Bit(byte[] input) {
    if (input == null || input.length == 0) {
      return null;
    }
    md.update(input);
    byte[] digest = md.digest();
    return convertToString(digest);
  }

  private static String convertToString(byte[] data) {
    StringBuilder r = new StringBuilder(data.length * 2);
    for (byte b : data) {
      r.append(HEX_CODE[(b >> 4) & 0xF]);
      r.append(HEX_CODE[(b & 0xF)]);
    }
    return r.toString();
  }

  public static String decodeBase64(String source) {
    return new String(Bytes.base642bytes(source));
  }
}
