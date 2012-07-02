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

package org.apache.gora.dynamodb.query;

import java.nio.ByteBuffer;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * Represents a unit of data: a key value pair tagged by a family name
 */
public abstract class DynamoDBColumn {
  //public static final Logger LOG = LoggerFactory.getLogger(DynamoDBColumn.class);

  public static final int SUB = 0;
  public static final int SUPER = 1;
  
  private String family;
  private int type;
  
  public String getFamily() {
    return family;
  }
  public void setFamily(String family) {
    this.family = family;
  }
  public int getType() {
    return type;
  }
  public void setType(int type) {
    this.type = type;
  }
  
  public abstract ByteBuffer getName();
  public abstract Object getValue();
  

/*  protected Object fromByteBuffer(Type type, ByteBuffer byteBuffer) {
    Object value = null;
    switch (type) {
      case STRING:
        value = new Utf8(StringSerializer.get().fromByteBuffer(byteBuffer));
        break;
      case BYTES:
        value = byteBuffer;
        break;
      case INT:
        value = IntegerSerializer.get().fromByteBuffer(byteBuffer);
        break;
      case LONG:
        value = LongSerializer.get().fromByteBuffer(byteBuffer);
        break;
      case FLOAT:
        value = FloatSerializer.get().fromByteBuffer(byteBuffer);
        break;
      case DOUBLE:
        value = DoubleSerializer.get().fromByteBuffer(byteBuffer);
        break;

      default:
        LOG.info("Type is not supported: " + type);

    }
    return value;
  }
*/
}
