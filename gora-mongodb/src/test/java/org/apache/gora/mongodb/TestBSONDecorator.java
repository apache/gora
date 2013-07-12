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
package org.apache.gora.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.apache.gora.mongodb.utils.BSONDecorator;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class TestBSONDecorator {

  @Test
  public void testContainsField() {
    // Init the object used for testing
    DBObject dbo1 = BasicDBObjectBuilder
        .start()
        .add("root0", "value")
        .add("root1", new BasicDBObject("leaf1", 1))
        .add("root2",
                new BasicDBObject("parent1", new BasicDBObject("leaf2", "test")))
        .get();
    BSONDecorator dboc = new BSONDecorator(dbo1);

    // Root level field, does exist
    assertTrue(dboc.containsField("root0"));
    // Root level field, does not exist
    assertFalse(dboc.containsField("doestNotExist"));

    // 1-deep level field, does exist
    assertTrue(dboc.containsField("root1.leaf1"));
    // 1-deep level field, parent does not exist
    assertFalse(dboc.containsField("doesNotExist.leaf2"));
    // 1-deep level field, leaf does not exist
    assertFalse(dboc.containsField("root1.doestNotExist"));

    // 3-deep level field, does exist
    assertTrue(dboc.containsField("root2.parent1.leaf2"));
    // 3-deep level field, leaf does not exist
    assertFalse(dboc.containsField("root2.parent1.doestNotExist"));
    // 3-deep level field, first parent does not exist
    assertFalse(dboc.containsField("doesNotExist.parent1.leaf2"));
    // 3-deep level field, intermediate parent does not exist
    assertFalse(dboc.containsField("root2.doesNotExist.leaf2"));
  }

  @Test
  public void testBinaryField() {
    // Init the object used for testing
    DBObject dbo1 = BasicDBObjectBuilder
        .start()
        .add("root0", "value")
        .add("root1", new BasicDBObject("leaf1", "abcdefgh".getBytes()))
        .add(
                "root2",
                new BasicDBObject("parent1", new BasicDBObject("leaf2", "test"
                        .getBytes())))
        .add("root3", ByteBuffer.wrap("test2".getBytes())).get();
    BSONDecorator dboc = new BSONDecorator(dbo1);

    // Access first bytes field
    assertTrue(dboc.containsField("root1.leaf1"));
    assertArrayEquals("abcdefgh".getBytes(), dboc.getBytes("root1.leaf1")
        .array());

    // Access second bytes field
    assertTrue(dboc.containsField("root2.parent1.leaf2"));
    assertArrayEquals("test".getBytes(), dboc.getBytes("root2.parent1.leaf2")
        .array());

    // Access third bytes field
    assertTrue(dboc.containsField("root3"));
    assertArrayEquals("test2".getBytes(), dboc.getBytes("root3").array());
  }

}
