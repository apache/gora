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

package org.apache.gora.hbase.store;

import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.io.hfile.Compression.Algorithm;
import org.apache.hadoop.hbase.regionserver.StoreFile.BloomType;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * Mapping definitions for HBase. Thread safe.
 */
public class HBaseMapping {

  private final Map<String, HTableDescriptor> tableDescriptors;
  
  //name of the primary table
  private final String tableName; 
  
  // a map from field name to hbase column
  private final Map<String, HBaseColumn> columnMap;
  
  public HBaseMapping(Map<String, HTableDescriptor> tableDescriptors,
      String tableName, Map<String, HBaseColumn> columnMap) {
    super();
    this.tableDescriptors = tableDescriptors;
    this.tableName = tableName;
    this.columnMap = columnMap;
  }

  public String getTableName() {
    return tableName;
  }
  
  public HTableDescriptor getTable() {
    return getTable(tableName);
  }
  
  public HTableDescriptor getTable(String tableName) {
    return tableDescriptors.get(tableName);
  }
  
  public HBaseColumn getColumn(String fieldName) {
    return columnMap.get(fieldName);
  }
  
  /**
   * A builder for creating the mapper. This will allow building a thread safe
   * {@link HBaseMapping} using simple immutabilty.
   *
   */
  public static class HBaseMappingBuilder {
    private Map<String, HTableDescriptor> tableDescriptors 
      = new HashMap<String, HTableDescriptor>();
    private String tableName; 
    private Map<String, HBaseColumn> columnMap = 
      new HashMap<String, HBaseColumn>();
    
    public void addTable(String tableName) {
      if(!tableDescriptors.containsKey(tableName)) {
        tableDescriptors.put(tableName, new HTableDescriptor(tableName));
      }
    }
    
    public String getTableName() {
      return tableName;
    }
    
    public void setTableName(String tableName) {
      this.tableName = tableName;
    }
    
    public void addColumnFamily(String tableName, String familyName,
        String compression, String blockCache, String blockSize,
        String bloomFilter ,String maxVersions, String timeToLive, 
        String inMemory) {
      
      HColumnDescriptor columnDescriptor = addColumnFamily(tableName, 
          familyName);
      
      if(compression != null)
        columnDescriptor.setCompressionType(Algorithm.valueOf(compression));
      if(blockCache != null)
        columnDescriptor.setBlockCacheEnabled(Boolean.parseBoolean(blockCache));
      if(blockSize != null)
        columnDescriptor.setBlocksize(Integer.parseInt(blockSize));
      if(bloomFilter != null)
        columnDescriptor.setBloomFilterType(BloomType.valueOf(bloomFilter));
      if(maxVersions != null)
        columnDescriptor.setMaxVersions(Integer.parseInt(maxVersions));
      if(timeToLive != null)
        columnDescriptor.setTimeToLive(Integer.parseInt(timeToLive));
      if(inMemory != null)
        columnDescriptor.setInMemory(Boolean.parseBoolean(inMemory));
      
      getTable(tableName).addFamily(columnDescriptor);
    }
    
    public HTableDescriptor getTable(String tableName) {
      return tableDescriptors.get(tableName);
    }
    
    public HColumnDescriptor addColumnFamily(String tableName, 
        String familyName) {
      HTableDescriptor tableDescriptor = getTable(tableName);
      HColumnDescriptor columnDescriptor =  tableDescriptor.getFamily(
          Bytes.toBytes(familyName));
      if(columnDescriptor == null) {
        columnDescriptor = new HColumnDescriptor(familyName);
        tableDescriptor.addFamily(columnDescriptor);
      }
      return columnDescriptor;
    }
    
    public void addField(String fieldName, String tableName, String family, 
        String qualifier) {
      byte[] familyBytes = Bytes.toBytes(family);
      byte[] qualifierBytes = qualifier == null ? null : 
        Bytes.toBytes(qualifier);
      
      HBaseColumn column = new HBaseColumn(tableName, familyBytes, 
          qualifierBytes);
      columnMap.put(fieldName, column);
    }
    
    /**
     * @return A newly constructed mapping.
     */
    public HBaseMapping build() {
      return new HBaseMapping(tableDescriptors, tableName, columnMap);
    }
  }
}