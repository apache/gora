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
 * Encodes data in a ascii hex representation
 */

public class HexEncoder implements Encoder {
  
  private byte chars[] = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  private void encode(byte[] a, long l) {
    for (int i = a.length - 1; i >= 0; i--) {
      a[i] = chars[(int) (l & 0x0f)];
      l = l >>> 4;
    }
  }

  private int fromChar(byte b) {
    if (b >= '0' && b <= '9') {
      return (b - '0');
    } else if (b >= 'a' && b <= 'f') {
      return (b - 'a' + 10);
    }
    
    throw new IllegalArgumentException("Bad char " + b);
  }
  
  private long decode(byte[] a) {
    long b = 0;
    for (int i = 0; i < a.length; i++) {
      b = b << 4;
      b |= fromChar(a[i]);
    }
    
    return b;
  }

  @Override
  public byte[] encodeByte(byte b, byte[] ret) {
    encode(ret, 0xff & b);
    return ret;
  }
  
  @Override
  public byte[] encodeByte(byte b) {
    return encodeByte(b, new byte[2]);
  }
  
  @Override
  public byte decodeByte(byte[] a) {
    return (byte) decode(a);
  }
  
  @Override
  public byte[] encodeShort(short s) {
    return encodeShort(s, new byte[4]);
  }
  
  @Override
  public byte[] encodeShort(short s, byte[] ret) {
    encode(ret, 0xffff & s);
    return ret;
  }
  
  @Override
  public short decodeShort(byte[] a) {
    return (short) decode(a);
  }
  
  @Override
  public byte[] encodeInt(int i) {
    return encodeInt(i, new byte[8]);
  }
  
  @Override
  public byte[] encodeInt(int i, byte[] ret) {
    encode(ret, i);
    return ret;
  }
  
  @Override
  public int decodeInt(byte[] a) {
    return (int) decode(a);
  }
  
  @Override
  public byte[] encodeLong(long l) {
    return encodeLong(l, new byte[16]);
  }
  
  @Override
  public byte[] encodeLong(long l, byte[] ret) {
    encode(ret, l);
    return ret;
  }
  
  @Override
  public long decodeLong(byte[] a) {
    return decode(a);
  }
  
  @Override
  public byte[] encodeDouble(double d) {
    return encodeDouble(d, new byte[16]);
  }
  
  @Override
  public byte[] encodeDouble(double d, byte[] ret) {
    return encodeLong(Double.doubleToRawLongBits(d), ret);
  }
  
  @Override
  public double decodeDouble(byte[] a) {
    return Double.longBitsToDouble(decodeLong(a));
  }
  
  @Override
  public byte[] encodeFloat(float d) {
    return encodeFloat(d, new byte[16]);
  }
  
  @Override
  public byte[] encodeFloat(float d, byte[] ret) {
    return encodeInt(Float.floatToRawIntBits(d), ret);
  }
  
  @Override
  public float decodeFloat(byte[] a) {
    return Float.intBitsToFloat(decodeInt(a));
  }
  
  @Override
  public boolean decodeBoolean(byte[] val) {
    if (decodeByte(val) == 1) {
      return true;
    }
    return false;
  }
  
  @Override
  public byte[] encodeBoolean(boolean b) {
    return encodeBoolean(b, new byte[2]);
  }
  
  @Override
  public byte[] encodeBoolean(boolean b, byte[] ret) {
    if (b)
      encode(ret, 1);
    else
      encode(ret, 0);
    
    return ret;
  }
  
  private byte[] toBinary(byte[] hex) {
    byte[] bin = new byte[(hex.length / 2) + (hex.length % 2)];
    
    int j = 0;
    for (int i = 0; i < bin.length; i++) {
      bin[i] = (byte) (fromChar(hex[j++]) << 4);
      if (j >= hex.length)
        break;
      bin[i] |= (byte) fromChar(hex[j++]);
    }
    
    return bin;
  }
  
  private byte[] fromBinary(byte[] bin) {
    byte[] hex = new byte[bin.length * 2];
    
    int j = 0;
    for (int i = 0; i < bin.length; i++) {
      hex[j++] = chars[0x0f & (bin[i] >>> 4)];
      hex[j++] = chars[0x0f & bin[i]];
    }
    
    return hex;
  }

  @Override
  public byte[] followingKey(int size, byte[] per) {
    return fromBinary(Utils.followingKey(size, toBinary(per)));
  }
  
  @Override
  public byte[] lastPossibleKey(int size, byte[] er) {
    return fromBinary(Utils.lastPossibleKey(size, toBinary(er)));
  }
  
}
