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
/**
 * Copyright 2009 The Apache Software Foundation
 *
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.util.Utf8;
import org.apache.gora.avro.PersistentDatumReader;
import org.apache.gora.avro.PersistentDatumWriter;
import org.apache.hadoop.io.WritableUtils;

//  This code is copied almost directly from HBase project's Bytes class.
/**
 * Utility class that handles byte arrays, conversions to/from other types.
 *
 */
public class ByteUtils {

  /**
   * Size of boolean in bytes
   */
  public static final int SIZEOF_BOOLEAN = Byte.SIZE/Byte.SIZE;

  /**
   * Size of byte in bytes
   */
  public static final int SIZEOF_BYTE = SIZEOF_BOOLEAN;

  /**
   * Size of char in bytes
   */
  public static final int SIZEOF_CHAR = Character.SIZE/Byte.SIZE;

  /**
   * Size of double in bytes
   */
  public static final int SIZEOF_DOUBLE = Double.SIZE/Byte.SIZE;

  /**
   * Size of float in bytes
   */
  public static final int SIZEOF_FLOAT = Float.SIZE/Byte.SIZE;

  /**
   * Size of int in bytes
   */
  public static final int SIZEOF_INT = Integer.SIZE/Byte.SIZE;

  /**
   * Size of long in bytes
   */
  public static final int SIZEOF_LONG = Long.SIZE/Byte.SIZE;

  /**
   * Size of short in bytes
   */
  public static final int SIZEOF_SHORT = Short.SIZE/Byte.SIZE;

  /**
   * Put bytes at the specified byte array position.
   * @param tgtBytes the byte array
   * @param tgtOffset position in the array
   * @param srcBytes byte to write out
   * @param srcOffset
   * @param srcLength
   * @return incremented offset
   */
  public static int putBytes(byte[] tgtBytes, int tgtOffset, byte[] srcBytes,
      int srcOffset, int srcLength) {
    System.arraycopy(srcBytes, srcOffset, tgtBytes, tgtOffset, srcLength);
    return tgtOffset + srcLength;
  }

  /**
   * Write a single byte out to the specified byte array position.
   * @param bytes the byte array
   * @param offset position in the array
   * @param b byte to write out
   * @return incremented offset
   */
  public static int putByte(byte[] bytes, int offset, byte b) {
    bytes[offset] = b;
    return offset + 1;
  }

  /**
   * Returns a new byte array, copied from the passed ByteBuffer.
   * @param bb A ByteBuffer
   * @return the byte array
   */
  public static byte[] toBytes(ByteBuffer bb) {
    int length = bb.limit();
    byte [] result = new byte[length];
    System.arraycopy(bb.array(), bb.arrayOffset(), result, 0, length);
    return result;
  }

  /**
   * @param b Presumed UTF-8 encoded byte array.
   * @return String made from <code>b</code>
   */
  public static String toString(final byte [] b) {
    if (b == null) {
      return null;
    }
    return toString(b, 0, b.length);
  }

  public static String toString(final byte [] b1,
                                String sep,
                                final byte [] b2) {
    return toString(b1, 0, b1.length) + sep + toString(b2, 0, b2.length);
  }

