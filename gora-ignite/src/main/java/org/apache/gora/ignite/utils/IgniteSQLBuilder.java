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
package org.apache.gora.ignite.utils;

import avro.shaded.com.google.common.collect.Lists;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import org.apache.gora.ignite.store.Column;
import org.apache.gora.ignite.store.IgniteMapping;

/**
 *
 * SQL Builder utility for Ignite.
 */
public class IgniteSQLBuilder {

  private static String format(String pattern, Object... args) {
    MessageFormat messageFormat = new MessageFormat(pattern, Locale.getDefault());
    return messageFormat.format(args);
  }

  public static String tableExists(String tableName) {
    return format("SELECT * FROM {0} LIMIT 0", tableName);
  }

  public static String createTable(IgniteMapping mapping) {
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("CREATE TABLE ");
    sqlBuilder.append(mapping.getTableName());
    sqlBuilder.append("(");
    ArrayList<Map.Entry<String, Column>> fieldsList = Lists.newArrayList(mapping.getFields().entrySet());
    for (Map.Entry<String, Column> aField : fieldsList) {
      Column aColumn = aField.getValue();
      String name = aColumn.getName();
      Column.FieldType dataType = aColumn.getDataType();
      sqlBuilder.append(name).append(" ").append(dataType.toString()).append(",");
    }
    sqlBuilder.append("PRIMARY KEY ");
    sqlBuilder.append("(");
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      sqlBuilder.append(mapping.getPrimaryKey().get(i));
      sqlBuilder.append(i == mapping.getPrimaryKey().size() - 1 ? "" : ",");
    }
    sqlBuilder.append(")");
    sqlBuilder.append(");");
    return sqlBuilder.toString();
  }
  
  public static String dropTable(String tableName) {
    return format("DROP TABLE IF EXISTS {0} ;", tableName);
  }

}
