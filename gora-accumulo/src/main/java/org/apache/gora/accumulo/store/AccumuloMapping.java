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
package org.apache.gora.accumulo.store;

import java.util.HashMap;
import java.util.Map;

import org.apache.accumulo.core.util.Pair;
import org.apache.hadoop.io.Text;

/**
 * Mapping definitions for Accumulo.
 */
public class AccumuloMapping {

  /**
   * A map of field names to Field objects containing schema's fields
   */
  Map<String,Pair<Text,Text>> fieldMap = new HashMap<>();

  /**
   * Look up the column associated to the Avro field.
   */
  Map<Pair<Text,Text>,String> columnMap = new HashMap<>();

  Map<String,String> tableConfig = new HashMap<>();
  String tableName;
  String encoder;

}
