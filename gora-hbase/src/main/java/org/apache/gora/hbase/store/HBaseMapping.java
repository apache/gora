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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder.ModifyableColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.io.compress.Compression.Algorithm;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * Mapping definitions for HBase. Thread safe.
 * It holds a definition for a single table. 
 */
public class HBaseMapping {

  private final TableDescriptor tableDescriptor;
  
  // a map from field name to hbase column
  private final Map<String, HBaseColumn> columnMap;
  
  public HBaseMapping(TableDescriptor tableDescriptor,
      Map<String, HBaseColumn> columnMap) {
    super();
    this.tableDescriptor = tableDescriptor;
    this.columnMap = columnMap;
  }
  

  public String getTableName() {
    return tableDescriptor.getTableName().getNameAsString();
  }
  
  public TableDescriptor getTable() {
    return tableDescriptor;
  }
  
  public HBaseColumn getColumn(String fieldName) {
    return columnMap.get(fieldName);
  }
  
  /**
   * A builder for creating the mapper. This will allow building a thread safe
   * {@link HBaseMapping} using simple immutability.
   *
   */
  public static class HBaseMappingBuilder { 
    private Map<String, Map<String, ColumnFamilyDescriptor>> tableToFamilies =
      new HashMap<>();
    private Map<String, HBaseColumn> columnMap = 
      new HashMap<>();
    
    private TableName tableName;
    
    public String getTableName() {
      return tableName.getNameAsString();
    }
    
    public void setTableName(String tableName) {
      this.tableName = TableName.valueOf(tableName);
    }
    
    public void addFamilyProps(String tableName, String familyName,
        String compression, String blockCache, String blockSize,
        String bloomFilter ,String maxVersions, String timeToLive, 
        String inMemory) {
      
      // We keep track of all tables, because even though we
      // only build a mapping for one table. We do this because of the way
      // the mapping file is set up. 
      // (First family properties are defined, whereafter columns are defined).
      //
      // HBaseMapping in fact does not need to support multiple tables,
      // because a Store itself only supports a single table. (Every store 
      // instance simply creates one mapping instance for itself).
      //
      // TODO A nice solution would be to redefine the mapping file structure.
      // For example nest columns in families. Of course this would break compatibility.
      
      
      Map<String, ColumnFamilyDescriptor> families = getOrCreateFamilies(tableName);

      ColumnFamilyDescriptor columnDescriptor = getOrCreateFamily(familyName, families);
      ModifyableColumnFamilyDescriptor modifyableColFamilyDescriptor =
              (ModifyableColumnFamilyDescriptor) columnDescriptor;
      
      if(compression != null)
        modifyableColFamilyDescriptor.setCompressionType(Algorithm.valueOf(compression));
      if(blockCache != null)
        modifyableColFamilyDescriptor.setBlockCacheEnabled(Boolean.parseBoolean(blockCache));
      if(blockSize != null)
        modifyableColFamilyDescriptor.setBlocksize(Integer.parseInt(blockSize));
      if(bloomFilter != null)
        modifyableColFamilyDescriptor.setBloomFilterType(BloomType.valueOf(bloomFilter));
      if(maxVersions != null)
        modifyableColFamilyDescriptor.setMaxVersions(Integer.parseInt(maxVersions));
      if(timeToLive != null)
        modifyableColFamilyDescriptor.setTimeToLive(Integer.parseInt(timeToLive));
      if(inMemory != null)
        modifyableColFamilyDescriptor.setInMemory(Boolean.parseBoolean(inMemory));
    }

    public void addColumnFamily(String tableName, String familyName) {
      Map<String, ColumnFamilyDescriptor> families = getOrCreateFamilies(tableName);
      getOrCreateFamily(familyName, families);
    }
    
    public void addField(String fieldName, String family, String qualifier) {
      byte[] familyBytes = Bytes.toBytes(family);
      byte[] qualifierBytes = qualifier == null ? null : 
        Bytes.toBytes(qualifier);
      
      HBaseColumn column = new HBaseColumn(familyBytes, qualifierBytes);
      columnMap.put(fieldName, column);
    }


    private ColumnFamilyDescriptor getOrCreateFamily(String familyName,
                                                     Map<String, ColumnFamilyDescriptor> families) {
      ColumnFamilyDescriptor columnDescriptor = families.get(familyName);
      if (columnDescriptor == null) {
        ColumnFamilyDescriptorBuilder columnDescBuilder = ColumnFamilyDescriptorBuilder
                .newBuilder(Bytes.toBytes(familyName));
        columnDescriptor = columnDescBuilder.build();
        families.put(familyName, columnDescriptor);
      }
      return columnDescriptor;
    }

    private Map<String, ColumnFamilyDescriptor> getOrCreateFamilies(String tableName) {
      Map<String, ColumnFamilyDescriptor> families;
      families = tableToFamilies.get(tableName);
      if (families == null) {
        families = new HashMap<>();
        tableToFamilies.put(tableName, families);
      }
      return families;
    }
    
    public void renameTable(String oldName, String newName) {
      Map<String, ColumnFamilyDescriptor> families = tableToFamilies.remove(oldName);
      if (families == null) throw new IllegalArgumentException(oldName + " does not exist");
      tableToFamilies.put(newName, families);
    }
    
    /**
     * @return A newly constructed mapping.
     */
    public HBaseMapping build() {
      if (tableName == null) throw new IllegalStateException("tableName is not specified");

      Map<String, ColumnFamilyDescriptor> families = tableToFamilies.get(tableName.getNameAsString());
      if (families == null) throw new IllegalStateException("no families for table " + tableName);

      TableDescriptorBuilder tableDescBuilder = TableDescriptorBuilder.newBuilder(tableName);

      for (ColumnFamilyDescriptor familyDescriptor : families.values()) {
        tableDescBuilder.setColumnFamily(familyDescriptor);
      }
      TableDescriptor tableDescriptor = tableDescBuilder.build();
      return new HBaseMapping(tableDescriptor, columnMap);
    }
  }

  public String toString() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append(this.getTableName());
    strBuilder.append("\n");
    for (Entry<String, HBaseColumn> mappingEntry : this.columnMap.entrySet()) {
      byte[] familyBytes = mappingEntry.getValue().getFamily() == null ? new byte[0] : mappingEntry.getValue().getFamily() ;
      byte[] qualifierBytes = mappingEntry.getValue().getQualifier() == null ? new byte[0] : mappingEntry.getValue().getQualifier() ;
      strBuilder.append(mappingEntry.getKey() + " -> " + new String(familyBytes, StandardCharsets.UTF_8) + ":" + new String(qualifierBytes, StandardCharsets.UTF_8)) ;
      strBuilder.append("\n");
    }
    return strBuilder.toString() ;
  }
  
}