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
package org.apache.gora.accumulo.encoders;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.gora.accumulo.util.FixedByteArrayOutputStream;

/**
 * 
 */
public class BinaryEncoder implements Encoder {

  @Override
  public byte[] encodeShort(short s) throws IOException {
    return encodeShort(s, new byte[2]);
  }

  @Override
  public byte[] encodeShort(short s, byte[] ret) throws IOException {
    try (DataOutputStream dos = new DataOutputStream(new FixedByteArrayOutputStream(ret))){
      dos.writeShort(s);
      dos.close();
      return ret;
    }
  }

  @Override
  public short decodeShort(byte[] a) throws IOException {
    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(a))){
      short s = dis.readShort();
      dis.close();
      return s;
    }
  }

  @Override
  public byte[] encodeInt(int i) throws IOException {
    return encodeInt(i, new byte[4]);
  }

  @Override
  public byte[] encodeInt(int i, byte[] ret) throws IOException {
    try (DataOutputStream dos = new DataOutputStream(new FixedByteArrayOutputStream(ret))){
      dos.writeInt(i);
      dos.close();
      return ret;
    }
  }

  @Override
  public int decodeInt(byte[] a) throws IOException {
    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(a))){
      int i = dis.readInt();
      dis.close();
      return i;
    }
  }

  @Override
  public byte[] encodeLong(long l) throws IOException {
    return encodeLong(l, new byte[8]);
  }

  @Override
  public byte[] encodeLong(long l, byte[] ret) throws IOException {
    try (DataOutputStream dos = new DataOutputStream(new FixedByteArrayOutputStream(ret))){
      dos.writeLong(l);
      dos.close();
      return ret;
    }
  }

  @Override
  public long decodeLong(byte[] a) throws IOException {
    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(a))){
      long l = dis.readLong();
      dis.close();
      return l;
    }
  }

  @Override
  public byte[] encodeDouble(double d) throws IOException {
    return encodeDouble(d, new byte[8]);
  }

  @Override
  public byte[] encodeDouble(double d, byte[] ret) throws IOException {
    try (DataOutputStream dos = new DataOutputStream(new FixedByteArrayOutputStream(ret))){
      long l = Double.doubleToRawLongBits(d);
      dos.writeLong(l);
      dos.close();
      return ret;
    }
  }

  @Override
  public double decodeDouble(byte[] a) throws IOException {
    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(a))){
      long l = dis.readLong();
      dis.close();
      return Double.longBitsToDouble(l);
    }
  }

  @Override
  public byte[] encodeFloat(float d) throws IOException {
    return encodeFloat(d, new byte[4]);
  }

  @Override
  public byte[] encodeFloat(float f, byte[] ret) throws IOException {
    try (DataOutputStream dos = new DataOutputStream(new FixedByteArrayOutputStream(ret))){
      int i = Float.floatToRawIntBits(f);
      dos.writeInt(i);
      return ret;
    }
  }

  @Override
  public float decodeFloat(byte[] a) throws IOException {
    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(a))){
      int i = dis.readInt();
      return Float.intBitsToFloat(i);
    }
  }

  @Override
  public byte[] encodeByte(byte b, byte[] ret) {
    ret[0] = 0;
    return ret;
  }

  @Override
  public byte[] encodeByte(byte b) {
    return encodeByte(b, new byte[1]);
  }

  @Override
  public byte decodeByte(byte[] a) {
    return a[0];
  }

  @Override
  public boolean decodeBoolean(byte[] a) throws IOException {
    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(a))){
      return dis.readBoolean();
    }
  }

  @Override
  public byte[] encodeBoolean(boolean b) throws IOException {
    return encodeBoolean(b, new byte[1]);
  }

  @Override
  public byte[] encodeBoolean(boolean b, byte[] ret) throws IOException {
    try (DataOutputStream dos = new DataOutputStream(new FixedByteArrayOutputStream(ret))){
      dos.writeBoolean(b);
      return ret;
    }
  }

  @Override
  public byte[] lastPossibleKey(int size, byte[] er) {
    return Utils.lastPossibleKey(size, er);
  }

  @Override
  public byte[] followingKey(int size, byte[] per) {
    return Utils.followingKey(size, per);
  }
}
