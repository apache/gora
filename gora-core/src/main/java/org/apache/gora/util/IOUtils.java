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

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter; 
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.util.ByteBufferInputStream;
import org.apache.avro.util.ByteBufferOutputStream;
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

  public static final int BUFFER_SIZE = 8192;

  private static BinaryDecoder decoder;

  private static Configuration getOrCreateConf(Configuration conf) {
    return conf != null ? conf : new Configuration();
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
    throw new IOException("cannot read from DataInput of instance:"
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

  /**
   * Serializes the object to the given data output using
   * available Hadoop serializations.
   *
   * @param conf Hadoop conf.
   * @param obj object instance to be serialized.
   * @param out data stream which serialized content is written.
   * @param objClass Class type of the object to be serialized.
   * @param <T> class type of object to be serialized.
   * @throws IOException occurred while serializing the object to bytes.
   */
  public static<T> void serialize(Configuration conf, DataOutput out
      , T obj, Class<T> objClass) throws IOException {

    SerializationFactory serializationFactory = new SerializationFactory(getOrCreateConf(conf));
    Serializer<T> serializer = serializationFactory.getSerializer(objClass);

    try (ByteBufferOutputStream os = new ByteBufferOutputStream()) {
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
    }
  }

  /**
   * Serializes the object to the given data output using
   * available Hadoop serializations.
   *
   * @param <T> class type of object to be serialized.
   * @param conf Hadoop conf.
   * @param obj object instance to be serialized.
   * @param out data stream which serialized content is written.
   * @throws IOException occurred while serializing the object to bytes.
   */
  @SuppressWarnings("unchecked")
  public static<T> void serialize(Configuration conf, DataOutput out
      , T obj) throws IOException {
    Text.writeString(out, obj.getClass().getName());
    serialize(conf, out, obj, (Class<T>)obj.getClass());
  }

  /**
   * Serializes the object to the given data output using
   * available Hadoop serializations
   *
   * @param conf Hadoop conf.
   * @param <T> class type of object to be serialized.
   * @param obj object instance to be serialized.
   * @return serialized byte array.
   * @throws IOException occurred while serializing the object to bytes.
   */
  public static<T> byte[] serialize(Configuration conf, T obj) throws IOException {
    DataOutputBuffer buffer = new DataOutputBuffer();
    serialize(conf, buffer, obj);
    return buffer.getData();
  }


  /**
   * Serializes the field object using the datumWriter.
   *
   * @param <T> class type of object to be serialized.
   * @param datumWriter AVRO datum writer for given schema.
   * @param object object to be serialized.
   * @param os output stream which serialized content is written.
   * @throws IOException occurred while serializing the object to bytes.
   */
  public static<T extends SpecificRecord> void serialize(OutputStream os,
      SpecificDatumWriter<T> datumWriter, T object)
      throws IOException {

    BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(os, null);
    datumWriter.write(object, encoder);
    encoder.flush();
  }

  /**
   * Serializes the field object using the datumWriter.
   *
   * @param <T> class type of object to be serialized.
   * @param datumWriter AVRO datum writer for given schema.
   * @param object object to be serialized.
   * @param os output stream which serialized content is written.
   * @throws IOException occurred while serializing the object to bytes.
   */
  public static<T> void serialize(OutputStream os,
      SpecificDatumWriter<T> datumWriter, T object)
      throws IOException {

    BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(os, null);
    datumWriter.write(object, encoder);
    encoder.flush();
  }
  
  /**
   * Serializes the field object using the datumWriter.
   *
   * @param <T> class type of object to be serialized.
   * @param datumWriter AVRO datum writer for given schema.
   * @param object object to be serialized.
   * @return serialized byte array.
   * @throws IOException occurred while serializing the object to bytes.
   */
  public static<T extends SpecificRecord> byte[] serialize(SpecificDatumWriter<T> datumWriter
      , T object) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    serialize(os, datumWriter, object);
    return os.toByteArray();
  }

  /**
   * Serializes the field object using the datumWriter.
   *
   * @param <T> class type of object to be serialized.
   * @param datumWriter AVRO datum writer for given schema.
   * @param object object to be serialized.
   * @return serialized byte array.
   * @throws IOException occurred while serializing the object to bytes.
   */
  public static<T> byte[] serialize(SpecificDatumWriter<T> datumWriter
      , T object) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    serialize(os, datumWriter, object);
    return os.toByteArray();
  }
  
  /**
   * Deserializes the object in the given data input using
   * available Hadoop serializations.
   *
   * @param conf Hadoop conf.
   * @param in data input stream where serialized content is read.
   * @param <T> object class type.
   * @param obj data object.
   * @param objClass object class type as String.
   * @return deserialized object.
   * @throws IOException occurred while deserializing the byte content.
   * @throws ClassNotFoundException class definition cannot be found for given class name.
   */
  @SuppressWarnings("unchecked")
  public static<T> T deserialize(Configuration conf, DataInput in
      , T obj , String objClass) throws IOException, ClassNotFoundException {

    Class<T> c = (Class<T>) ClassLoadingUtils.loadClass(objClass);

    return deserialize(conf, in, obj, c);
  }

  /**
   * Deserializes the object in the given data input using
   * available Hadoop serializations.
   *
   * @param conf Hadoop conf.
   * @param in data input stream where serialized content is read.
   * @param <T> object class type.
   * @param obj data object.
   * @param objClass object class type.
   * @throws IOException occurred while deserializing the byte content.
   * @return deserialized object.
   */
  public static<T> T deserialize(Configuration conf, DataInput in
      , T obj , Class<T> objClass) throws IOException {
    SerializationFactory serializationFactory = new SerializationFactory(getOrCreateConf(conf));
    Deserializer<T> deserializer = serializationFactory.getDeserializer(
        objClass);

    int length = WritableUtils.readVInt(in);
    byte[] arr = new byte[length];
    in.readFully(arr);
    List<ByteBuffer> list = new ArrayList<>();
    list.add(ByteBuffer.wrap(arr));

    try (ByteBufferInputStream is = new ByteBufferInputStream(list)) {
      deserializer.open(is);
      return deserializer.deserialize(obj);

    }finally {
      if(deserializer != null)
        deserializer.close();
    }
  }

  /**
   * Deserializes the object in the given data input using
   * available Hadoop serializations.
   *
   * @param conf Hadoop conf.
   * @param in data input stream where serialized content is read.
   * @param <T> object class type.
   * @param obj data object.
   * @throws IOException occurred while deserializing the byte content.
   * @throws ClassNotFoundException class definition cannot be found for given class name.
   * @return deserialized object.
   */
  @SuppressWarnings("unchecked")
  public static<T> T deserialize(Configuration conf, DataInput in
      , T obj) throws IOException, ClassNotFoundException {
    String clazz = Text.readString(in);
    Class<T> c = (Class<T>)ClassLoadingUtils.loadClass(clazz);
    return deserialize(conf, in, obj, c);
  }

  /**
   * Deserializes the object in the given data input using
   * available Hadoop serializations.
   *
   * @param conf Hadoop conf.
   * @param in serialized byte array of  object.
   * @param <T> object class type.
   * @param obj data object.
   * @throws IOException occurred while deserializing the byte content.
   * @throws ClassNotFoundException class definition cannot be found for given class name.
   * @return deserialized object.
   */
  public static<T> T deserialize(Configuration conf, byte[] in
      , T obj) throws IOException, ClassNotFoundException {
    try (DataInputBuffer buffer = new DataInputBuffer()) {
      buffer.reset(in, in.length);
      return deserialize(conf, buffer, obj);
    }
  }

  /**
   * Deserializes the field object using the datumReader.
   *
   * @param is data input stream.
   * @param datumReader AVRO datum reader associated schema.
   * @param <T> field class type.
   * @param <K> key class type.
   * @param object data field object.
   * @return deserialized field object.
   * @throws IOException while deserializing the byte content.
   */
  public static<K, T extends SpecificRecord> T deserialize(InputStream is,
      SpecificDatumReader<T> datumReader, T object)
      throws IOException {
    decoder = DecoderFactory.get().binaryDecoder(is, decoder);
    return datumReader.read(object, decoder);
  }

  /**
   * Deserializes the field object using the datumReader.
   *
   * @param bytes serialized byte array of field.
   * @param datumReader AVRO datum reader associated schema.
   * @param <T> field class type.
   * @param <K> key class type.
   * @param object data field object.
   * @return deserialized field object.
   * @throws IOException while deserializing the byte content.
   */
  public static<K, T extends SpecificRecord> T deserialize(byte[] bytes,
      SpecificDatumReader<T> datumReader, T object)
      throws IOException {
    decoder = DecoderFactory.get().binaryDecoder(bytes, decoder);
    return datumReader.read(object, decoder);
  }

  /**
   * Deserializes the field object using the datumReader.
   *
   * @param bytes serialized byte array of field.
   * @param datumReader AVRO datum reader associated schema.
   * @param object data field object.
   * @param <T> field class type.
   * @param <K> key class type.
   * @return deserialized field object.
   * @throws IOException while deserializing the byte content.
   */
  public static<K, T> T deserialize(byte[] bytes,
      SpecificDatumReader<T> datumReader, T object)
      throws IOException {
    decoder = DecoderFactory.get().binaryDecoder(bytes, decoder);
    return datumReader.read(object, decoder);
  }
  
  /**
   * Writes a byte[] to the output, representing whether each given field is null
   * or not. A Vint and ceil( fields.length / 8 ) bytes are written to the output.
   *
   * @param out the output to write to.
   * @param fields the fields to check for null. @see #readNullFieldsInfo(DataInput).
   * @throws IOException when writing the data to the stream.
   */
  public static void writeNullFieldsInfo(DataOutput out, Object ... fields)
    throws IOException {

    boolean[] isNull = new boolean[fields.length];

    for(int i=0; i<fields.length; i++) {
      isNull[i] = fields[i] == null;
    }

    writeBoolArray(out, isNull);
  }

  /**
   * Reads the data written by {@link #writeNullFieldsInfo(DataOutput, Object...)}
   * and returns a boolean array representing whether each field is null or not.
   *
   * @param in the input to read from.
   * @return a boolean[] representing whether each field is null or not.
   * @throws IOException when value is too long to fit in integer.
   */
  public static boolean[] readNullFieldsInfo(DataInput in) throws IOException {
    return readBoolArray(in);
  }

  /**
   * Writes a boolean[] to the output.
   *
   * @param out the output to write to.
   * @param boolArray boolean array.
   * @throws IOException when writing the data to the stream.
   */
  public static void writeBoolArray(DataOutput out, boolean[] boolArray)
    throws IOException {

    WritableUtils.writeVInt(out, boolArray.length);

    byte b = 0;
    int i;
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
   * Reads a boolean[] from input.
   *
   * @param in data input stream to read values.
   * @return boolean array.
   * @throws IOException when value too long to fit in integer.
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
   *
   * @param out encoder instance which wraps the output stream where data is written.
   * @param boolArray boolean array.
   * @throws IOException when failed writing the data to the stream.
   */
  public static void writeBoolArray(Encoder out, boolean[] boolArray)
    throws IOException {

    out.writeInt(boolArray.length);

    int byteArrLength = (int)Math.ceil(boolArray.length / 8.0);

    byte b = 0;
    byte[] arr = new byte[byteArrLength];
    int i;
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
   * Reads a boolean[] from input.
   *
   * @param in decoder instance which wraps the input stream where data is read.
   * @return boolean array.
   * @throws IOException when failed reading the data from stream.
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
   *
   * @param out the data output to write to.
   * @param arr the array to write. @see #readStringArray(DataInput).
   * @throws IOException when failed writing the data to output stream.
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
   *
   * @param in the data input to read from.
   * @return read String[].
   * @throws IOException when failed reading the data from stream.
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
   * Stores the given object in the configuration under the given dataKey.
   *
   * @param obj the object to store.
   * @param conf the configuration to store the object into.
   * @param dataKey the key to store the data.
   * @param <T> the given object class type.
   * @throws IOException when failed storing the given data in Hadoop conf.
   */
  public static<T> void storeToConf(T obj, Configuration conf, String dataKey)
    throws IOException {
    String classKey = dataKey + "._class";
    conf.set(classKey, obj.getClass().getName());
    DefaultStringifier.store(conf, obj, dataKey);
  }

  /**
   * Loads the object stored by {@link #storeToConf(Object, Configuration, String)}
   * method from the configuration under the given dataKey.
   *
   * @param conf the configuration to read from.
   * @param dataKey the key to get the data from.
   * @param <T> the given object class type.
   * @return the store object.
   * @throws IOException when failed retrieving the data given key from Hadoop conf.
   */
  @SuppressWarnings("unchecked")
  public static<T> T loadFromConf(Configuration conf, String dataKey)
    throws IOException {
    String classKey = dataKey + "._class";
    String className = conf.get(classKey);
    try {
      return (T) DefaultStringifier.load(conf, dataKey, ClassLoadingUtils.loadClass(className));
    } catch (IOException | ClassNotFoundException ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Copies the contents of the buffers into a single byte[].
   *
   * @param buffers input buffers to be merged.
   * @return merged byte array.
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
   * Reads until the end of the input stream, and returns the contents as a byte[].
   *
   * @param in input stream where the data is read.
   * @return byte array of read data from the stream.
   * @throws IOException when failed reading the data from input stream.
   */
  public static byte[] readFully(InputStream in) throws IOException {
    List<ByteBuffer> buffers = new ArrayList<>(4);
    while(true) {
      ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
      int count = in.read(buffer.array(), 0, BUFFER_SIZE);
      if(count > 0) {
        buffer.limit(count);
        buffers.add(buffer);
      }
      if(count < BUFFER_SIZE)
        break;
    }

    return getAsBytes(buffers);
  }

}
