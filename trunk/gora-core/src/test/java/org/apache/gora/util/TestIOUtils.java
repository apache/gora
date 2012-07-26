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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.Assert;

import org.apache.avro.ipc.ByteBufferInputStream;
import org.apache.avro.ipc.ByteBufferOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.gora.mapreduce.GoraMapReduceUtils;
import org.apache.gora.util.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.junit.Test;

/**
 * Test case for {@link IOUtils} class.
 */
public class TestIOUtils {

  public static final Logger log = LoggerFactory.getLogger(TestIOUtils.class);
  
  public static Configuration conf = new Configuration();

  private static final int BOOL_ARRAY_MAX_LENGTH = 30;
  private static final int STRING_ARRAY_MAX_LENGTH = 30;
  
  private static class BoolArrayWrapper implements Writable {
    boolean[] arr;
    @SuppressWarnings("unused")
    public BoolArrayWrapper() {
    }
    public BoolArrayWrapper(boolean[] arr) {
      this.arr = arr;
    }
    @Override
    public void readFields(DataInput in) throws IOException {
      this.arr = IOUtils.readBoolArray(in);
    }
    @Override
    public void write(DataOutput out) throws IOException {
      IOUtils.writeBoolArray(out, arr);
    }
    @Override
    public boolean equals(Object obj) {
      return Arrays.equals(arr, ((BoolArrayWrapper)obj).arr);
    }
  }
  
  private static class StringArrayWrapper implements Writable {
    String[] arr;
    @SuppressWarnings("unused")
    public StringArrayWrapper() {
    }
    public StringArrayWrapper(String[] arr) {
      this.arr = arr;
    }
    @Override
    public void readFields(DataInput in) throws IOException {
      this.arr = IOUtils.readStringArray(in);
    }
    @Override
    public void write(DataOutput out) throws IOException {
      IOUtils.writeStringArray(out, arr);
    }
    @Override
    public boolean equals(Object obj) {
      return Arrays.equals(arr, ((StringArrayWrapper)obj).arr);
    }
  }
  
  @SuppressWarnings("unchecked")
  public static <T> void testSerializeDeserialize(T... objects) throws Exception {
    ByteBufferOutputStream os = new ByteBufferOutputStream();
    DataOutputStream dos = new DataOutputStream(os);
    ByteBufferInputStream is = null;
    DataInputStream dis = null;
    
    GoraMapReduceUtils.setIOSerializations(conf, true);
    
    try {
      for(T before : objects) {
        IOUtils.serialize(conf, dos , before, (Class<T>)before.getClass());
        dos.flush();
      }
       
      is = new ByteBufferInputStream(os.getBufferList());
      dis = new DataInputStream(is);
      
      for(T before : objects) {
        T after = IOUtils.deserialize(conf, dis, null, (Class<T>)before.getClass());
        
        log.info("Before: " + before);
        log.info("After : " + after);
        
        Assert.assertEquals(before, after);
      }
      
      //assert that the end of input is reached
      try {
        long skipped = dis.skip(1);
        Assert.assertEquals(0, skipped);
      }catch (EOFException expected) {
        //either should throw exception or return 0 as skipped
      }
    }finally {
      org.apache.hadoop.io.IOUtils.closeStream(dos);
      org.apache.hadoop.io.IOUtils.closeStream(os);
      org.apache.hadoop.io.IOUtils.closeStream(dis);
      org.apache.hadoop.io.IOUtils.closeStream(is);
    }
  }
  
  @Test
  public void testWritableSerde() throws Exception {
    Text text = new Text("foo goes to a bar to get some buzz");
    testSerializeDeserialize(text);
  }
  
  @Test
  public void testJavaSerializableSerde() throws Exception {
    Integer integer = Integer.valueOf(42);
    testSerializeDeserialize(integer);
  }
  
  @Test
  public void testReadWriteBoolArray() throws Exception {
    
    boolean[][] patterns = {
        {true},
        {false},
        {true, false},
        {false, true},
        {true, false, true},
        {false, true, false},
        {false, true, false, false, true, true, true},
        {false, true, false, false, true, true, true, true},
        {false, true, false, false, true, true, true, true, false},
    };
    
    for(int i=0; i<BOOL_ARRAY_MAX_LENGTH; i++) {
      for(int j=0; j<patterns.length; j++) {
        boolean[] arr = new boolean[i];
        for(int k=0; k<i; k++) {
          arr[k] = patterns[j][k % patterns[j].length];
        }
        
        testSerializeDeserialize(new BoolArrayWrapper(arr));
      }
    }
  }
  
  @Test
  public void testReadWriteNullFieldsInfo() throws IOException {

    Integer n = null; //null
    Integer nn = new Integer(42); //not null

    testNullFieldsWith(nn);
    testNullFieldsWith(n);
    testNullFieldsWith(n, nn);
    testNullFieldsWith(nn, n);
    testNullFieldsWith(nn, n, nn, n);
    testNullFieldsWith(nn, n, nn, n, n, n, nn, nn, nn, n, n);
  }

  private void testNullFieldsWith( Object ... values ) throws IOException {
    DataOutputBuffer out = new DataOutputBuffer();
    DataInputBuffer in = new DataInputBuffer();

    IOUtils.writeNullFieldsInfo(out, values);

    in.reset(out.getData(), out.getLength());

    boolean[] ret = IOUtils.readNullFieldsInfo(in);

    //assert
    Assert.assertEquals(values.length, ret.length);

    for(int i=0; i<values.length; i++) {
      Assert.assertEquals( values[i] == null , ret[i]);
    }
  }
  
  @Test
  public void testReadWriteStringArray() throws Exception {
    for(int i=0; i<STRING_ARRAY_MAX_LENGTH; i++) {
      String[] arr = new String[i];
      for(int j=0; j<i; j++) {
        arr[j] = String.valueOf(j);
      }
      
      testSerializeDeserialize(new StringArrayWrapper(arr));
    }
  }
  
  @Test
  public void testReadFullyBufferLimit() throws IOException {
    for(int i=-2; i<=2; i++) {
      byte[] bytes = new byte[IOUtils.BUFFER_SIZE + i];
      for(int j=0; j<bytes.length; j++) {
        bytes[j] = (byte)j;
      }
      ByteArrayInputStream is = new ByteArrayInputStream(bytes);
      
      byte[] readBytes = IOUtils.readFully(is);
      assertByteArrayEquals(bytes, readBytes);
    }
  }
  
  public void assertByteArrayEquals(byte[] expected, byte[] actual) {
    Assert.assertEquals("Array lengths do not match", expected.length, actual.length);
    for(int j=0; j<expected.length; j++) {
      Assert.assertEquals("bytes at position "+j+" do not match", expected[j], actual[j]);
    }
  }
}
