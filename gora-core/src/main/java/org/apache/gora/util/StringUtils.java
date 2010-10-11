/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An utility class for String related functionality.
 */
public class StringUtils {

  /**
   * Joins the two given arrays, removing dup elements.
   */
  public static String[] joinStringArrays(String[] arr1, String... arr2) {
    HashSet<String> set = new HashSet<String>();
    for(String str : arr1) set.add(str);
    for(String str : arr2) set.add(str);

    return set.toArray(new String[set.size()]);
  }

  public static String join(List<String> strs) {
    return join(new StringBuilder(), strs).toString();
  }

  public static String join(String[] strs) {
    return join(new StringBuilder(), strs).toString();
  }

  public static StringBuilder join(StringBuilder builder, Collection<String> strs) {
    int i = 0;
    for (String s : strs) {
      if(i != 0) builder.append(',');
      builder.append(s);
      i++;
    }
    return builder;
  }

  public static StringBuilder join(StringBuilder builder, String[] strs) {
    for (int i = 0; i < strs.length; i++) {
      if(i != 0) builder.append(",");
      builder.append(strs[i]);
    }
    return builder;
  }

  /** helper for string null and empty checking*/
  public static boolean is(String str) {
    return str != null && str.length() > 0;
  }

  //below is taken from:http://jvalentino.blogspot.com/2007/02/shortcut-to-calculating-power-set-using.html
  /**
   * Returns the power set from the given set by using a binary counter
   * Example: S = {a,b,c}
   * P(S) = {[], [c], [b], [b, c], [a], [a, c], [a, b], [a, b, c]}
   * @param set String[]
   * @return LinkedHashSet
   */
  public static LinkedHashSet<Set<String>> powerset(String[] set) {

    //create the empty power set
    LinkedHashSet<Set<String>> power = new LinkedHashSet<Set<String>>();

    //get the number of elements in the set
    int elements = set.length;

    //the number of members of a power set is 2^n
    int powerElements = (int) Math.pow(2,elements);

    //run a binary counter for the number of power elements
    for (int i = 0; i < powerElements; i++) {

      //convert the binary number to a string containing n digits
      String binary = intToBinary(i, elements);

      //create a new set
      LinkedHashSet<String> innerSet = new LinkedHashSet<String>();

      //convert each digit in the current binary number to the corresponding element
      //in the given set
      for (int j = 0; j < binary.length(); j++) {
        if (binary.charAt(j) == '1')
          innerSet.add(set[j]);
      }

      //add the new set to the power set
      power.add(innerSet);

    }

    return power;
  }

  /**
   * Converts the given integer to a String representing a binary number
   * with the specified number of digits
   * For example when using 4 digits the binary 1 is 0001
   * @param binary int
   * @param digits int
   * @return String
   */
  private static String intToBinary(int binary, int digits) {
    String temp = Integer.toBinaryString(binary);
    int foundDigits = temp.length();
    String returner = temp;
    for (int i = foundDigits; i < digits; i++) {
      returner = "0" + returner;
    }
    return returner;
  }

  public static int parseInt(String str, int defaultValue) {
    if(str == null) {
      return defaultValue;
    }
    return Integer.parseInt(str);
  }

  /**
   * Returns the name of the class without the package name.
   */
  public static String getClassname(Class<?> clazz) {
    return getClassname(clazz.getName());
  }

  /**
   * Returns the name of the class without the package name.
   */
  public static String getClassname(String classname) {
    String[] parts = classname.split("\\.");
    return parts[parts.length-1];
  }

}
