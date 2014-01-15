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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.gora.cassandra.serializers.CharSequenceSerializer;
import org.apache.gora.persistency.impl.PersistentBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraSuperColumn extends CassandraColumn {
  public static final Logger LOG = LoggerFactory.getLogger(CassandraSuperColumn.class);

  private HSuperColumn<String, ByteBuffer, ByteBuffer> hSuperColumn;
  
  public ByteBuffer getName() {
    return StringSerializer.get().toByteBuffer(hSuperColumn.getName());
  }

  public Object getValue() {
    Field field = getField();
    Schema fieldSchema = field.schema();
    Type type = fieldSchema.getType();
    
    Object value = null;
    
    switch (type) {
      case ARRAY:
        List<Object> array = new ArrayList<Object>();
        
        for (HColumn<ByteBuffer, ByteBuffer> hColumn : this.hSuperColumn.getColumns()) {
          Object memberValue = fromByteBuffer(fieldSchema.getElementType(), hColumn.getValue());
          // int i = IntegerSerializer().get().fromByteBuffer(hColumn.getName());
          array.add(memberValue);      
        }
        value = array;
        
        break;
      case MAP:
        Map<CharSequence, Object> map = new HashMap<CharSequence, Object>();
        
        for (HColumn<ByteBuffer, ByteBuffer> hColumn : this.hSuperColumn.getColumns()) {
          Object memberValue = null;
          memberValue = fromByteBuffer(fieldSchema.getValueType(), hColumn.getValue());
          map.put(CharSequenceSerializer.get().fromByteBuffer(hColumn.getName()), memberValue);      
        }
        value = map;
        
        break;
      case RECORD:
        String fullName = fieldSchema.getFullName();
        
        Class<?> claz = null;
        try {
          claz = Class.forName(fullName);
        } catch (ClassNotFoundException cnfe) {
          LOG.warn("Unable to load class " + fullName, cnfe);
          break;
        }

        try {
          value = claz.newInstance();          
        } catch (InstantiationException ie) {
          LOG.warn("Instantiation error", ie);
          break;
        } catch (IllegalAccessException iae) {
          LOG.warn("Illegal access error", iae);
          break;
        }
        
        // we updated the value instance, now update its members
        if (value instanceof PersistentBase) {
          PersistentBase record = (PersistentBase) value;

          for (HColumn<ByteBuffer, ByteBuffer> hColumn : this.hSuperColumn.getColumns()) {
            String memberName = StringSerializer.get().fromByteBuffer(hColumn.getName());
            if (memberName == null || memberName.length() == 0) {
              LOG.warn("member name is null or empty.");
              continue;
            }
            Field memberField = fieldSchema.getField(memberName);
            CassandraSubColumn cassandraColumn = new CassandraSubColumn();
            cassandraColumn.setField(memberField);
            cassandraColumn.setValue(hColumn);
            record.put(record.getSchema().getField(memberName).pos(), cassandraColumn.getValue());
          }
        }
        break;
      default:
        LOG.warn("Type: " + type.name() + " not supported for field: " + field.name());
    }
    
    return value;
  }

  public void setValue(HSuperColumn<String, ByteBuffer, ByteBuffer> hSuperColumn) {
    this.hSuperColumn = hSuperColumn;
  }

}