  /**
   * @param b Presumed UTF-8 encoded byte array.
   * @param off
   * @param len
   * @return String made from <code>b</code>
   */
  public static String toString(final byte [] b, int off, int len) {
    if(b == null) {
      return null;
    }
    if(len == 0) {
      return "";
    }
    String result = null;
    try {
      result = new String(b, off, len, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return result;
  }
  /**
   * Converts a string to a UTF-8 byte array.
   * @param s
   * @return the byte array
   */
  public static byte[] toBytes(String s) {
    if (s == null) {
      throw new IllegalArgumentException("string cannot be null");
    }
    byte [] result = null;
    try {
      result = s.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * Convert a boolean to a byte array.
   * @param b
   * @return <code>b</code> encoded in a byte array.
   */
  public static byte [] toBytes(final boolean b) {
    byte [] bb = new byte[1];
    bb[0] = b? (byte)-1: (byte)0;
    return bb;
  }

  /**
   * @param b
   * @return True or false.
   */
  public static boolean toBoolean(final byte [] b) {
    if (b == null || b.length > 1) {
      throw new IllegalArgumentException("Array is wrong size");
    }
    return b[0] != (byte)0;
  }

  /**
   * Convert a long value to a byte array
   * @param val
   * @return the byte array
   */
  public static byte[] toBytes(long val) {
    byte [] b = new byte[8];
    for(int i=7;i>0;i--) {
      b[i] = (byte)(val);
      val >>>= 8;
    }
    b[0] = (byte)(val);
    return b;
  }

  /**
   * Converts a byte array to a long value
   * @param bytes
   * @return the long value
   */
  public static long toLong(byte[] bytes) {
    return toLong(bytes, 0);
  }

  /**
   * Converts a byte array to a long value
   * @param bytes
   * @param offset
   * @return the long value
   */
  public static long toLong(byte[] bytes, int offset) {
    return toLong(bytes, offset, SIZEOF_LONG);
  }

  /**
   * Converts a byte array to a long value
   * @param bytes
   * @param offset
   * @param length
   * @return the long value
   */
  public static long toLong(byte[] bytes, int offset, final int length) {
    if (bytes == null || length != SIZEOF_LONG ||
        (offset + length > bytes.length)) {
      return -1L;
    }
    long l = 0;
    for(int i = offset; i < (offset + length); i++) {
      l <<= 8;
      l ^= (long)bytes[i] & 0xFF;
    }
    return l;
  }

  /**
   * Presumes float encoded as IEEE 754 floating-point "single format"
   * @param bytes
   * @return Float made from passed byte array.
   */
  public static float toFloat(byte [] bytes) {
    return toFloat(bytes, 0);
  }

  /**
   * Presumes float encoded as IEEE 754 floating-point "single format"
   * @param bytes
   * @param offset
   * @return Float made from passed byte array.
   */
  public static float toFloat(byte [] bytes, int offset) {
    int i = toInt(bytes, offset);
    return Float.intBitsToFloat(i);
  }
  /**
   * @param f
   * @return the float represented as byte []
   */
  public static byte [] toBytes(final float f) {
    // Encode it as int
    int i = Float.floatToRawIntBits(f);
    return toBytes(i);
  }

  /**
   * @param bytes
   * @return Return double made from passed bytes.
   */
  public static double toDouble(final byte [] bytes) {
    return toDouble(bytes, 0);
  }

  /**
   * @param bytes
   * @param offset
   * @return Return double made from passed bytes.
   */
  public static double toDouble(final byte [] bytes, final int offset) {
    long l = toLong(bytes, offset);
    return Double.longBitsToDouble(l);
  }

  /**
   * @param d
   * @return the double represented as byte []
   */
  public static byte [] toBytes(final double d) {
    // Encode it as a long
    long l = Double.doubleToRawLongBits(d);
    return toBytes(l);
  }

  /**
   * Convert an int value to a byte array
   * @param val
   * @return the byte array
   */
  public static byte[] toBytes(int val) {
    byte [] b = new byte[4];
    for(int i = 3; i > 0; i--) {
      b[i] = (byte)(val);
      val >>>= 8;
    }
    b[0] = (byte)(val);
    return b;
  }

  /**
   * Converts a byte array to an int value
   * @param bytes
   * @return the int value
   */
  public static int toInt(byte[] bytes) {
    return toInt(bytes, 0);
  }

  /**
   * Converts a byte array to an int value
   * @param bytes
   * @param offset
   * @return the int value
   */
  public static int toInt(byte[] bytes, int offset) {
    return toInt(bytes, offset, SIZEOF_INT);
  }

  /**
   * Converts a byte array to an int value
   * @param bytes
   * @param offset
   * @param length
   * @return the int value
   */
  public static int toInt(byte[] bytes, int offset, final int length) {
    if (bytes == null || length != SIZEOF_INT ||
        (offset + length > bytes.length)) {
      return -1;
    }
    int n = 0;
    for(int i = offset; i < (offset + length); i++) {
      n <<= 8;
      n ^= bytes[i] & 0xFF;
    }
    return n;
  }

  /**
   * Convert a short value to a byte array
   * @param val
   * @return the byte array
   */
  public static byte[] toBytes(short val) {
    byte[] b = new byte[SIZEOF_SHORT];
    b[1] = (byte)(val);
    val >>= 8;
    b[0] = (byte)(val);
    return b;
  }

  /**
   * Converts a byte array to a short value
   * @param bytes
   * @return the short value
   */
  public static short toShort(byte[] bytes) {
    return toShort(bytes, 0);
  }

  /**
   * Converts a byte array to a short value
   * @param bytes
   * @param offset
   * @return the short value
   */
  public static short toShort(byte[] bytes, int offset) {
    return toShort(bytes, offset, SIZEOF_SHORT);
  }

  /**
   * Converts a byte array to a short value
   * @param bytes
   * @param offset
   * @param length
   * @return the short value
   */
  public static short toShort(byte[] bytes, int offset, final int length) {
    if (bytes == null || length != SIZEOF_SHORT ||
        (offset + length > bytes.length)) {
      return -1;
    }
    short n = 0;
    n ^= bytes[offset] & 0xFF;
    n <<= 8;
    n ^= bytes[offset+1] & 0xFF;
    return n;
  }

  /**
   * Convert a char value to a byte array
   *
   * @param val
   * @return the byte array
   */
  public static byte[] toBytes(char val) {
    byte[] b = new byte[SIZEOF_CHAR];
    b[1] = (byte) (val);
    val >>= 8;
    b[0] = (byte) (val);
    return b;
  }

  /**
   * Converts a byte array to a char value
   *
   * @param bytes
   * @return the char value
   */
  public static char toChar(byte[] bytes) {
    return toChar(bytes, 0);
  }


  /**
   * Converts a byte array to a char value
   *
   * @param bytes
   * @param offset
   * @return the char value
   */
  public static char toChar(byte[] bytes, int offset) {
    return toChar(bytes, offset, SIZEOF_CHAR);
  }

  /**
   * Converts a byte array to a char value
   *
   * @param bytes
   * @param offset
   * @param length
   * @return the char value
   */
  public static char toChar(byte[] bytes, int offset, final int length) {
    if (bytes == null || length != SIZEOF_CHAR ||
      (offset + length > bytes.length)) {
      return (char)-1;
    }
    char n = 0;
    n ^= bytes[offset] & 0xFF;
    n <<= 8;
    n ^= bytes[offset + 1] & 0xFF;
    return n;
  }

  /**
   * Converts a byte array to a char array value
   *
   * @param bytes
   * @return the char value
   */
  public static char[] toChars(byte[] bytes) {
    return toChars(bytes, 0, bytes.length);
  }

  /**
   * Converts a byte array to a char array value
   *
   * @param bytes
   * @param offset
   * @return the char value
   */
  public static char[] toChars(byte[] bytes, int offset) {
    return toChars(bytes, offset, bytes.length-offset);
  }

  /**
   * Converts a byte array to a char array value
   *
   * @param bytes
   * @param offset
   * @param length
   * @return the char value
   */
  public static char[] toChars(byte[] bytes, int offset, final int length) {
    int max = offset + length;
    if (bytes == null || (max > bytes.length) || length %2 ==1) {
      return null;
    }

    char[] chars = new char[length / 2];
    for (int i = 0, j = offset; i < chars.length && j < max; i++, j += 2) {
      char c = 0;
      c ^= bytes[j] & 0xFF;
      c <<= 8;
      c ^= bytes[j + 1] & 0xFF;
      chars[i] = c;
    }
    return chars;
  }

  /**
   * @param vint Integer to make a vint of.
   * @return Vint as bytes array.
   */
  public static byte [] vintToBytes(final long vint) {
    long i = vint;
    int size = WritableUtils.getVIntSize(i);
    byte [] result = new byte[size];
    int offset = 0;
    if (i >= -112 && i <= 127) {
      result[offset] = ((byte)i);
      return result;
    }

    int len = -112;
    if (i < 0) {
      i ^= -1L; // take one's complement'
      len = -120;
    }

    long tmp = i;
    while (tmp != 0) {
      tmp = tmp >> 8;
    len--;
    }

    result[offset++] = (byte)len;

    len = (len < -120) ? -(len + 120) : -(len + 112);

    for (int idx = len; idx != 0; idx--) {
      int shiftbits = (idx - 1) * 8;
      long mask = 0xFFL << shiftbits;
      result[offset++] = (byte)((i & mask) >> shiftbits);
    }
    return result;
  }

  /**
   * @param buffer
   * @return vint bytes as an integer.
   */
  public static long bytesToVlong(final byte [] buffer) {
    int offset = 0;
    byte firstByte = buffer[offset++];
    int len = WritableUtils.decodeVIntSize(firstByte);
    if (len == 1) {
      return firstByte;
    }
    long i = 0;
    for (int idx = 0; idx < len-1; idx++) {
      byte b = buffer[offset++];
      i = i << 8;
      i = i | (b & 0xFF);
    }
    return (WritableUtils.isNegativeVInt(firstByte) ? (i ^ -1L) : i);
  }

  /**
   * @param buffer
   * @return vint bytes as an integer.
   */
  public static int bytesToVint(final byte [] buffer) {
    int offset = 0;
    byte firstByte = buffer[offset++];
    int len = WritableUtils.decodeVIntSize(firstByte);
    if (len == 1) {
      return firstByte;
    }
    long i = 0;
    for (int idx = 0; idx < len-1; idx++) {
      byte b = buffer[offset++];
      i = i << 8;
      i = i | (b & 0xFF);
    }
    return (int)(WritableUtils.isNegativeVInt(firstByte) ? (i ^ -1L) : i);
  }

  /**
   * Reads a zero-compressed encoded long from input stream and returns it.
   * @param buffer Binary array
   * @param offset Offset into array at which vint begins.
   * @throws java.io.IOException
   * @return deserialized long from stream.
   */
  public static long readVLong(final byte [] buffer, final int offset)
  throws IOException {
    byte firstByte = buffer[offset];
    int len = WritableUtils.decodeVIntSize(firstByte);
    if (len == 1) {
      return firstByte;
    }
    long i = 0;
    for (int idx = 0; idx < len-1; idx++) {
      byte b = buffer[offset + 1 + idx];
      i = i << 8;
      i = i | (b & 0xFF);
    }
    return (WritableUtils.isNegativeVInt(firstByte) ? (i ^ -1L) : i);
  }

  /**
   * @param left
   * @param right
   * @return 0 if equal, < 0 if left is less than right, etc.
   */
  public static int compareTo(final byte [] left, final byte [] right) {
    return compareTo(left, 0, left.length, right, 0, right.length);
  }

  /**
   * @param b1
   * @param b2
   * @param s1 Where to start comparing in the left buffer
   * @param s2 Where to start comparing in the right buffer
   * @param l1 How much to compare from the left buffer
   * @param l2 How much to compare from the right buffer
   * @return 0 if equal, < 0 if left is less than right, etc.
   */
  public static int compareTo(byte[] b1, int s1, int l1,
      byte[] b2, int s2, int l2) {
    // Bring WritableComparator code local
    int end1 = s1 + l1;
    int end2 = s2 + l2;
    for (int i = s1, j = s2; i < end1 && j < end2; i++, j++) {
      int a = (b1[i] & 0xff);
      int b = (b2[j] & 0xff);
      if (a != b) {
        return a - b;
      }
    }
    return l1 - l2;
  }

  /**
   * @param left
   * @param right
   * @return True if equal
   */
  public static boolean equals(final byte [] left, final byte [] right) {
    // Could use Arrays.equals?
    return left == null && right == null? true:
      (left == null || right == null || (left.length != right.length))? false:
        compareTo(left, right) == 0;
  }

  @SuppressWarnings("unchecked")
  public static Object fromBytes( byte[] val, Schema schema
      , PersistentDatumReader<?> datumReader, Object object)
  throws IOException {
    Type type = schema.getType();
    switch (type) {
    case ENUM:
      String symbol = schema.getEnumSymbols().get(val[0]);
      return Enum.valueOf(ReflectData.get().getClass(schema), symbol);
    case STRING:  return new Utf8(toString(val));
    case BYTES:   return ByteBuffer.wrap(val);
    case INT:     return bytesToVint(val);
    case LONG:    return bytesToVlong(val);
    case FLOAT:   return toFloat(val);
    case DOUBLE:  return toDouble(val);
    case BOOLEAN: return val[0] != 0;
    case RECORD:  //fall
    case MAP:
    case ARRAY:   return IOUtils.deserialize(val, datumReader, schema, object);
    default: throw new RuntimeException("Unknown type: "+type);
    }
  }

  public static byte[] toBytes(Object o, Schema schema
      , PersistentDatumWriter<?> datumWriter)
  throws IOException {
    Type type = schema.getType();
    switch (type) {
    case STRING:  return toBytes(((Utf8)o).toString()); // TODO: maybe ((Utf8)o).getBytes(); ?
    case BYTES:   return ((ByteBuffer)o).array();
    case INT:     return vintToBytes((Integer)o);
    case LONG:    return vintToBytes((Long)o);
    case FLOAT:   return toBytes((Float)o);
    case DOUBLE:  return toBytes((Double)o);
    case BOOLEAN: return (Boolean)o ? new byte[] {1} : new byte[] {0};
    case ENUM:    return new byte[] { (byte)((Enum<?>) o).ordinal() };
    case RECORD:  //fall
    case MAP:
    case ARRAY:   return IOUtils.serialize(datumWriter, schema, o);
    default: throw new RuntimeException("Unknown type: "+type);
    }
  }
}