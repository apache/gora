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
 * It holds a definition for a single table. 
 */
public class HBaseMapping {

  private final HTableDescriptor tableDescriptor;
  
  // a map from field name to hbase column
  private final Map<String, HBaseColumn> columnMap;
  
  public HBaseMapping(HTableDescriptor tableDescriptor,
      Map<String, HBaseColumn> columnMap) {
    super();
    this.tableDescriptor = tableDescriptor;
    this.columnMap = columnMap;
  }
  

  public String getTableName() {
    return tableDescriptor.getNameAsString();
  }
  
  public HTableDescriptor getTable() {
    return tableDescriptor;
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
    private Map<String, Map<String, HColumnDescriptor>> tableToFamilies = 
      new HashMap<String, Map<String, HColumnDescriptor>>();
    private Map<String, HBaseColumn> columnMap = 
      new HashMap<String, HBaseColumn>();
    
    private String tableName;
    
    public String getTableName() {
      return tableName;
    }
    
    public void setTableName(String tableName) {
      this.tableName = tableName;
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
      
      
      Map<String, HColumnDescriptor> families = getOrCreateFamilies(tableName);;
      
      
      HColumnDescriptor columnDescriptor = getOrCreateFamily(familyName, families);
      
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
    }

    public void addColumnFamily(String tableName, String familyName) {
      Map<String, HColumnDescriptor> families = getOrCreateFamilies(tableName);
      getOrCreateFamily(familyName, families);
    }
    
    public void addField(String fieldName, String family, String qualifier) {
      byte[] familyBytes = Bytes.toBytes(family);
      byte[] qualifierBytes = qualifier == null ? null : 
        Bytes.toBytes(qualifier);
      
      HBaseColumn column = new HBaseColumn(familyBytes, qualifierBytes);
      columnMap.put(fieldName, column);
    }
    

    private HColumnDescriptor getOrCreateFamily(String familyName,
        Map<String, HColumnDescriptor> families) {
      HColumnDescriptor columnDescriptor = families.get(familyName);
      if (columnDescriptor == null) {
        columnDescriptor=new HColumnDescriptor(familyName);
        families.put(familyName, columnDescriptor);
      }
      return columnDescriptor;
    }

    private Map<String, HColumnDescriptor> getOrCreateFamilies(String tableName) {
      Map<String, HColumnDescriptor> families;
      families = tableToFamilies.get(tableName);
      if (families == null) {
        families = new HashMap<String, HColumnDescriptor>();
        tableToFamilies.put(tableName, families);
      }
      return families;
    }
    
    public void renameTable(String oldName, String newName) {
      Map<String, HColumnDescriptor> families = tableToFamilies.remove(oldName);
      if (families == null) throw new IllegalArgumentException(oldName + " does not exist");
      tableToFamilies.put(newName, families);
    }
    
    /**
     * @return A newly constructed mapping.
     */
    public HBaseMapping build() {
      if (tableName == null) throw new IllegalStateException("tableName is not specified");
      
      Map<String, HColumnDescriptor> families = tableToFamilies.get(tableName);
      if (families == null) throw new IllegalStateException("no families for table " + tableName);
      
      HTableDescriptor tableDescriptors = new HTableDescriptor(tableName);
      for (HColumnDescriptor desc : families.values()) {
        tableDescriptors.addFamily(desc);
      }
      return new HBaseMapping(tableDescriptors, columnMap);
    }
  }

}