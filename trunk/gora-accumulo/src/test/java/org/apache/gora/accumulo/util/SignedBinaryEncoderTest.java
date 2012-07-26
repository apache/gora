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
package org.apache.gora.accumulo.util;

import java.util.ArrayList;
import java.util.Collections;

import junit.framework.Assert;

import org.apache.gora.accumulo.encoders.SignedBinaryEncoder;
import org.apache.hadoop.io.Text;
import org.junit.Test;

/**
 * 
 */
public class SignedBinaryEncoderTest {
  @Test
  public void testShort() {
    short s = Short.MIN_VALUE;
    Text prev = null;
    
    SignedBinaryEncoder encoder = new SignedBinaryEncoder();

    while (true) {
      byte[] enc = encoder.encodeShort(s);
      Assert.assertEquals(s, encoder.decodeShort(enc));
      Text current = new Text(enc);
      if (prev != null)
        Assert.assertTrue(prev.compareTo(current) < 0);
      prev = current;
      s++;
      if (s == Short.MAX_VALUE)
        break;
    }
  }

  private void testInt(int start, int finish) {
    int i = start;
    Text prev = null;
    
    SignedBinaryEncoder encoder = new SignedBinaryEncoder();

    while (true) {
      byte[] enc = encoder.encodeInt(i);
      Assert.assertEquals(i, encoder.decodeInt(enc));
      Text current = new Text(enc);
      if (prev != null)
        Assert.assertTrue(prev.compareTo(current) < 0);
      prev = current;
      i++;
      if (i == finish)
        break;
    }
  }
  
  @Test
  public void testInt() {
    testInt(Integer.MIN_VALUE, Integer.MIN_VALUE + (1 << 16));
    testInt(-(1 << 15), (1 << 15));
    testInt(Integer.MAX_VALUE - (1 << 16), Integer.MAX_VALUE);
  }
  
  private void testLong(long start, long finish) {
    long l = start;
    Text prev = null;
    
    SignedBinaryEncoder encoder = new SignedBinaryEncoder();

    while (true) {
      byte[] enc = encoder.encodeLong(l);
      Assert.assertEquals(l, encoder.decodeLong(enc));
      Text current = new Text(enc);
      if (prev != null)
        Assert.assertTrue(prev.compareTo(current) < 0);
      prev = current;
      l++;
      if (l == finish)
        break;
    }
  }
  
  @Test
  public void testLong() {
    testLong(Long.MIN_VALUE, Long.MIN_VALUE + (1 << 16));
    testLong(-(1 << 15), (1 << 15));
    testLong(Long.MAX_VALUE - (1 << 16), Long.MAX_VALUE);
  }
  
  @Test
  public void testDouble() {
    
    ArrayList<Double> testData = new ArrayList<Double>();
    testData.add(Double.NEGATIVE_INFINITY);
    testData.add(Double.MIN_VALUE);
    testData.add(Math.nextUp(Double.NEGATIVE_INFINITY));
    testData.add(Math.pow(10.0, 30.0) * -1.0);
    testData.add(Math.pow(10.0, 30.0));
    testData.add(Math.pow(10.0, -30.0) * -1.0);
    testData.add(Math.pow(10.0, -30.0));
    testData.add(Math.nextAfter(0.0, Double.NEGATIVE_INFINITY));
    testData.add(0.0);
    testData.add(Math.nextAfter(Double.MAX_VALUE, Double.NEGATIVE_INFINITY));
    testData.add(Double.MAX_VALUE);
    testData.add(Double.POSITIVE_INFINITY);
    
    Collections.sort(testData);
    
    SignedBinaryEncoder encoder = new SignedBinaryEncoder();

    for (int i = 0; i < testData.size(); i++) {
      byte[] enc = encoder.encodeDouble(testData.get(i));
      Assert.assertEquals(testData.get(i), encoder.decodeDouble(enc));
      if (i > 1) {
        Assert.assertTrue("Checking " + testData.get(i) + " > " + testData.get(i - 1),
            new Text(enc).compareTo(new Text(encoder.encodeDouble(testData.get(i - 1)))) > 0);
      }
    }
  }

  @Test
  public void testFloat() {
    
    ArrayList<Float> testData = new ArrayList<Float>();
    testData.add(Float.NEGATIVE_INFINITY);
    testData.add(Float.MIN_VALUE);
    testData.add(Math.nextUp(Float.NEGATIVE_INFINITY));
    testData.add((float) Math.pow(10.0f, 30.0f) * -1.0f);
    testData.add((float) Math.pow(10.0f, 30.0f));
    testData.add((float) Math.pow(10.0f, -30.0f) * -1.0f);
    testData.add((float) Math.pow(10.0f, -30.0f));
    testData.add(Math.nextAfter(0.0f, Float.NEGATIVE_INFINITY));
    testData.add(0.0f);
    testData.add(Math.nextAfter(Float.MAX_VALUE, Float.NEGATIVE_INFINITY));
    testData.add(Float.MAX_VALUE);
    testData.add(Float.POSITIVE_INFINITY);
    
    Collections.sort(testData);
    
    SignedBinaryEncoder encoder = new SignedBinaryEncoder();

    for (int i = 0; i < testData.size(); i++) {
      byte[] enc = encoder.encodeFloat(testData.get(i));
      Assert.assertEquals(testData.get(i), encoder.decodeFloat(enc));
      if (i > 1) {
        Assert.assertTrue("Checking " + testData.get(i) + " > " + testData.get(i - 1),
            new Text(enc).compareTo(new Text(encoder.encodeFloat(testData.get(i - 1)))) > 0);
      }
    }
  }

}
