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
package org.apache.gora.mapreduce;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.Serialization;
import org.apache.hadoop.io.serializer.Serializer;

public class StringSerialization implements Serialization<String> {

  @Override
  public boolean accept(Class<?> c) {
    return c.equals(String.class);
  }

  @Override
  public Deserializer<String> getDeserializer(Class<String> c) {
    return new Deserializer<String>() {
      private DataInputStream in;

      @Override
      public void open(InputStream in) throws IOException {
        this.in = new DataInputStream(in);
      }

      @Override
      public void close() throws IOException {
        this.in.close();
      }

      @Override
      public String deserialize(String t) throws IOException {
        return Text.readString(in);
      }
    };
  }

  @Override
  public Serializer<String> getSerializer(Class<String> c) {
    return new Serializer<String>() {

      private DataOutputStream out;

      @Override
      public void close() throws IOException {
        this.out.close();
      }

      @Override
      public void open(OutputStream out) throws IOException {
        this.out = new DataOutputStream(out);
      }

      @Override
      public void serialize(String str) throws IOException {
        Text.writeString(out, str);
      }
    };
  }
}
