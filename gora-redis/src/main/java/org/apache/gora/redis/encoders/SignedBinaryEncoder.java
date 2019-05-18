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
package org.apache.gora.redis.encoders;

import java.io.IOException;

/**
 * This class transforms this bits within a primitive type so that 
 * the bit representation sorts correctly lexographicaly. Primarily 
 * it does some simple transformations so that negative numbers sort 
 * before positive numbers, when compared lexographically.
 */
public class SignedBinaryEncoder extends BinaryEncoder {

  @Override
  public byte[] encodeShort(short s, byte[] ret) throws IOException{
    s = (short)((s & 0xffff) ^ 0x8000);
    return super.encodeShort(s, ret);
  }

  @Override
  public short decodeShort(byte[] a) throws IOException{
    short s = super.decodeShort(a);
    s = (short)((s & 0xffff) ^ 0x8000);
    return s;
  }

  @Override
  public byte[] encodeInt(int i, byte[] ret) throws IOException{
    i = i ^ 0x80000000;
    return super.encodeInt(i, ret);
  }

  @Override
  public int decodeInt(byte[] a) throws IOException{
    int i = super.decodeInt(a);
    i = i ^ 0x80000000;
    return i;
  }

  @Override
  public byte[] encodeLong(long l, byte[] ret) throws IOException{
    l = l ^ 0x8000000000000000L;
    return super.encodeLong(l, ret);
  }

  @Override
  public long decodeLong(byte[] a) throws IOException {
    long l = super.decodeLong(a);
    l = l ^ 0x8000000000000000L;
    return l;
  }

  @Override
  public byte[] encodeDouble(double d, byte[] ret) throws IOException {
    long l = Double.doubleToRawLongBits(d);
    if(l < 0)
      l = ~l;
    else
      l = l ^ 0x8000000000000000L;
    return super.encodeLong(l,ret);
  }

  @Override
  public double decodeDouble(byte[] a) throws IOException{
    long l = super.decodeLong(a);
    if(l < 0)
      l = l ^ 0x8000000000000000L;
    else
      l = ~l;
    return Double.longBitsToDouble(l);
  }

  @Override
  public byte[] encodeFloat(float f, byte[] ret) throws IOException {
    int i = Float.floatToRawIntBits(f);
    if(i < 0)
      i = ~i;
    else
      i = i ^ 0x80000000;

    return super.encodeInt(i, ret);

  }

  @Override
  public float decodeFloat(byte[] a) throws IOException{
    int i = super.decodeInt(a);
    if(i < 0)
      i = i ^ 0x80000000;
    else
      i = ~i;
    return Float.intBitsToFloat(i);
  }

}
