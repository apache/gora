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
import java.util.ArrayList;
import java.util.List;
import org.apache.gora.kudu.mapping.Column;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Type;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.KuduPredicate;
import org.apache.kudu.client.KuduScanner;
import org.apache.kudu.client.PartialRow;
import org.apache.kudu.client.RowResult;
import org.apache.kudu.client.RowResultIterator;

public class KuduClientUtils {

  private KuduClientUtils() {
  }

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
        res = row.getBinaryCopy(column.getName());
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

  public static List<KuduPredicate> createRangePredicate(ColumnSchema column, Object startK, Object endK) {
    List<KuduPredicate> predList = new ArrayList<>();
    switch (column.getType()) {
      case INT8:
      case INT16:
      case INT32:
      case INT64:
        if (startK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.GREATER_EQUAL, (long) startK));
        }
        if (endK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.LESS_EQUAL, (long) endK));
        }
        break;
      case BINARY:
        if (startK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.GREATER_EQUAL, (byte[]) startK));
        }
        if (endK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.LESS_EQUAL, (byte[]) endK));
        }
        break;
      case STRING:
        if (startK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.GREATER_EQUAL, (String) startK));
        }
        if (endK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.LESS_EQUAL, (String) endK));
        }
        break;
      case BOOL:
        if (startK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.GREATER_EQUAL, (boolean) startK));
        }
        if (endK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.LESS_EQUAL, (boolean) endK));
        }
        break;
      case FLOAT:
        if (startK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.GREATER_EQUAL, (float) startK));
        }
        if (endK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.LESS_EQUAL, (float) endK));
        }
        break;
      case DOUBLE:
        if (startK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.GREATER_EQUAL, (double) startK));
        }
        if (endK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.LESS_EQUAL, (double) endK));
        }
        break;
      case UNIXTIME_MICROS:
        if (startK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.GREATER_EQUAL, (Timestamp) startK));
        }
        if (endK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.LESS_EQUAL, (Timestamp) endK));
        }
        break;
      case DECIMAL:
        if (startK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.GREATER_EQUAL, (BigDecimal) startK));
        }
        if (endK != null) {
          predList.add(KuduPredicate.newComparisonPredicate(column, KuduPredicate.ComparisonOp.LESS_EQUAL, (BigDecimal) endK));
        }
        break;
      default:
        throw new AssertionError(column.getType().name());
    }
    return predList;
  }

  public static RowResult waitFirstResult(KuduScanner scanner) throws KuduException {
    RowResult result = null;
    while (scanner.hasMoreRows()) {
      RowResultIterator nextRows = scanner.nextRows();
      if (nextRows.hasNext()) {
        result = nextRows.next();
        break;
      }
    }
    return result;
  }
}
