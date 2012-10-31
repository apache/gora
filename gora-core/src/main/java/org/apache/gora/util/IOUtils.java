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

package org.apache.gora.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.ipc.ByteBufferInputStream;
import org.apache.avro.ipc.ByteBufferOutputStream;
import org.apache.gora.avro.PersistentDatumReader;
import org.apache.gora.avro.PersistentDatumWriter;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.DefaultStringifier;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.SerializationFactory;
import org.apache.hadoop.io.serializer.Serializer;

/**
 * An utility class for I/O related functionality.
 */
public class IOUtils {

  private static SerializationFactory serializationFactory = null;
  private static Configuration conf;

  public static final int BUFFER_SIZE = 8192;

  private static BinaryDecoder decoder;

  private static Configuration getOrCreateConf(Configuration conf) {
    if(conf == null) {
      if(IOUtils.conf == null) {
        IOUtils.conf = new Configuration();
      }
    }
    return conf != null ? conf : IOUtils.conf;
  }

  public static Object readObject(DataInput in)
    throws ClassNotFoundException, IOException {

    if(in instanceof ObjectInput) {
      return ((ObjectInput)in).readObject();
    } else {
      if(in instanceof InputStream) {
        ObjectInput objIn = new ObjectInputStream((InputStream)in);
        Object obj = objIn.readObject();
        return obj;
      }
    }
    throw new IOException("cannot write to DataOutput of instance:"
        + in.getClass());
  }

  public static void writeObject(DataOutput out, Object obj)
    throws IOException {
    if(out instanceof ObjectOutput) {
      ((ObjectOutput)out).writeObject(obj);
    } else {
      if(out instanceof OutputStream) {
        ObjectOutput objOut = new ObjectOutputStream((OutputStream)out);
        objOut.writeObject(obj);
      }
    }
    throw new IOException("cannot write to DataOutput of instance:"
        + out.getClass());
  }

  /** Serializes the object to the given dataoutput using
   * available Hadoop serializations
   * @throws IOException */
  public static<T> void serialize(Configuration conf, DataOutput out
      , T obj, Class<T> objClass) throws IOException {

    if(serializationFactory == null) {
      serializationFactory = new SerializationFactory(getOrCreateConf(conf));
    }
    Serializer<T> serializer = serializationFactory.getSerializer(objClass);

    ByteBufferOutputStream os = new ByteBufferOutputStream();
    try {
      serializer.open(os);
      serializer.serialize(obj);

      int length = 0;
      List<ByteBuffer> buffers = os.getBufferList();
      for(ByteBuffer buffer : buffers) {
        length += buffer.limit() - buffer.arrayOffset();
      }

      WritableUtils.writeVInt(out, length);
      for(ByteBuffer buffer : buffers) {
        byte[] arr = buffer.array();
        out.write(arr, buffer.arrayOffset(), buffer.limit());
      }

    }finally {
      if(serializer != null)
        serializer.close();
      if(os != null)
        os.close();
    }
  }

  /** Serializes the object to the given dataoutput using
   * available Hadoop serializations
   * @throws IOException */
  @SuppressWarnings("unchecked")
  public static<T> void serialize(Configuration conf, DataOutput out
      , T obj) throws IOException {
    Text.writeString(out, obj.getClass().getCanonicalName());
    serialize(conf, out, obj, (Class<T>)obj.getClass());
  }

  /** Serializes the object to the given dataoutput using
   * available Hadoop serializations*/
  public static<T> byte[] serialize(Configuration conf, T obj) throws IOException {
    DataOutputBuffer buffer = new DataOutputBuffer();
    serialize(conf, buffer, obj);
    return buffer.getData();
  }


  /**
   * Serializes the field object using the datumWriter.
   */
  public static<T extends PersistentBase> void serialize(OutputStream os,
      PersistentDatumWriter<T> datumWriter, Schema schema, Object object)
      throws IOException {

    BinaryEncoder encoder = new BinaryEncoder(os);
    datumWriter.write(schema, object, encoder);
    encoder.flush();
  }

