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
package org.apache.gora.redis.store;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.apache.gora.redis.encoders.Encoder;
import org.apache.gora.redis.encoders.SignedBinaryEncoder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * 
 */
public class PartitionTest {
  // TODO test more types

  private static Encoder encoder = new SignedBinaryEncoder();

  static long encl(long l) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    try {
      dos.writeLong(l);
      dos.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return encoder.decodeLong(baos.toByteArray());
  }

  @Test
  public void test1() throws IOException {
    assertEquals(encl(0x006f000000000000l), (long) RedisStore.followingKey(encoder, Long.class, new byte[] {0x00, 0x6f}));
    assertEquals(encl(1l), (long) RedisStore.followingKey(encoder, Long.class, new byte[] {0, 0, 0, 0, 0, 0, 0, 0}));
    assertEquals(encl(0x106f000000000001l), (long) RedisStore.followingKey(encoder, Long.class, new byte[] {0x10, 0x6f, 0, 0, 0, 0, 0, 0}));
    assertEquals(
        encl(-1l),
        (long) RedisStore.followingKey(encoder, Long.class, new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff,
            (byte) 0xfe}));
    
    assertEquals(encl(0x8000000000000001l), (long) RedisStore.followingKey(encoder, Long.class, new byte[] {(byte) 0x80, 0, 0, 0, 0, 0, 0, 0}));
    assertEquals(
        encl(0x8000000000000000l),
        (long) RedisStore.followingKey(encoder, Long.class, new byte[] {(byte) 0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff}));


    try {
      RedisStore.followingKey(encoder, Long.class,
          new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
      assertTrue(false);
    } catch (IllegalArgumentException iea) {
      
    }
  }
  
  @Test
  public void test2() throws IOException {
    assertEquals(encl(0x00ffffffffffffffl), (long) RedisStore.lastPossibleKey(encoder, Long.class, new byte[] {0x01}));
    assertEquals(encl(0x006effffffffffffl), (long) RedisStore.lastPossibleKey(encoder, Long.class, new byte[] {0x00, 0x6f}));
    assertEquals(encl(0xff6effffffffffffl), (long) RedisStore.lastPossibleKey(encoder, Long.class, new byte[] {(byte) 0xff, 0x6f}));
    assertEquals(encl(0xfffeffffffffffffl), (long) RedisStore.lastPossibleKey(encoder, Long.class, new byte[] {(byte) 0xff, (byte) 0xff}));
    assertEquals(encl(0l), (long) RedisStore.lastPossibleKey(encoder, Long.class, new byte[] {(byte) 0, 0, 0, 0, 0, 0, 0, 0}));
    
    assertEquals(encl(0x7effffffffffffffl), (long) RedisStore.lastPossibleKey(encoder, Long.class, new byte[] {(byte) 0x7f}));
    assertEquals(encl(0x7fffffffffffffffl), (long) RedisStore.lastPossibleKey(encoder, Long.class, new byte[] {(byte) 0x80}));
    assertEquals(encl(0x80ffffffffffffffl), (long) RedisStore.lastPossibleKey(encoder, Long.class, new byte[] {(byte) 0x81}));

    try {
      RedisStore.lastPossibleKey(encoder, Long.class, new byte[] {(byte) 0, 0, 0, 0, 0, 0, 0});
      assertTrue(false);
    } catch (IllegalArgumentException iea) {
      
    }
  }
}
