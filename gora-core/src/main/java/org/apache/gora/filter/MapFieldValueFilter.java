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
package org.apache.gora.filter;

import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.ObjectWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A filter that checks for a single field in the persistent.
 * 
 * @param <K>
 * @param <T>
 */
public class MapFieldValueFilter<K, T extends PersistentBase> implements Filter<K, T> {

  protected String fieldName;
  protected Utf8 mapKey;
  protected FilterOp filterOp;
  protected List<Object> operands = new ArrayList<Object>();
  protected boolean filterIfMissing = false;

  private Configuration conf = new Configuration(); // just create empty conf,
                                                    // needed for ObjectWritable

  @Override
  public void write(DataOutput out) throws IOException {
    Text.writeString(out, fieldName);
    Text.writeString(out, mapKey.toString());
    WritableUtils.writeEnum(out, filterOp);
    WritableUtils.writeVInt(out, operands.size());
    for (int i = 0; i < operands.size(); i++) {
      Object operand = operands.get(i);
      if (operand instanceof String) {
        throw new IllegalStateException("Use Utf8 instead of String for operands");
      }
      if (operand instanceof Utf8) {
        operand = operand.toString();
      }
      if (operand instanceof Boolean) {
        ObjectWritable.writeObject(out, operand, Boolean.TYPE, conf);
      } else if (operand instanceof Character) {
        ObjectWritable.writeObject(out, operand, Character.TYPE, conf);
      } else if (operand instanceof Byte) {
        ObjectWritable.writeObject(out, operand, Byte.TYPE, conf);
      } else if (operand instanceof Short) {
        ObjectWritable.writeObject(out, operand, Short.TYPE, conf);
      } else if (operand instanceof Integer) {
        ObjectWritable.writeObject(out, operand, Integer.TYPE, conf);
      } else if (operand instanceof Long) {
        ObjectWritable.writeObject(out, operand, Long.TYPE, conf);
      } else if (operand instanceof Float) {
        ObjectWritable.writeObject(out, operand, Float.TYPE, conf);
      } else if (operand instanceof Double) {
        ObjectWritable.writeObject(out, operand, Double.TYPE, conf);
      } else if (operand instanceof Void) {
        ObjectWritable.writeObject(out, operand, Void.TYPE, conf);
      } else {
        ObjectWritable.writeObject(out, operand, operand.getClass(), conf);
      }
    }
    out.writeBoolean(filterIfMissing);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    fieldName = Text.readString(in);
    mapKey = new Utf8(Text.readString(in));
    filterOp = WritableUtils.readEnum(in, FilterOp.class);
    operands.clear();
    int operandsSize = WritableUtils.readVInt(in);
    for (int i = 0; i < operandsSize; i++) {
      Object operand = ObjectWritable.readObject(in, conf);
      if (operand instanceof String) {
        operand = new Utf8((String) operand);
      }
      operands.add(operand);
    }
    filterIfMissing = in.readBoolean();
  }

  @Override
  public boolean filter(K key, T persistent) {
    int fieldIndex = persistent.getFieldIndex(fieldName);
    Map<Utf8, ?> fieldValue = (Map<Utf8, ?>) persistent.get(fieldIndex);
    if (fieldValue == null) {
      return filterIfMissing;
    }
    Object value = fieldValue.get(mapKey);
    Object operand = operands.get(0);
    if (value == null) {
      return filterIfMissing;
    }
    if (filterOp.equals(FilterOp.EQUALS)) {
      boolean equals = value.equals(operand);
      return !equals;
    } else if (filterOp.equals(FilterOp.NOT_EQUALS)) {
      boolean equals = value.equals(operand);
      return equals;
    } else {
      throw new IllegalStateException(filterOp + " not yet implemented");
    }
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public Utf8 getMapKey() {
    return mapKey;
  }

  public void setMapKey(Utf8 mapKey) {
    this.mapKey = mapKey;
  }

  public FilterOp getFilterOp() {
    return filterOp;
  }

  public void setFilterOp(FilterOp filterOp) {
    this.filterOp = filterOp;
  }

  public List<Object> getOperands() {
    return operands;
  }

  public void setOperands(List<Object> operands) {
    this.operands = operands;
  }

  public void setFilterIfMissing(boolean filterIfMissing) {
    this.filterIfMissing = filterIfMissing;
  }

  public boolean isFilterIfMissing() {
    return filterIfMissing;
  }

  @Override
  public String toString() {
    return "SingleFieldValueFilter [fieldName=" + fieldName + ",mapKey=" + mapKey + ", filterOp=" + filterOp + ", operands=" + operands
        + ", filterIfMissing=" + filterIfMissing + "]";
  }
}
