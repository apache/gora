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
 * 
 */
public interface Encoder {
  
  public byte[] encodeByte(byte b, byte[] ret);
  
  public byte[] encodeByte(byte b);
  
  public byte decodeByte(byte[] a);

  public byte[] encodeShort(short s);
  
  public byte[] encodeShort(short s, byte ret[]);
  
  public short decodeShort(byte[] a);
  
  public byte[] encodeInt(int i);
  
  public byte[] encodeInt(int i, byte ret[]);
  
  public int decodeInt(byte[] a);
  
  public byte[] encodeLong(long l);
  
  public byte[] encodeLong(long l, byte ret[]);
  
  public long decodeLong(byte[] a);
  
  public byte[] encodeDouble(double d);
  
  public byte[] encodeDouble(double d, byte[] ret);
  
  public double decodeDouble(byte[] a);
  
  public byte[] encodeFloat(float d);
  
  public byte[] encodeFloat(float f, byte[] ret);
  
  public float decodeFloat(byte[] a);
  
  public boolean decodeBoolean(byte[] val);
  
  public byte[] encodeBoolean(boolean b);
  
  public byte[] encodeBoolean(boolean b, byte[] ret);

  byte[] followingKey(int size, byte[] per);

  byte[] lastPossibleKey(int size, byte[] er);

}
