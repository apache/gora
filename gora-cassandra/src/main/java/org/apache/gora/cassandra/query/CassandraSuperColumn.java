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

import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.gora.cassandra.serializers.CharSequenceSerializer;
import org.apache.gora.cassandra.store.CassandraStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraSuperColumn extends CassandraColumn {
  public static final Logger LOG = LoggerFactory.getLogger(CassandraSuperColumn.class);

  private HSuperColumn<String, ByteBuffer, ByteBuffer> hSuperColumn;
  
  public ByteBuffer getName() {
    return StringSerializer.get().toByteBuffer(hSuperColumn.getName());
  }

 private Object getSuperValue(Field field, Schema fieldSchema, Type type){
    Object value = null;
    
    switch (type) {
      case ARRAY:
        List<Object> array = new ArrayList<>();
        
        for (HColumn<ByteBuffer, ByteBuffer> hColumn : this.hSuperColumn.getColumns()) {
          Object memberValue = fromByteBuffer(fieldSchema.getElementType(), hColumn.getValue());
          // int i = IntegerSerializer().get().fromByteBuffer(hColumn.getName());
          array.add(memberValue);      
        }
        value = array;
        
        break;
      case MAP:
        Map<CharSequence, Object> map = new HashMap<>();

        for (HColumn<ByteBuffer, ByteBuffer> hColumn : this.hSuperColumn.getColumns()) {
          CharSequence mapKey = CharSequenceSerializer.get().fromByteBuffer(hColumn.getName());
          if (!mapKey.toString().contains(CassandraStore.UNION_COL_SUFIX)) {
            Object memberValue = null;
            // We need detect real type for UNION Fields
            if (fieldSchema.getValueType().getType().equals(Type.UNION)){
              
              HColumn<ByteBuffer, ByteBuffer> cc = getUnionTypeColumn(mapKey
                  + CassandraStore.UNION_COL_SUFIX, this.hSuperColumn.getColumns());
              Integer unionIndex = getUnionIndex(cc);
              Schema realSchema = fieldSchema.getValueType().getTypes().get(unionIndex);
              memberValue = fromByteBuffer(realSchema, hColumn.getValue());
              
            }else{
              memberValue = fromByteBuffer(fieldSchema.getValueType(), hColumn.getValue());            
            }            
            map.put(mapKey, memberValue);      
          }
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

            if (!memberName.contains(CassandraStore.UNION_COL_SUFIX)) {
            Field memberField = fieldSchema.getField(memberName);
            Schema memberSchema = memberField.schema();
            Type memberType = memberSchema.getType();
            
            CassandraSubColumn cassandraColumn = new CassandraSubColumn();
            cassandraColumn.setField(memberField);
            cassandraColumn.setValue(hColumn);
            
            if (memberType.equals(Type.UNION)){
              HColumn<ByteBuffer, ByteBuffer> hc = getUnionTypeColumn(memberField.name()
                  + CassandraStore.UNION_COL_SUFIX, this.hSuperColumn.getColumns().toArray());
              Integer unionIndex = getUnionIndex(hc);
              cassandraColumn.setUnionType(unionIndex);
            }
            
            record.put(record.getSchema().getField(memberName).pos(), cassandraColumn.getValue());
          }
          }
        }
        break;
      case UNION:
        int schemaPos = this.getUnionType();
        Schema unioSchema = fieldSchema.getTypes().get(schemaPos);
        Type unionType = unioSchema.getType();
        value = getSuperValue(field, unioSchema, unionType);
        break;
      default:
        Object memberValue = null;
        // Using for UnionIndex of Union type field get value. UnionIndex always Integer.  
        for (HColumn<ByteBuffer, ByteBuffer> hColumn : this.hSuperColumn.getColumns()) {
          memberValue = fromByteBuffer(fieldSchema, hColumn.getValue());      
        }
        value = memberValue;
        LOG.warn("Type: " + type.name() + " not supported for field: " + field.name());
    }
    return value;
  }

 private Integer getUnionIndex(HColumn<ByteBuffer, ByteBuffer> uc){
   Integer val = IntegerSerializer.get().fromByteBuffer(uc.getValue());
   return Integer.parseInt(val.toString());
 }
 
  private HColumn<ByteBuffer, ByteBuffer> getUnionTypeColumn(String fieldName,
    List<HColumn<ByteBuffer, ByteBuffer>> columns) {
    return getUnionTypeColumn(fieldName, columns.toArray());
}

  private HColumn<ByteBuffer, ByteBuffer> getUnionTypeColumn(String fieldName, Object[] hColumns) {
    for (Object currentHColumn : hColumns) {
      @SuppressWarnings("unchecked")
      HColumn<ByteBuffer, ByteBuffer> hColumn = (HColumn<ByteBuffer, ByteBuffer>) currentHColumn;
      String columnName = StringSerializer.get().fromByteBuffer(hColumn.getNameBytes().duplicate());
      if (fieldName.equals(columnName))
        return hColumn;
    }
    return null;
}

  public Object getValue() {
    Field field = getField();
    Schema fieldSchema = field.schema();
    Type type = fieldSchema.getType();
    
    Object value = getSuperValue(field, fieldSchema, type);
    
    return value;
  }

  public void setValue(HSuperColumn<String, ByteBuffer, ByteBuffer> hSuperColumn) {
    this.hSuperColumn = hSuperColumn;
  }

}
