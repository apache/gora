/**
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
package org.apache.gora.accumulo.encoders;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 
 */
public class Utils {
  private static BigInteger newPositiveBigInteger(byte[] er) {
    byte[] copy = new byte[er.length + 1];
    System.arraycopy(er, 0, copy, 1, er.length);
    BigInteger bi = new BigInteger(copy);
    return bi;
  }
  
  public static byte[] lastPossibleKey(int size, byte[] er) {
    if (size == er.length)
      return er;
    
    if (er.length > size)
      throw new IllegalArgumentException();
    
    BigInteger bi = newPositiveBigInteger(er);
    if (bi.equals(BigInteger.ZERO))
      throw new IllegalArgumentException("Nothing comes before zero");
    
    bi = bi.subtract(BigInteger.ONE);
    
    byte ret[] = new byte[size];
    Arrays.fill(ret, (byte) 0xff);
    
    System.arraycopy(getBytes(bi, er.length), 0, ret, 0, er.length);
    
    return ret;
  }
  
  private static byte[] getBytes(BigInteger bi, int minLen) {
    byte[] ret = bi.toByteArray();
    
    if (ret[0] == 0) {
      // remove leading 0 that makes num positive
      byte copy[] = new byte[ret.length - 1];
      System.arraycopy(ret, 1, copy, 0, copy.length);
      ret = copy;
    }
    
    // leading digits are dropped
    byte copy[] = new byte[minLen];
    if (bi.compareTo(BigInteger.ZERO) < 0) {
      Arrays.fill(copy, (byte) 0xff);
    }
    System.arraycopy(ret, 0, copy, minLen - ret.length, ret.length);
    
    return copy;
  }
  
  public static byte[] followingKey(int size, byte[] per) {
    
    if (per.length > size)
      throw new IllegalArgumentException();
    
    if (size == per.length) {
      // add one
      BigInteger bi = new BigInteger(per);
      bi = bi.add(BigInteger.ONE);
      if (bi.equals(BigInteger.ZERO)) {
        throw new IllegalArgumentException("Wrapped");
      }
      return getBytes(bi, size);
    } else {
      return Arrays.copyOf(per, size);
    }
  }
}
