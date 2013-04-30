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

package org.apache.gora.cassandra.query;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import me.prettyprint.cassandra.serializers.FloatSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.util.Utf8;
import org.apache.gora.cassandra.serializers.GenericArraySerializer;
import org.apache.gora.cassandra.serializers.StatefulHashMapSerializer;
import org.apache.gora.cassandra.serializers.TypeUtils;
import org.apache.gora.cassandra.store.CassandraStore;
import org.apache.gora.persistency.StatefulHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraSubColumn extends CassandraColumn {
  public static final Logger LOG = LoggerFactory.getLogger(CassandraSubColumn.class);

  private static final String ENCODING = "UTF-8";
  
  private static CharsetEncoder charsetEncoder = Charset.forName(ENCODING).newEncoder();;


  /**
   * Key-value pair containing the raw data.
   */
  private HColumn<ByteBuffer, ByteBuffer> hColumn;

  public ByteBuffer getName() {
    return hColumn.getName();
  }

  /**
   * Deserialize a String into an typed Object, according to the field schema.
   * @see org.apache.gora.cassandra.query.CassandraColumn#getValue()
   */
  public Object getValue() {
    Field field = getField();
    Schema fieldSchema = field.schema();
    Type type = fieldSchema.getType();
    ByteBuffer byteBuffer = hColumn.getValue();
    if (byteBuffer == null) {
      return null;
    }
    Object value = null;
    if (type == Type.ARRAY) {
      GenericArraySerializer serializer = GenericArraySerializer.get(fieldSchema.getElementType());
      GenericArray genericArray = serializer.fromByteBuffer(byteBuffer);
      value = genericArray;
    } else if (type == Type.MAP) {
      StatefulHashMapSerializer serializer = StatefulHashMapSerializer.get(fieldSchema.getValueType());
      StatefulHashMap map = serializer.fromByteBuffer(byteBuffer);
      value = map;
    } else if (type == Type.UNION){
      // the selected union schema is obtained
      Schema unionFieldSchema = getUnionSchema(super.getUnionType(), field.schema());
      // we use the selected union schema to deserialize our actual value
      value = fromByteBuffer(unionFieldSchema, byteBuffer);
    } else {
      value = fromByteBuffer(fieldSchema, byteBuffer);
    }

    return value;
  }
  
  /**
   * Gets the specific schema for a union data type
   * @param pSchemaPos
   * @param pSchema
   * @return
   */
  private Schema getUnionSchema (int pSchemaPos, Schema pSchema){
    Schema unionSchema = pSchema.getTypes().get(pSchemaPos);
    // default union element
    if ( unionSchema == null )
      pSchema.getTypes().get(CassandraStore.DEFAULT_UNION_SCHEMA);
    return unionSchema;
  }

  public void setValue(HColumn<ByteBuffer, ByteBuffer> hColumn) {
    this.hColumn = hColumn;
  }
}
