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


/**
 * This class transforms this bits within a primitive type so that 
 * the bit representation sorts correctly lexographicaly. Primarily 
 * it does some simple transformations so that negative numbers sort 
 * before positive numbers, when compared lexographically.
 */
public class SignedBinaryEncoder extends BinaryEncoder {
  
  public byte[] encodeShort(short s, byte ret[]){
    s = (short)((s & 0xffff) ^ 0x8000);
    return super.encodeShort(s, ret);
  }
  
  public short decodeShort(byte[] a){
    short s = super.decodeShort(a);
    s = (short)((s & 0xffff) ^ 0x8000);
    return s;
  }
  
  public byte[] encodeInt(int i, byte ret[]){
    i = i ^ 0x80000000;
    return super.encodeInt(i, ret);
  }
  
  public int decodeInt(byte[] a){
    int i = super.decodeInt(a);
    i = i ^ 0x80000000;
    return i;
  }
  
  public byte[] encodeLong(long l, byte ret[]){
    l = l ^ 0x8000000000000000l;
    return super.encodeLong(l, ret);
  }
  
  public long decodeLong(byte[] a) {
    long l = super.decodeLong(a);
    l = l ^ 0x8000000000000000l;
    return l;
  }
  
  
  public byte[] encodeDouble(double d, byte[] ret) {
    long l = Double.doubleToRawLongBits(d);
    if(l < 0)
      l = ~l;
    else
      l = l ^ 0x8000000000000000l;
    return super.encodeLong(l,ret);
  }
  
  public double decodeDouble(byte[] a){
    long l = super.decodeLong(a);
    if(l < 0)
      l = l ^ 0x8000000000000000l;
    else
      l = ~l;
    return Double.longBitsToDouble(l);
  }
  
  public byte[] encodeFloat(float f, byte[] ret) {
    int i = Float.floatToRawIntBits(f);
    if(i < 0)
      i = ~i;
    else
      i = i ^ 0x80000000;
    
    return super.encodeInt(i, ret);
    
  }
  
  public float decodeFloat(byte[] a){
    int i = super.decodeInt(a);
    if(i < 0)
      i = i ^ 0x80000000;
    else
      i = ~i;
    return Float.intBitsToFloat(i);
  }
  
}
