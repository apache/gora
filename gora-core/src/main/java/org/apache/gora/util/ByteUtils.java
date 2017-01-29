/**
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
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.util.Utf8;
import org.apache.hadoop.io.WritableUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//  This code is copied almost directly from HBase project's Bytes class.
/**
 * Utility class that handles byte arrays, conversions to/from other types.
 *
 */
public class ByteUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ByteUtils.class);

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
   *
   * @param tgtBytes the byte array which source/input bytes are written.
   * @param tgtOffset offset position in the array to be considered to write bytes.
   * @param srcBytes source of byte to write out on target byte array.
   * @param srcOffset offset of source byte array to be considered.
   * @param srcLength length from offset of source byte array to be considered.
   * @return incremented offset.
   */
  public static int putBytes(byte[] tgtBytes, int tgtOffset, byte[] srcBytes,
      int srcOffset, int srcLength) {
    System.arraycopy(srcBytes, srcOffset, tgtBytes, tgtOffset, srcLength);
    return tgtOffset + srcLength;
  }

  /**
   * Write a single byte out to the specified byte array position.
   *
   * @param bytes the byte array input.
   * @param offset position in the array.
   * @param b byte to write out.
   * @return incremented offset.
   */
  public static int putByte(byte[] bytes, int offset, byte b) {
    bytes[offset] = b;
    return offset + 1;
  }

  /**
   * Returns a new byte array, copied from the passed ByteBuffer.
   *
   * @param bb A ByteBuffer as input.
   * @return the byte array of <code>bb</code>.
   */
  public static byte[] toBytes(ByteBuffer bb) {
    int length = bb.limit();
    byte [] result = new byte[length];
    System.arraycopy(bb.array(), bb.arrayOffset(), result, 0, length);
    return result;
  }

  /**
   * Converts a byte array to it s String representation.
   *
   * @param b Presumed UTF-8 encoded byte array.
   * @return String made from <code>b</code>.
   */
  public static String toString(final byte [] b) {
    if (b == null) {
      return null;
    }
    return toString(b, 0, b.length);
  }

  /**
   * Converts two byte array given a separator string in between to single String representation.
   *
   * @param b1 Presumed UTF-8 encoded byte array input 1.
   * @param b2 Presumed UTF-8 encoded byte array input 2.
   * @param sep Separator String.
   * @return String made from <code>b1</code>+<code>sep</code>+<code>b2</code>.
   */
  public static String toString(final byte [] b1,
                                String sep,
                                final byte [] b2) {
    return toString(b1, 0, b1.length) + sep + toString(b2, 0, b2.length);
  }

  /**
   * Converts byte array given offset and length to it s String representation.
   *
   * @param b Presumed UTF-8 encoded byte array.
   * @param off offset to be from the input byte array.
   * @param len length to be considered from the offset.
   * @return String made from <code>b</code>.
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
      LOG.error(e.getMessage());
      throw new RuntimeException(e);
    }
    return result;
  }

  /**
   * Converts a string to a UTF-8 byte array.
   *
   * @param s input String value.
   * @return the byte array.
   */
  public static byte[] toBytes(String s) {
    if (s == null) {
      throw new IllegalArgumentException("string cannot be null");
    }
    byte [] result = null;
    try {
      result = s.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.error(e.getMessage());
      throw new RuntimeException(e);
    }
    return result;
  }

  /**
   * Convert a boolean to a byte array.
   *
   * @param b input boolean value.
   * @return <code>b</code> encoded in a byte array.
   */
  public static byte [] toBytes(final boolean b) {
    byte [] bb = new byte[1];
    bb[0] = b? (byte)-1: (byte)0;
    return bb;
  }

  /**
   * Convert a byte array to it s boolean value.
   *
   * @param b input byte array.
   * @return boolean value either True or false.
   */
  public static boolean toBoolean(final byte [] b) {
    if (b == null || b.length > 1) {
      throw new IllegalArgumentException("Array is wrong size");
    }
    return b[0] != (byte)0;
  }

  /**
   * Convert a long value to a byte array.
   *
   * @param val input long value.
   * @return the byte array.
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
   * Converts a byte array to a long value.
   *
   * @param bytes input byte array.
   * @return the long value.
   */
  public static long toLong(byte[] bytes) {
    return toLong(bytes, 0);
  }

  /**
   * Converts a byte array to a long value.
   *
   * @param bytes input byte array.
   * @param offset offset to be from the input byte array.
   * @return the long value.
   */
  public static long toLong(byte[] bytes, int offset) {
    return toLong(bytes, offset, SIZEOF_LONG);
  }

  /**
   * Converts a byte array to a long value.
   *
   * @param bytes input byte array.
   * @param offset offset to be from the input byte array.
   * @param length length to be considered from the offset.
   * @return the long value.
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
   * Presumes float encoded as IEEE 754 floating-point "single format".
   *
   * @param bytes offset to be considered from byte array.
   * @return Float made from passed byte array.
   */
  public static float toFloat(byte [] bytes) {
    return toFloat(bytes, 0);
  }

  /**
   * Presumes float encoded as IEEE 754 floating-point "single format".
   *
   * @param bytes input byte array to be considered.
   * @param offset offset to be considered from byte array.
   * @return Float made from passed byte array.
   */
  public static float toFloat(byte [] bytes, int offset) {
    int i = toInt(bytes, offset);
    return Float.intBitsToFloat(i);
  }
  /**
   * Converts float value to it s byte array representation.
   *
   * @param f input float value.
   * @return the float represented as byte [].
   */
  public static byte [] toBytes(final float f) {
    // Encode it as int
    int i = Float.floatToRawIntBits(f);
    return toBytes(i);
  }

  /**
   * Converts byte array to a double value.
   *
   * @param bytes input byte array to be considered.
   * @return Return double made from passed bytes.
   */
  public static double toDouble(final byte [] bytes) {
    return toDouble(bytes, 0);
  }

  /**
   * Converts byte array given offset to double value.
   *
   * @param bytes input byte array.
   * @param offset offset to be considered from input array.
   * @return Return double made from passed bytes.
   */
  public static double toDouble(final byte [] bytes, final int offset) {
    long l = toLong(bytes, offset);
    return Double.longBitsToDouble(l);
  }

  /**
   * Converts double value to byte array.
   *
   * @param d double input value.
   * @return the double represented as byte [].
   */
  public static byte [] toBytes(final double d) {
    // Encode it as a long
    long l = Double.doubleToRawLongBits(d);
    return toBytes(l);
  }

  /**
   * Convert an int value to a byte array.
   *
   * @param val int input value.
   * @return the byte array.
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
   * Converts a byte array to an int value.
   *
   * @param bytes input byte array to be converted.
   * @return the int value
   */
  public static int toInt(byte[] bytes) {
    return toInt(bytes, 0);
  }

  /**
   * Converts a byte array to an int value.
   *
   * @param bytes input byte array.
   * @param offset input byte array offset.
   * @return the int value.
   */
  public static int toInt(byte[] bytes, int offset) {
    return toInt(bytes, offset, SIZEOF_INT);
  }

  /**
   * Converts a byte array to an int value.
   *
   * @param bytes input byte array.
   * @param offset input byte array offset.
   * @param length length from offset position.
   * @return the int value.
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
   * Convert a short value to a byte array.
   *
   * @param val input short value.
   * @return the byte array.
   */
  public static byte[] toBytes(short val) {
    byte[] b = new byte[SIZEOF_SHORT];
    b[1] = (byte)(val);
    val >>= 8;
    b[0] = (byte)(val);
    return b;
  }

  /**
   * Converts a byte array to a short value.
   *
   * @param bytes input byte array.
   * @return the short value.
   */
  public static short toShort(byte[] bytes) {
    return toShort(bytes, 0);
  }

  /**
   * Converts a byte array to a short value.
   *
   * @param bytes input byte array.
   * @param offset offset position of input array.
   * @return the short value.
   */
  public static short toShort(byte[] bytes, int offset) {
    return toShort(bytes, offset, SIZEOF_SHORT);
  }

  /**
   * Converts a byte array to a short value.
   *
   * @param bytes input byte array.
   * @param offset offset position from input byte array.
   * @param length length to be considered from offset.
   * @return the short value.
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
   * Convert a char value to a byte array.
   *
   * @param val input char value.
   * @return the byte array.
   */
  public static byte[] toBytes(char val) {
    byte[] b = new byte[SIZEOF_CHAR];
    b[1] = (byte) (val);
    val >>= 8;
    b[0] = (byte) (val);
    return b;
  }

  /**
   * Converts a byte array to a char value.
   *
   * @param bytes input bytes array.
   * @return the char value.
   */
  public static char toChar(byte[] bytes) {
    return toChar(bytes, 0);
  }


  /**
   * Converts a byte array to a char value.
   *
   * @param bytes input bytes array.
   * @param offset offset to be considered from input array.
   * @return the char value.
   */
  public static char toChar(byte[] bytes, int offset) {
    return toChar(bytes, offset, SIZEOF_CHAR);
  }

  /**
   * Converts a byte array to a char value.
   *
   * @param bytes input byte array.
   * @param offset offset to be considered from input array.
   * @param length length to be considered from offset.
   * @return the char value.
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
   * Converts a byte array to a char array value.
   *
   * @param bytes input byte array.
   * @return the char value.
   */
  public static char[] toChars(byte[] bytes) {
    return toChars(bytes, 0, bytes.length);
  }

  /**
   * Converts a byte array to a char array value.
   *
   * @param bytes input byte array.
   * @param offset offset to be considered from input array.
   * @return the char value.
   */
  public static char[] toChars(byte[] bytes, int offset) {
    return toChars(bytes, offset, bytes.length-offset);
  }

  /**
   * Converts a byte array to a char array value.
   *
   * @param bytes input byte array.
   * @param offset offset to be considered from input array.
   * @param length length to be considered from offset.
   * @return the char value.
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
   * Converts a int value to it s variable length byte array representation.
   *
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
   * Converts variable length byte array to it s long value representation.
   *
   * @param buffer input byte array.
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
   * Converts variable length byte array to it s  int value representation.
   *
   * @param buffer input byte array.
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
   *
   * @param buffer Binary input array.
   * @param offset Offset into array at which vint begins.
   * @throws java.io.IOException when read error occurs while extracting data from input stream.
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
   * Compare byte two byte arrays, and returns a int value accordingly.
   *
   * @param left left byte array to be considered.
   * @param right right byte array to be considered.
   * @return 0 if equal, {@literal <} 0 if left is less than right, etc.
   */
  public static int compareTo(final byte [] left, final byte [] right) {
    return compareTo(left, 0, left.length, right, 0, right.length);
  }

  /**
   * Compare byte two byte arrays given offset and length, and returns a int value accordingly.
   *
   * @param b1 left byte array to be considered.
   * @param b2 right byte array to be considered.
   * @param s1 Where to start comparing in the left buffer.
   * @param s2 Where to start comparing in the right buffer.
   * @param l1 How much to compare from the left buffer.
   * @param l2 How much to compare from the right buffer.
   * @return 0 if equal, {@literal <} 0 if left is less than right, etc.
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
   * Compare byte two byte arrays for equality, and returns a boolean value accordingly.
   *
   * @param left left byte array to be considered.
   * @param right right byte array to be considered.
   * @return True if equal.
   */
  public static boolean equals(final byte [] left, final byte [] right) {
    // Could use Arrays.equals?
    return left == null && right == null? true:
      (left == null || right == null || (left.length != right.length))? false:
        compareTo(left, right) == 0;
  }

  @SuppressWarnings("unchecked")
  public static <T> T fromBytes( byte[] val, Schema schema
      , SpecificDatumReader<T> datumReader, T object)
  throws IOException {
    Type type = schema.getType();
    switch (type) {
    case ENUM:
      String symbol = schema.getEnumSymbols().get(val[0]);
      return (T)Enum.valueOf(ReflectData.get().getClass(schema), symbol);
    case STRING:  return (T)new Utf8(toString(val));
    case BYTES:   return (T)ByteBuffer.wrap(val);
    case INT:     return (T)Integer.valueOf(bytesToVint(val));
    case LONG:    return (T)Long.valueOf(bytesToVlong(val));
    case FLOAT:   return (T)Float.valueOf(toFloat(val));
    case DOUBLE:  return (T)Double.valueOf(toDouble(val));
    case BOOLEAN: return (T)Boolean.valueOf(val[0] != 0);
    case RECORD:  //fall
    case MAP:
    case ARRAY:   return (T)IOUtils.deserialize(val, (SpecificDatumReader<SpecificRecord>)datumReader, (SpecificRecord)object);
    default: throw new RuntimeException("Unknown type: "+type);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> byte[] toBytes(T o, Schema schema
      , SpecificDatumWriter<T> datumWriter)
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
    case ARRAY:   return IOUtils.serialize((SpecificDatumWriter<SpecificRecord>)datumWriter, (SpecificRecord)o);
    default: throw new RuntimeException("Unknown type: "+type);
    }
  }
}