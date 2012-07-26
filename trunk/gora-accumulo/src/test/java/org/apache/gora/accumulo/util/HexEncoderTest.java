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

import org.apache.gora.accumulo.encoders.HexEncoder;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class HexEncoderTest {
  
  @Test
  public void testByte() {
    HexEncoder encoder = new HexEncoder();
    
    Assert.assertEquals("12", new String(encoder.encodeByte((byte) 0x12)));
    Assert.assertEquals("f2", new String(encoder.encodeByte((byte) 0xf2)));
    
    byte b = Byte.MIN_VALUE;
    while (b != Byte.MAX_VALUE) {
      Assert.assertEquals(b, encoder.decodeByte(encoder.encodeByte(b)));
      b++;
    }
  }

  @Test
  public void testShort() {
    HexEncoder encoder = new HexEncoder();
    
    Assert.assertEquals("1234", new String(encoder.encodeShort((short) 0x1234)));
    Assert.assertEquals("f234", new String(encoder.encodeShort((short) 0xf234)));
    
    short s = Short.MIN_VALUE;
    while (s != Short.MAX_VALUE) {
      Assert.assertEquals(s, encoder.decodeShort(encoder.encodeShort(s)));
      s++;
    }
  }
}