  /**
   * Serializes the field object using the datumWriter.
   */
  public static<T extends PersistentBase> byte[] serialize(PersistentDatumWriter<T> datumWriter
      , Schema schema, Object object) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    serialize(os, datumWriter, schema, object);
    return os.toByteArray();
  }

  /** Deserializes the object in the given datainput using
   * available Hadoop serializations.
   * @throws IOException
   * @throws ClassNotFoundException */
  @SuppressWarnings("unchecked")
  public static<T> T deserialize(Configuration conf, DataInput in
      , T obj , String objClass) throws IOException, ClassNotFoundException {

    Class<T> c = (Class<T>) ClassLoadingUtils.loadClass(objClass);

    return deserialize(conf, in, obj, c);
  }

  /** Deserializes the object in the given datainput using
   * available Hadoop serializations.
   * @throws IOException */
  public static<T> T deserialize(Configuration conf, DataInput in
      , T obj , Class<T> objClass) throws IOException {
    if(serializationFactory == null) {
      serializationFactory = new SerializationFactory(getOrCreateConf(conf));
    }
    Deserializer<T> deserializer = serializationFactory.getDeserializer(
        objClass);

    int length = WritableUtils.readVInt(in);
    byte[] arr = new byte[length];
    in.readFully(arr);
    List<ByteBuffer> list = new ArrayList<ByteBuffer>();
    list.add(ByteBuffer.wrap(arr));
    ByteBufferInputStream is = new ByteBufferInputStream(list);

    try {
      deserializer.open(is);
      T newObj = deserializer.deserialize(obj);
      return newObj;

    }finally {
      if(deserializer != null)
        deserializer.close();
      if(is != null)
        is.close();
    }
  }

  /** Deserializes the object in the given datainput using
   * available Hadoop serializations.
   * @throws IOException
   * @throws ClassNotFoundException */
  @SuppressWarnings("unchecked")
  public static<T> T deserialize(Configuration conf, DataInput in
      , T obj) throws IOException, ClassNotFoundException {
    String clazz = Text.readString(in);
    Class<T> c = (Class<T>)ClassLoadingUtils.loadClass(clazz);
    return deserialize(conf, in, obj, c);
  }

  /** Deserializes the object in the given datainput using
   * available Hadoop serializations.
   * @throws IOException
   * @throws ClassNotFoundException */
  public static<T> T deserialize(Configuration conf, byte[] in
      , T obj) throws IOException, ClassNotFoundException {
    DataInputBuffer buffer = new DataInputBuffer();
    buffer.reset(in, in.length);
    return deserialize(conf, buffer, obj);
  }

  /**
   * Deserializes the field object using the datumReader.
   */
  @SuppressWarnings("unchecked")
  public static<K, T extends PersistentBase> K deserialize(InputStream is,
      PersistentDatumReader<T> datumReader, Schema schema, K object)
      throws IOException {
    decoder = DecoderFactory.defaultFactory().createBinaryDecoder(is, decoder);
    return (K)datumReader.read(object, schema, decoder);
  }

  /**
   * Deserializes the field object using the datumReader.
   */
  @SuppressWarnings("unchecked")
  public static<K, T extends PersistentBase> K deserialize(byte[] bytes,
      PersistentDatumReader<T> datumReader, Schema schema, K object)
      throws IOException {
    decoder = DecoderFactory.defaultFactory().createBinaryDecoder(bytes, decoder);
    return (K)datumReader.read(object, schema, decoder);
  }


  /**
   * Serializes the field object using the datumWriter.
   */
  public static<T extends PersistentBase> byte[] deserialize(PersistentDatumWriter<T> datumWriter
      , Schema schema, Object object) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    serialize(os, datumWriter, schema, object);
    return os.toByteArray();
  }

  /**
   * Writes a byte[] to the output, representing whether each given field is null
   * or not. A Vint and ceil( fields.length / 8 ) bytes are written to the output.
   * @param out the output to write to
   * @param fields the fields to check for null
   * @see #readNullFieldsInfo(DataInput)
   */
  public static void writeNullFieldsInfo(DataOutput out, Object ... fields)
    throws IOException {

    boolean[] isNull = new boolean[fields.length];

    for(int i=0; i<fields.length; i++) {
      isNull[i] = (fields[i] == null);
    }

    writeBoolArray(out, isNull);
  }

  /**
   * Reads the data written by {@link #writeNullFieldsInfo(DataOutput, Object...)}
   * and returns a boolean array representing whether each field is null or not.
   * @param in the input to read from
   * @return a boolean[] representing whether each field is null or not.
   */
  public static boolean[] readNullFieldsInfo(DataInput in) throws IOException {
    return readBoolArray(in);
  }

  /**
   * Writes a boolean[] to the output.
   */
  public static void writeBoolArray(DataOutput out, boolean[] boolArray)
    throws IOException {

    WritableUtils.writeVInt(out, boolArray.length);

    byte b = 0;
    int i = 0;
    for(i=0; i<boolArray.length; i++) {
      if(i % 8 == 0 && i != 0) {
        out.writeByte(b);
        b = 0;
      }
      b >>= 1;
      if(boolArray[i])
        b |= 0x80;
      else
        b &= 0x7F;
    }
    if(i % 8 != 0) {
      for(int j=0; j < 8 - (i % 8); j++) { //shift for the remaining byte
        b >>=1;
        b &= 0x7F;
      }
    }

    out.writeByte(b);
  }

  /**
   * Reads a boolean[] from input
   * @throws IOException
   */
  public static boolean[] readBoolArray(DataInput in) throws IOException {
    int length = WritableUtils.readVInt(in);
    boolean[] arr = new boolean[length];

    byte b = 0;
    for(int i=0; i < length; i++) {
      if(i % 8 == 0) {
        b = in.readByte();
      }
      arr[i] = (b & 0x01) > 0;
      b >>= 1;
    }
    return arr;
  }


  /**
   * Writes a boolean[] to the output.
   */
  public static void writeBoolArray(Encoder out, boolean[] boolArray)
    throws IOException {

    out.writeInt(boolArray.length);

    int byteArrLength = (int)Math.ceil(boolArray.length / 8.0);

    byte b = 0;
    byte[] arr = new byte[byteArrLength];
    int i = 0;
    int arrIndex = 0;
    for(i=0; i<boolArray.length; i++) {
      if(i % 8 == 0 && i != 0) {
        arr[arrIndex++] = b;
        b = 0;
      }
      b >>= 1;
      if(boolArray[i])
        b |= 0x80;
      else
        b &= 0x7F;
    }
    if(i % 8 != 0) {
      for(int j=0; j < 8 - (i % 8); j++) { //shift for the remaining byte
        b >>=1;
        b &= 0x7F;
      }
    }

    arr[arrIndex++] = b;
    out.writeFixed(arr);
  }

  /**
   * Reads a boolean[] from input
   * @throws IOException
   */
  public static boolean[] readBoolArray(Decoder in) throws IOException {

    int length = in.readInt();
    boolean[] boolArr = new boolean[length];

    int byteArrLength = (int)Math.ceil(length / 8.0);
    byte[] byteArr = new byte[byteArrLength];
    in.readFixed(byteArr);

    int arrIndex = 0;
    byte b = 0;
    for(int i=0; i < length; i++) {
      if(i % 8 == 0) {
        b = byteArr[arrIndex++];
      }
      boolArr[i] = (b & 0x01) > 0;
      b >>= 1;
    }
    return boolArr;
  }

  /**
   * Writes the String array to the given DataOutput.
   * @param out the data output to write to
   * @param arr the array to write
   * @see #readStringArray(DataInput)
   */
  public static void writeStringArray(DataOutput out, String[] arr)
    throws IOException {
    WritableUtils.writeVInt(out, arr.length);
    for(String str : arr) {
      Text.writeString(out, str);
    }
  }

  /**
   * Reads and returns a String array that is written by
   * {@link #writeStringArray(DataOutput, String[])}.
   * @param in the data input to read from
   * @return read String[]
   */
  public static String[] readStringArray(DataInput in) throws IOException {
    int len = WritableUtils.readVInt(in);
    String[] arr = new String[len];
    for(int i=0; i<len; i++) {
      arr[i] = Text.readString(in);
    }
    return arr;
  }

  /**
   * Stores the given object in the configuration under the given dataKey
   * @param obj the object to store
   * @param conf the configuration to store the object into
   * @param dataKey the key to store the data
   */
  public static<T> void storeToConf(T obj, Configuration conf, String dataKey)
    throws IOException {
    String classKey = dataKey + "._class";
    conf.set(classKey, obj.getClass().getCanonicalName());
    DefaultStringifier.store(conf, obj, dataKey);
  }

  /**
   * Loads the object stored by {@link #storeToConf(Object, Configuration, String)}
   * method from the configuration under the given dataKey.
   * @param conf the configuration to read from
   * @param dataKey the key to get the data from
   * @return the store object
   */
  @SuppressWarnings("unchecked")
  public static<T> T loadFromConf(Configuration conf, String dataKey)
    throws IOException {
    String classKey = dataKey + "._class";
    String className = conf.get(classKey);
    try {
      T obj = (T) DefaultStringifier.load(conf, dataKey, ClassLoadingUtils.loadClass(className));
      return obj;
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Copies the contents of the buffers into a single byte[]
   */
  //TODO: not tested
  public static byte[] getAsBytes(List<ByteBuffer> buffers) {
    //find total size
    int size = 0;
    for(ByteBuffer buffer : buffers) {
      size += buffer.remaining();
    }

    byte[] arr = new byte[size];

    int offset = 0;
    for(ByteBuffer buffer : buffers) {
      int len = buffer.remaining();
      buffer.get(arr, offset, len);
      offset += len;
    }

    return arr;
  }

  /**
   * Reads until the end of the input stream, and returns the contents as a byte[]
   */
  public static byte[] readFully(InputStream in) throws IOException {
    List<ByteBuffer> buffers = new ArrayList<ByteBuffer>(4);
    while(true) {
      ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
      int count = in.read(buffer.array(), 0, BUFFER_SIZE);
      if(count > 0) {
        buffer.limit(count);
        buffers.add(buffer);
      }
      if(count < BUFFER_SIZE) break;
    }

    return getAsBytes(buffers);
  }

}