/*
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
package org.apache.gora.kudu.utils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.apache.gora.kudu.mapping.Column;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Type;
import org.apache.kudu.client.KuduPredicate;
import org.apache.kudu.client.PartialRow;
import org.apache.kudu.client.RowResult;

public class KuduClientUtils {
  
  public static Object getObjectRow(RowResult row, Column column) {
    Type type = column.getDataType().getType();
    Object res;
    switch (type) {
      case INT8:
        res = row.getByte(column.getName());
        break;
      case INT16:
        res = row.getShort(column.getName());
        break;
      case INT32:
        res = row.getInt(column.getName());
        break;
      case INT64:
        res = row.getLong(column.getName());
        break;
      case BINARY:
        res = row.getBinary(column.getName());
        break;
      case STRING:
        res = row.getString(column.getName());
        break;
      case BOOL:
        res = row.getBoolean(column.getName());
        break;
      case FLOAT:
        res = row.getFloat(column.getName());
        break;
      case DOUBLE:
        res = row.getDouble(column.getName());
        break;
      case UNIXTIME_MICROS:
        res = row.getTimestamp(column.getName());
        break;
      case DECIMAL:
        res = row.getDecimal(column.getName());
        break;
      default:
        throw new AssertionError(type.name());
    }
    return res;
  }
  
  public static void addObjectRow(PartialRow row, Column column, Object key) {
    Type type = column.getDataType().getType();
    switch (type) {
      case INT8:
        row.addByte(column.getName(), (byte) key);
        break;
      case INT16:
        row.addShort(column.getName(), (short) key);
        break;
      case INT32:
        row.addInt(column.getName(), (int) key);
        break;
      case INT64:
        row.addLong(column.getName(), (long) key);
        break;
      case BINARY:
        row.addBinary(column.getName(), (byte[]) key);
        break;
      case STRING:
        row.addString(column.getName(), (String) key);
        break;
      case BOOL:
        row.addBoolean(column.getName(), (boolean) key);
        break;
      case FLOAT:
        row.addFloat(column.getName(), (float) key);
        break;
      case DOUBLE:
        row.addDouble(column.getName(), (double) key);
        break;
      case UNIXTIME_MICROS:
        if (key instanceof Timestamp) {
          row.addTimestamp(column.getName(), (Timestamp) key);
        } else if (key instanceof Long) {
          row.addTimestamp(column.getName(), new Timestamp((Long) key));
        }
        break;
      case DECIMAL:
        row.addDecimal(column.getName(), (BigDecimal) key);
        break;
      default:
        throw new AssertionError(type.name());
    }
  }
  
  public static KuduPredicate createEqualPredicate(ColumnSchema column, Object key) {
    KuduPredicate pred;
    switch (column.getType()) {
      case INT8:
      case INT16:
      case INT32:
      case INT64:
        pred = KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.EQUAL, (long) key);
        break;
      case BINARY:
        pred = KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.EQUAL, (byte[]) key);
        break;
      case STRING:
        pred = KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.EQUAL, (String) key);
        break;
      case BOOL:
        pred = KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.EQUAL, (boolean) key);
        break;
      case FLOAT:
        pred = KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.EQUAL, (float) key);
        break;
      case DOUBLE:
        pred = KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.EQUAL, (double) key);
        break;
      case UNIXTIME_MICROS:
        pred = KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.EQUAL, (Timestamp) key);
        break;
      case DECIMAL:
        pred = KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.EQUAL, (BigDecimal) key);
        break;
      default:
        throw new AssertionError(column.getType().name());
    }
    return pred;
  }
}
