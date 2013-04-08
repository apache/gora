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

import static org.apache.gora.hbase.util.HBaseByteInterface.fromBytes;
import static org.apache.gora.hbase.util.HBaseByteInterface.toBytes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.gora.hbase.query.HBaseGetResult;
import org.apache.gora.hbase.query.HBaseQuery;
import org.apache.gora.hbase.query.HBaseScannerResult;
import org.apache.gora.hbase.store.HBaseMapping.HBaseMappingBuilder;
import org.apache.gora.hbase.util.HBaseByteInterface;
import org.apache.gora.persistency.ListGenericArray;
import org.apache.gora.persistency.State;
import org.apache.gora.persistency.StateManager;
import org.apache.gora.persistency.StatefulHashMap;
import org.apache.gora.persistency.StatefulMap;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * DataStore for HBase. Thread safe.
 *
 */
public class HBaseStore<K, T extends PersistentBase> extends DataStoreBase<K, T>
implements Configurable {

  public static final Logger LOG = LoggerFactory.getLogger(HBaseStore.class);

  public static final String PARSE_MAPPING_FILE_KEY = "gora.hbase.mapping.file";

  @Deprecated
  private static final String DEPRECATED_MAPPING_FILE = "hbase-mapping.xml";
  public static final String DEFAULT_MAPPING_FILE = "gora-hbase-mapping.xml";

  private volatile HBaseAdmin admin;

  private volatile HBaseTableConnection table;

  private final boolean autoCreateSchema = true;

  private volatile HBaseMapping mapping;

  public HBaseStore()  {
  }

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) {
    try {
      
      super.initialize(keyClass, persistentClass, properties);
      this.conf = HBaseConfiguration.create(getConf());
      admin = new HBaseAdmin(this.conf);
      mapping = readMapping(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
      
    } catch (FileNotFoundException ex) {
      try {
        mapping = readMapping(getConf().get(PARSE_MAPPING_FILE_KEY, DEPRECATED_MAPPING_FILE));
        LOG.warn(DEPRECATED_MAPPING_FILE + " is deprecated, please rename the file to "
            + DEFAULT_MAPPING_FILE);
      } catch (FileNotFoundException ex1) {
          LOG.error(ex1.getMessage());
          LOG.error(ex1.getStackTrace().toString());
          //throw (ex1); //throw the original exception
      } catch (Exception ex1) {
        LOG.warn(DEPRECATED_MAPPING_FILE + " is deprecated, please rename the file to "
            + DEFAULT_MAPPING_FILE);
        throw new RuntimeException(ex1);
      } 
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    if(autoCreateSchema) {
      createSchema();
    }
    try{
      boolean autoflush = this.conf.getBoolean("hbase.client.autoflush.default", false);
      table = new HBaseTableConnection(getConf(), getSchemaName(), autoflush);
    } catch(IOException ex2){
      LOG.error(ex2.getMessage());
      LOG.error(ex2.getStackTrace().toString());
    }
  }

  @Override
  public String getSchemaName() {
    //return the name of this table
    return mapping.getTableName();
  }

  @Override
  public void createSchema() {
    try{
      if(schemaExists()) {
        return;
      }
      HTableDescriptor tableDesc = mapping.getTable();
  
      admin.createTable(tableDesc);
    } catch(IOException ex2){
      LOG.error(ex2.getMessage());
      LOG.error(ex2.getStackTrace().toString());
    }
  }

  @Override
  public void deleteSchema() {
    try{
      if(!schemaExists()) {
        return;
      }
      admin.disableTable(getSchemaName());
      admin.deleteTable(getSchemaName());
    } catch(IOException ex2){
      LOG.error(ex2.getMessage());
      LOG.error(ex2.getStackTrace().toString());
    }
  }

  @Override
  public boolean schemaExists() {
    try{
      return admin.tableExists(mapping.getTableName());
    } catch(IOException ex2){
      LOG.error(ex2.getMessage());
      LOG.error(ex2.getStackTrace().toString());
      return false;
    }
  }

  @Override
  public T get(K key, String[] fields) {
    try{
      fields = getFieldsToQuery(fields);
      Get get = new Get(toBytes(key));
      addFields(get, fields);
      Result result = table.get(get);
      return newInstance(result, fields);      
    } catch(IOException ex2){
      LOG.error(ex2.getMessage());
      LOG.error(ex2.getStackTrace().toString());
      return null;
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void put(K key, T persistent) {
    try{
      Schema schema = persistent.getSchema();
      StateManager stateManager = persistent.getStateManager();
      byte[] keyRaw = toBytes(key);
      Put put = new Put(keyRaw);
      Delete delete = new Delete(keyRaw);
      boolean hasPuts = false;
      boolean hasDeletes = false;
      Iterator<Field> iter = schema.getFields().iterator();
      for (int i = 0; iter.hasNext(); i++) {
        Field field = iter.next();
        if (!stateManager.isDirty(persistent, i)) {
          continue;
        }
        Type type = field.schema().getType();
        Object o = persistent.get(i);
        HBaseColumn hcol = mapping.getColumn(field.name());
        if (hcol == null) {
          throw new RuntimeException("HBase mapping for field ["+ persistent.getClass().getName() +
              "#"+ field.name()+"] not found. Wrong gora-hbase-mapping.xml?");
        }
        switch(type) {
          case MAP:
            if(o instanceof StatefulMap) {
              StatefulHashMap<Utf8, ?> map = (StatefulHashMap<Utf8, ?>) o;
              for (Entry<Utf8, State> e : map.states().entrySet()) {
                Utf8 mapKey = e.getKey();
                switch (e.getValue()) {
                  case DIRTY:
                    byte[] qual = Bytes.toBytes(mapKey.toString());
                    byte[] val = toBytes(map.get(mapKey), field.schema().getValueType());
                    put.add(hcol.getFamily(), qual, val);
                    hasPuts = true;
                    break;
                  case DELETED:
                    qual = Bytes.toBytes(mapKey.toString());
                    hasDeletes = true;
                    delete.deleteColumn(hcol.getFamily(), qual);
                    break;
                }
              }
            } else {
              Set<Map.Entry> set = ((Map)o).entrySet();
              for(Entry entry: set) {
                byte[] qual = toBytes(entry.getKey());
                byte[] val = toBytes(entry.getValue());
                put.add(hcol.getFamily(), qual, val);
                hasPuts = true;
              }
            }
            break;
          case ARRAY:
            if(o instanceof GenericArray) {
              GenericArray arr = (GenericArray) o;
              int j=0;
              for(Object item : arr) {
                byte[] val = toBytes(item);
                put.add(hcol.getFamily(), Bytes.toBytes(j++), val);
                hasPuts = true;
              }
            }
            break;
          default:
            put.add(hcol.getFamily(), hcol.getQualifier(), toBytes(o, field.schema()));
            hasPuts = true;
            break;
        }
      }
      if (hasPuts) {
        table.put(put);
      }
      if (hasDeletes) {
        table.delete(delete);
      }
    } catch(IOException ex2){
      LOG.error(ex2.getMessage());
      LOG.error(ex2.getStackTrace().toString());
    }
  }

  public void delete(T obj) {
    throw new RuntimeException("Not implemented yet");
  }

  /**
   * Deletes the object with the given key.
   * @return always true
   */
  @Override
  public boolean delete(K key) {
    try{
      table.delete(new Delete(toBytes(key)));
      //HBase does not return success information and executing a get for
      //success is a bit costly
      return true;
    } catch(IOException ex2){
      LOG.error(ex2.getMessage());
      LOG.error(ex2.getStackTrace().toString());
      return false;
    }
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    try {
      String[] fields = getFieldsToQuery(query.getFields());
      //find whether all fields are queried, which means that complete
      //rows will be deleted
      boolean isAllFields = Arrays.equals(fields
          , getBeanFactory().getCachedPersistent().getFields());
  
      org.apache.gora.query.Result<K, T> result = null;
      result = query.execute();
      ArrayList<Delete> deletes = new ArrayList<Delete>();
      while(result.next()) {
        Delete delete = new Delete(toBytes(result.getKey()));
        deletes.add(delete);
        if(!isAllFields) {
          addFields(delete, query);
        }
      }
      table.delete(deletes);
      return deletes.size();
    } catch (Exception ex) {
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
      return -1;
    }
  }

  @Override
  public void flush() {
    try{
      table.flushCommits();
    }catch(IOException ex){
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
    }
  }

  @Override
  public Query<K, T> newQuery() {
    return new HBaseQuery<K, T>(this);
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query)
      throws IOException {

    // taken from o.a.h.hbase.mapreduce.TableInputFormatBase
    Pair<byte[][], byte[][]> keys = table.getStartEndKeys();
    if (keys == null || keys.getFirst() == null ||
        keys.getFirst().length == 0) {
      throw new IOException("Expecting at least one region.");
    }
    if (table == null) {
      throw new IOException("No table was provided.");
    }
    List<PartitionQuery<K,T>> partitions = new ArrayList<PartitionQuery<K,T>>(keys.getFirst().length);
    for (int i = 0; i < keys.getFirst().length; i++) {
      String regionLocation = table.getRegionLocation(keys.getFirst()[i]).
      getServerAddress().getHostname();
      byte[] startRow = query.getStartKey() != null ? toBytes(query.getStartKey())
          : HConstants.EMPTY_START_ROW;
      byte[] stopRow = query.getEndKey() != null ? toBytes(query.getEndKey())
          : HConstants.EMPTY_END_ROW;

      // determine if the given start an stop key fall into the region
      if ((startRow.length == 0 || keys.getSecond()[i].length == 0 ||
          Bytes.compareTo(startRow, keys.getSecond()[i]) < 0) &&
          (stopRow.length == 0 ||
              Bytes.compareTo(stopRow, keys.getFirst()[i]) > 0)) {

        byte[] splitStart = startRow.length == 0 || 
            Bytes.compareTo(keys.getFirst()[i], startRow) >= 0 ? 
            keys.getFirst()[i] : startRow;

        byte[] splitStop = (stopRow.length == 0 || 
            Bytes.compareTo(keys.getSecond()[i], stopRow) <= 0) && 
            keys.getSecond()[i].length > 0 ? keys.getSecond()[i] : stopRow;

        K startKey = Arrays.equals(HConstants.EMPTY_START_ROW, splitStart) ?
            null : HBaseByteInterface.fromBytes(keyClass, splitStart);
        K endKey = Arrays.equals(HConstants.EMPTY_END_ROW, splitStop) ?
            null : HBaseByteInterface.fromBytes(keyClass, splitStop);

        PartitionQuery<K, T> partition = new PartitionQueryImpl<K, T>(
            query, startKey, endKey, regionLocation);

        partitions.add(partition);
      }
    }
    return partitions;
  }

  @Override
  public org.apache.gora.query.Result<K, T> execute(Query<K, T> query){
    try{
      //check if query.fields is null
      query.setFields(getFieldsToQuery(query.getFields()));
  
      if(query.getStartKey() != null && query.getStartKey().equals(
          query.getEndKey())) {
        Get get = new Get(toBytes(query.getStartKey()));
        addFields(get, query.getFields());
        addTimeRange(get, query);
        Result result = table.get(get);
        return new HBaseGetResult<K,T>(this, query, result);
      } else {
        ResultScanner scanner = createScanner(query);
  
        org.apache.gora.query.Result<K,T> result
            = new HBaseScannerResult<K,T>(this,query, scanner);
  
        return result;
      }
    }catch(IOException ex){
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
      return null;
    }
  }

  public ResultScanner createScanner(Query<K, T> query) throws IOException {
    final Scan scan = new Scan();
    if (query.getStartKey() != null) {
      scan.setStartRow(toBytes(query.getStartKey()));
    }
    if (query.getEndKey() != null) {
      scan.setStopRow(toBytes(query.getEndKey()));
    }
    addFields(scan, query);

    return table.getScanner(scan);
  }

  private void addFields(Get get, String[] fieldNames) {
    for (String f : fieldNames) {
      HBaseColumn col = mapping.getColumn(f);
      if (col == null) {
        throw new  RuntimeException("HBase mapping for field ["+ f +"] not found. " +
            "Wrong gora-hbase-mapping.xml?");
      }
      Schema fieldSchema = fieldMap.get(f).schema();
      switch (fieldSchema.getType()) {
        case MAP:
        case ARRAY:
          get.addFamily(col.family); break;
        default:
          get.addColumn(col.family, col.qualifier); break;
      }
    }
  }

  private void addFields(Scan scan, Query<K,T> query)
  throws IOException {
    String[] fields = query.getFields();
    for (String f : fields) {
      HBaseColumn col = mapping.getColumn(f);
      if (col == null) {
        throw new  RuntimeException("HBase mapping for field ["+ f +"] not found. " +
            "Wrong gora-hbase-mapping.xml?");
      }
      Schema fieldSchema = fieldMap.get(f).schema();
      switch (fieldSchema.getType()) {
        case MAP:
        case ARRAY:
          scan.addFamily(col.family); break;
        default:
          scan.addColumn(col.family, col.qualifier); break;
      }
    }
  }

  //TODO: HBase Get, Scan, Delete should extend some common interface with addFamily, etc
  private void addFields(Delete delete, Query<K,T> query)
    throws IOException {
    String[] fields = query.getFields();
    for (String f : fields) {
      HBaseColumn col = mapping.getColumn(f);
      if (col == null) {
        throw new  RuntimeException("HBase mapping for field ["+ f +"] not found. " +
            "Wrong gora-hbase-mapping.xml?");
      }
      Schema fieldSchema = fieldMap.get(f).schema();
      switch (fieldSchema.getType()) {
        case MAP:
        case ARRAY:
          delete.deleteFamily(col.family); break;
        default:
          delete.deleteColumn(col.family, col.qualifier); break;
      }
    }
  }

  private void addTimeRange(Get get, Query<K, T> query) throws IOException {
    if(query.getStartTime() > 0 || query.getEndTime() > 0) {
      if(query.getStartTime() == query.getEndTime()) {
        get.setTimeStamp(query.getStartTime());
      } else {
        long startTime = query.getStartTime() > 0 ? query.getStartTime() : 0;
        long endTime = query.getEndTime() > 0 ? query.getEndTime() : Long.MAX_VALUE;
        get.setTimeRange(startTime, endTime);
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  /**
   * Creates a new Persistent instance with the values in 'result' for the fields listed.
   * @param result result form a HTable#get()
   * @param fields List of fields queried, or null for all
   * @return A new instance with default values for not listed fields
   *         null if 'result' is null.
   * @throws IOException
   */
  public T newInstance(Result result, String[] fields)
  throws IOException {
    if(result == null || result.isEmpty())
      return null;

    T persistent = newPersistent();
    StateManager stateManager = persistent.getStateManager();
    for (String f : fields) {
      HBaseColumn col = mapping.getColumn(f);
      if (col == null) {
        throw new  RuntimeException("HBase mapping for field ["+ f +"] not found. " +
            "Wrong gora-hbase-mapping.xml?");
      }
      Field field = fieldMap.get(f);
      Schema fieldSchema = field.schema();
      switch(fieldSchema.getType()) {
        case MAP:
          NavigableMap<byte[], byte[]> qualMap =
            result.getNoVersionMap().get(col.getFamily());
          if (qualMap == null) {
            continue;
          }
          Schema valueSchema = fieldSchema.getValueType();
          Map map = new HashMap();
          for (Entry<byte[], byte[]> e : qualMap.entrySet()) {
            map.put(new Utf8(Bytes.toString(e.getKey())),
                fromBytes(valueSchema, e.getValue()));
          }
          setField(persistent, field, map);
          break;
        case ARRAY:
          qualMap = result.getFamilyMap(col.getFamily());
          if (qualMap == null) {
            continue;
          }
          valueSchema = fieldSchema.getElementType();
          ArrayList arrayList = new ArrayList();
          for (Entry<byte[], byte[]> e : qualMap.entrySet()) {
            arrayList.add(fromBytes(valueSchema, e.getValue()));
          }
          ListGenericArray arr = new ListGenericArray(fieldSchema, arrayList);
          setField(persistent, field, arr);
          break;
        default:
          byte[] val =
            result.getValue(col.getFamily(), col.getQualifier());
          if (val == null) {
            continue;
          }
          setField(persistent, field, val);
          break;
      }
    }
    stateManager.clearDirty(persistent);
    return persistent;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void setField(T persistent, Field field, Map map) {
    persistent.put(field.pos(), new StatefulHashMap(map));
  }

  private void setField(T persistent, Field field, byte[] val)
  throws IOException {
    persistent.put(field.pos(), fromBytes(field.schema(), val));
  }

  @SuppressWarnings("rawtypes")
  private void setField(T persistent, Field field, GenericArray list) {
    persistent.put(field.pos(), list);
  }

  @SuppressWarnings("unchecked")
  private HBaseMapping readMapping(String filename) throws IOException {

    HBaseMappingBuilder mappingBuilder = new HBaseMappingBuilder();

    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(getClass().getClassLoader()
          .getResourceAsStream(filename));
      Element root = doc.getRootElement();

      List<Element> tableElements = root.getChildren("table");
      for(Element tableElement : tableElements) {
        String tableName = tableElement.getAttributeValue("name");

        List<Element> fieldElements = tableElement.getChildren("family");
        for(Element fieldElement : fieldElements) {
          String familyName  = fieldElement.getAttributeValue("name");
          String compression = fieldElement.getAttributeValue("compression");
          String blockCache  = fieldElement.getAttributeValue("blockCache");
          String blockSize   = fieldElement.getAttributeValue("blockSize");
          String bloomFilter = fieldElement.getAttributeValue("bloomFilter");
          String maxVersions = fieldElement.getAttributeValue("maxVersions");
          String timeToLive  = fieldElement.getAttributeValue("timeToLive");
          String inMemory    = fieldElement.getAttributeValue("inMemory");
          
          mappingBuilder.addFamilyProps(tableName, familyName, compression, 
              blockCache, blockSize, bloomFilter, maxVersions, timeToLive, 
              inMemory);
        }
      }

      List<Element> classElements = root.getChildren("class");
      for(Element classElement: classElements) {
        if(classElement.getAttributeValue("keyClass").equals(
            keyClass.getCanonicalName())
            && classElement.getAttributeValue("name").equals(
                persistentClass.getCanonicalName())) {

          String tableNameFromMapping = classElement.getAttributeValue("table");
          String tableName = getSchemaName(tableNameFromMapping, persistentClass);
          
          //tableNameFromMapping could be null here
          if (!tableName.equals(tableNameFromMapping)) {
            LOG.info("Keyclass and nameclass match but mismatching table names " 
                + " mappingfile schema is '" + tableNameFromMapping 
                + "' vs actual schema '" + tableName + "' , assuming they are the same.");
            if (tableNameFromMapping != null) {
              mappingBuilder.renameTable(tableNameFromMapping, tableName);
            }
          }
          mappingBuilder.setTableName(tableName);

          List<Element> fields = classElement.getChildren("field");
          for(Element field:fields) {
            String fieldName =  field.getAttributeValue("name");
            String family =  field.getAttributeValue("family");
            String qualifier = field.getAttributeValue("qualifier");
            mappingBuilder.addField(fieldName, family, qualifier);
            mappingBuilder.addColumnFamily(tableName, family);
          }
          
          
          
          //we found a matching key and value class definition,
          //do not continue on other class definitions
          break;
        }
      }
    } catch(IOException ex) {
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
      throw ex;
    } catch(Exception ex) {
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
      throw new IOException(ex);
    }

    return mappingBuilder.build();
  }

  @Override
  public void close() {
    try{
      table.close();
    }catch(IOException ex){
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
    }
  }

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public void setConf(Configuration conf) {
    this.conf = conf;
  }

}
