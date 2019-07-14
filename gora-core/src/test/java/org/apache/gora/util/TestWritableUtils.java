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
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

import org.apache.gora.util.WritableUtils;
import org.junit.Test;

/**
 * Test case for {@link WritableUtils} class.
 */
public class TestWritableUtils {
  @Test
  public void testWritesReads() throws Exception {
    Properties props = new Properties();
    props.put("keyBlah", "valueBlah");
    props.put("keyBlah2", "valueBlah2");
    
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutput out = new DataOutputStream(bos);
    WritableUtils.writeProperties(out, props);
    ((DataOutputStream)out).flush();
    
    DataInput in = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
    
    Properties propsRead = WritableUtils.readProperties(in);
    
    assertEquals(propsRead.get("keyBlah"), props.get("keyBlah"));
    assertEquals(propsRead.get("keyBlah2"), props.get("keyBlah2"));
  }
}
