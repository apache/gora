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
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.util.Utf8;
import org.apache.gora.hbase.query.HBaseGetResult;
import org.apache.gora.hbase.query.HBaseQuery;
import org.apache.gora.hbase.query.HBaseScannerResult;
import org.apache.gora.hbase.store.HBaseMapping.HBaseMappingBuilder;
import org.apache.gora.hbase.util.HBaseByteInterface;
import org.apache.gora.hbase.util.HBaseFilterUtil;
import org.apache.gora.persistency.impl.DirtyListWrapper;
import org.apache.gora.persistency.impl.DirtyMapWrapper;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataStore for HBase. Thread safe.
 *
 */
public class HBaseStore<K, T extends PersistentBase> extends DataStoreBase<K, T>
implements Configurable {

  public static final Logger LOG = LoggerFactory.getLogger(HBaseStore.class);

  public static final String PARSE_MAPPING_FILE_KEY = "gora.hbase.mapping.file";

  public static final String DEFAULT_MAPPING_FILE = "gora-hbase-mapping.xml";

  private static final String SCANNER_CACHING_PROPERTIES_KEY = "scanner.caching" ;
  private static final int SCANNER_CACHING_PROPERTIES_DEFAULT = 0 ;
  
  private volatile Admin admin;

  private volatile HBaseTableConnection table;

  private final boolean autoCreateSchema = true;

  private volatile HBaseMapping mapping;
  
  private HBaseFilterUtil<K, T> filterUtil;

  private int scannerCaching = SCANNER_CACHING_PROPERTIES_DEFAULT ;
  
  /**
   * Default constructor
   */
  public HBaseStore() {//Empty Constrctor
  }

  /**
   * Initialize the data store by reading the credentials, setting the client's properties up and
   * reading the mapping file. Initialize is called when then the call to
   * {@link org.apache.gora.store.DataStoreFactory#createDataStore} is made.
   *
   * @param keyClass
   * @param persistentClass
   * @param properties
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) {
    try {
      super.initialize(keyClass, persistentClass, properties);

      this.conf = HBaseConfiguration.create(getConf());
      admin = ConnectionFactory.createConnection(getConf()).getAdmin();
      mapping = readMapping(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
      filterUtil = new HBaseFilterUtil<>(this.conf);
    } catch (FileNotFoundException ex) {
      LOG.error("{}  is not found, please check the file.", DEFAULT_MAPPING_FILE);
      throw new RuntimeException(ex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Set scanner caching option
    try {
      this.setScannerCaching(
          Integer.valueOf(DataStoreFactory.findProperty(this.properties, this,
              SCANNER_CACHING_PROPERTIES_KEY,
              String.valueOf(SCANNER_CACHING_PROPERTIES_DEFAULT)))) ;
    }catch(Exception e){
      LOG.error("Can not load {} from gora.properties. Setting to default value: {}.", SCANNER_CACHING_PROPERTIES_KEY, SCANNER_CACHING_PROPERTIES_DEFAULT);
      this.setScannerCaching(SCANNER_CACHING_PROPERTIES_DEFAULT) ; // Default value if something is wrong
    }

    if(autoCreateSchema) {
      createSchema();
    }
    try{
      boolean autoflush = this.conf.getBoolean("hbase.client.autoflush.default", false);
      table = new HBaseTableConnection(getConf(), getSchemaName(), autoflush);
    } catch(IOException ex2){
      LOG.error(ex2.getMessage(), ex2);
    }
    closeHBaseAdmin();
  }

  @Override
  public String getSchemaName() {
    //return the name of this table
    return mapping.getTableName();
  }
  
  public HBaseMapping getMapping() {
    return mapping;
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
      LOG.error(ex2.getMessage(), ex2);
    }
    closeHBaseAdmin();
  }

  @Override
  public void deleteSchema() {
    try{
      if(!schemaExists()) {
        return;
      }
      admin.disableTable(mapping.getTable().getTableName());
      admin.deleteTable(mapping.getTable().getTableName());
    } catch(IOException ex2){
      LOG.error(ex2.getMessage(), ex2);
    }
    closeHBaseAdmin();
  }

  @Override
  public boolean schemaExists() {
    try{
      return admin.tableExists(mapping.getTable().getTableName());
    } catch(IOException ex2){
      LOG.error(ex2.getMessage(), ex2);
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
      LOG.error(ex2.getMessage(), ex2);
      return null;
    }
  }

  /**
   * {@inheritDoc} Serializes the Persistent data and saves in HBase. Topmost
   * fields of the record are persisted in "raw" format (not avro serialized).
   * This behavior happens in maps and arrays too.
   * 
   * ["null","type"] type (a.k.a. optional field) is persisted like as if it is
   * ["type"], but the column get deleted if value==null (so value read after
   * will be null).
   * 
   * @param persistent
   *          Record to be persisted in HBase
   */
  @Override
  public void put(K key, T persistent) {
    try {
      Schema schema = persistent.getSchema();
      byte[] keyRaw = toBytes(key);
      Put put = new Put(keyRaw);
      Delete delete = new Delete(keyRaw);
      List<Field> fields = schema.getFields();
      for (int i = 0; i < fields.size(); i++) {
        if (!persistent.isDirty(i)) {
          continue;
        }
        Field field = fields.get(i);
        Object o = persistent.get(i);
        HBaseColumn hcol = mapping.getColumn(field.name());
        if (hcol == null) {
          throw new RuntimeException("HBase mapping for field ["
              + persistent.getClass().getName() + "#" + field.name()
              + "] not found. Wrong gora-hbase-mapping.xml?");
        }
        addPutsAndDeletes(put, delete, o, field.schema().getType(),
            field.schema(), hcol, hcol.getQualifier());
      }

      if (delete.size() > 0) {
        table.delete(delete);
//        table.delete(delete);
//        table.delete(delete); // HBase sometimes does not delete arbitrarily
      }
      if (put.size() > 0) {
        table.put(put);
      }
    } catch (IOException ex2) {
      LOG.error(ex2.getMessage(), ex2);
    }
  }

  private void addPutsAndDeletes(Put put, Delete delete, Object o, Type type,
      Schema schema, HBaseColumn hcol, byte[] qualifier) throws IOException {
    switch (type) {
    case UNION:
      if (isNullable(schema) && o == null) {
        if (qualifier == null) {
//          delete.deleteFamily(hcol.getFamily());
          delete.addFamily(hcol.getFamily());
        } else {
//          delete.deleteColumn(hcol.getFamily(), qualifier);
          delete.addColumns(hcol.getFamily(), qualifier);
        }
      } else {
//        int index = GenericData.get().resolveUnion(schema, o);
        int index = getResolvedUnionIndex(schema);
        if (index > 1) {  //if more than 2 type in union, serialize directly for now
          byte[] serializedBytes = toBytes(o, schema);
          put.addColumn(hcol.getFamily(), qualifier, serializedBytes);
        } else {
          Schema resolvedSchema = schema.getTypes().get(index);
          addPutsAndDeletes(put, delete, o, resolvedSchema.getType(),
              resolvedSchema, hcol, qualifier);
        }
      }
      break;
    case MAP:
      // if it's a map that has been modified, then the content should be replaced by the new one
      // This is because we don't know if the content has changed or not.
      if (qualifier == null) {
        //delete.deleteFamily(hcol.getFamily());
        delete.addFamily(hcol.getFamily());
      } else {
        //delete.deleteColumn(hcol.getFamily(), qualifier);
        delete.addColumns(hcol.getFamily(), qualifier);
      }
      @SuppressWarnings({ "rawtypes", "unchecked" })
      Set<Entry> set = ((Map) o).entrySet();
      for (@SuppressWarnings("rawtypes") Entry entry : set) {
        byte[] qual = toBytes(entry.getKey());
        addPutsAndDeletes(put, delete, entry.getValue(), schema.getValueType()
            .getType(), schema.getValueType(), hcol, qual);
      }
      break;
    case ARRAY:
      List<?> array = (List<?>) o;
      int j = 0;
      for (Object item : array) {
        addPutsAndDeletes(put, delete, item, schema.getElementType().getType(),
            schema.getElementType(), hcol, Bytes.toBytes(j++));
      }
      break;
    default:
      byte[] serializedBytes = toBytes(o, schema);
      put.addColumn(hcol.getFamily(), qualifier, serializedBytes);
      break;
    }
  }

  private boolean isNullable(Schema unionSchema) {
    for (Schema innerSchema : unionSchema.getTypes()) {
      if (innerSchema.getType().equals(Schema.Type.NULL)) {
        return true;
      }
    }
    return false;
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
      LOG.error(ex2.getMessage(), ex2);
      return false;
    }
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    try {
      String[] fields = getFieldsToQuery(query.getFields());
      //find whether all fields are queried, which means that complete
      //rows will be deleted
      boolean isAllFields = Arrays.equals(fields, getFields());
  
      org.apache.gora.query.Result<K, T> result = null;
      result = query.execute();
      ArrayList<Delete> deletes = new ArrayList<>();
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
      LOG.error(ex.getMessage(), ex);
      return -1;
    }
  }

  @Override
  public void flush() {
    try{
      table.flushCommits();
    }catch(IOException ex){
      LOG.error(ex.getMessage(), ex);
    }
  }

  @Override
  public Query<K, T> newQuery() {
    return new HBaseQuery<>(this);
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query)
      throws IOException {

    if (table == null) {
      throw new IOException("No table was provided.");
    }

    // taken from o.a.h.hbase.mapreduce.TableInputFormatBase
    Pair<byte[][], byte[][]> keys = table.getStartEndKeys();
    if (keys == null || keys.getFirst() == null ||
        keys.getFirst().length == 0) {
      throw new IOException("Expecting at least one region.");
    }

    List<PartitionQuery<K,T>> partitions = new ArrayList<>(keys.getFirst().length);
    for (int i = 0; i < keys.getFirst().length; i++) {
      String regionLocation = table.getRegionLocation(keys.getFirst()[i]).getHostname();
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

        PartitionQueryImpl<K, T> partition = new PartitionQueryImpl<>(
            query, startKey, endKey, regionLocation);
        partition.setConf(getConf());

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
        return new HBaseGetResult<>(this, query, result);
      } else {
        ResultScanner scanner = createScanner(query);
  
        org.apache.gora.query.Result<K,T> result
            = new HBaseScannerResult<>(this, query, scanner);
  
        return result;
      }
    }catch(IOException ex){
      LOG.error(ex.getMessage(), ex);
      return null;
    }
  }

  public ResultScanner createScanner(Query<K, T> query) throws IOException {
    final Scan scan = new Scan();
    
    scan.setCaching(this.getScannerCaching()) ; 
    
    if (query.getStartKey() != null) {
      scan.setStartRow(toBytes(query.getStartKey()));
    }
    if (query.getEndKey() != null) {
      scan.setStopRow(toBytes(query.getEndKey()));
    }
    addFields(scan, query);
    if (query.getFilter() != null) {
      boolean succeeded = filterUtil.setFilter(scan, query.getFilter(), this);
      if (succeeded) {
        // don't need local filter
        query.setLocalFilterEnabled(false);
      }
    }

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
      addFamilyOrColumn(get, col, fieldSchema);
    }
  }

  private void addFamilyOrColumn(Get get, HBaseColumn col, Schema fieldSchema) {
    switch (fieldSchema.getType()) {
    case UNION:
      int index = getResolvedUnionIndex(fieldSchema);
      Schema resolvedSchema = fieldSchema.getTypes().get(index);
      addFamilyOrColumn(get, col, resolvedSchema);
      break;
    case MAP:
    case ARRAY:
      get.addFamily(col.family);
      break;
    default:
      get.addColumn(col.family, col.qualifier);
      break;
    }
  }

  private void addFields(Scan scan, Query<K, T> query) throws IOException {
    String[] fields = query.getFields();
    for (String f : fields) {
      HBaseColumn col = mapping.getColumn(f);
      if (col == null) {
        throw new  RuntimeException("HBase mapping for field ["+ f +"] not found. " +
            "Wrong gora-hbase-mapping.xml?");
      }
      Schema fieldSchema = fieldMap.get(f).schema();
      addFamilyOrColumn(scan, col, fieldSchema);
    }
  }

  private void addFamilyOrColumn(Scan scan, HBaseColumn col, Schema fieldSchema) {
    switch (fieldSchema.getType()) {
    case UNION:
      int index = getResolvedUnionIndex(fieldSchema);
      Schema resolvedSchema = fieldSchema.getTypes().get(index);
      addFamilyOrColumn(scan, col, resolvedSchema);
      break;
    case MAP:
    case ARRAY:
      scan.addFamily(col.family);
      break;
    default:
      scan.addColumn(col.family, col.qualifier);
      break;
    }
  }

  // TODO: HBase Get, Scan, Delete should extend some common interface with
  // addFamily, etc
  private void addFields(Delete delete, Query<K, T> query)    throws IOException {
    String[] fields = query.getFields();
    for (String f : fields) {
      HBaseColumn col = mapping.getColumn(f);
      if (col == null) {
        throw new  RuntimeException("HBase mapping for field ["+ f +"] not found. " +
            "Wrong gora-hbase-mapping.xml?");
      }
      Schema fieldSchema = fieldMap.get(f).schema();
      addFamilyOrColumn(delete, col, fieldSchema);
    }
  }

  private void addFamilyOrColumn(Delete delete, HBaseColumn col,
      Schema fieldSchema) {
    switch (fieldSchema.getType()) {
    case UNION:
      int index = getResolvedUnionIndex(fieldSchema);
      Schema resolvedSchema = fieldSchema.getTypes().get(index);
      addFamilyOrColumn(delete, col, resolvedSchema);
      break;
    case MAP:
    case ARRAY:
      delete.addFamily(col.family);
      break;
    default:
      delete.addColumns(col.family, col.qualifier);
      break;
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
    for (String f : fields) {
      HBaseColumn col = mapping.getColumn(f);
      if (col == null) {
        throw new  RuntimeException("HBase mapping for field ["+ f +"] not found. " +
            "Wrong gora-hbase-mapping.xml?");
      }
      Field field = fieldMap.get(f);
      Schema fieldSchema = field.schema();
      setField(result,persistent, col, field, fieldSchema);
    }
    persistent.clearDirty();
    return persistent;
  }

  private void setField(Result result, T persistent, HBaseColumn col,
      Field field, Schema fieldSchema) throws IOException {
    switch (fieldSchema.getType()) {
    case UNION:
      int index = getResolvedUnionIndex(fieldSchema);
      if (index > 1) { //if more than 2 type in union, deserialize directly for now
        byte[] val = result.getValue(col.getFamily(), col.getQualifier());
        if (val == null) {
          return;
        }
        setField(persistent, field, val);
      } else {
        Schema resolvedSchema = fieldSchema.getTypes().get(index);
        setField(result, persistent, col, field, resolvedSchema);
      }
      break;
    case MAP:
      NavigableMap<byte[], byte[]> qualMap = result.getNoVersionMap().get(
          col.getFamily());
      if (qualMap == null) {
        return;
      }
      Schema valueSchema = fieldSchema.getValueType();
      Map<Utf8, Object> map = new HashMap<>();
      for (Entry<byte[], byte[]> e : qualMap.entrySet()) {
        map.put(new Utf8(Bytes.toString(e.getKey())),
            fromBytes(valueSchema, e.getValue()));
      }
      setField(persistent, field, map);
      break;
    case ARRAY:
      qualMap = result.getFamilyMap(col.getFamily());
      if (qualMap == null) {
        return;
      }
      valueSchema = fieldSchema.getElementType();
      ArrayList<Object> arrayList = new ArrayList<>();
      DirtyListWrapper<Object> dirtyListWrapper = new DirtyListWrapper<>(arrayList);
      for (Entry<byte[], byte[]> e : qualMap.entrySet()) {
        dirtyListWrapper.add(fromBytes(valueSchema, e.getValue()));
      }
      setField(persistent, field, arrayList);
      break;
    default:
      byte[] val = result.getValue(col.getFamily(), col.getQualifier());
      if (val == null) {
        return;
      }
      setField(persistent, field, val);
      break;
    }
  }

  //TODO temporary solution, has to be changed after implementation of saving the index of union type
  private int getResolvedUnionIndex(Schema unionScema) {
    if (unionScema.getTypes().size() == 2) {

      // schema [type0, type1]
      Type type0 = unionScema.getTypes().get(0).getType();
      Type type1 = unionScema.getTypes().get(1).getType();

      // Check if types are different and there's a "null", like ["null","type"]
      // or ["type","null"]
      if (!type0.equals(type1)
          && (type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL))) {

        if (type0.equals(Schema.Type.NULL))
          return 1;
        else
          return 0;
      }
    }
    return 2;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void setField(T persistent, Field field, Map map) {
    persistent.put(field.pos(), new DirtyMapWrapper(map));
  }

  private void setField(T persistent, Field field, byte[] val)
  throws IOException {
    persistent.put(field.pos(), fromBytes(field.schema(), val));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void setField(T persistent, Field field, List list) {
    persistent.put(field.pos(), new DirtyListWrapper(list));
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
          LOG.debug("Keyclass and nameclass match.");

          String tableNameFromMapping = classElement.getAttributeValue("table");
          String tableName = getSchemaName(tableNameFromMapping, persistentClass);
          
          //tableNameFromMapping could be null here
          if (!tableName.equals(tableNameFromMapping)) {
          //TODO this might not be the desired behavior as the user might have actually made a mistake.
            LOG.warn("Mismatching schema's names. Mappingfile schema: '{}'. PersistentClass schema's name: '{}'. Assuming they are the same.", tableNameFromMapping, tableName);
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
        } else {
          LOG.error("KeyClass in gora-hbase-mapping is not the same as the one in the databean.");
        }
      }
    } catch (MalformedURLException ex) {
      LOG.error("Error while trying to read the mapping file {}. "
              + "Expected to be in the classpath "
              + "(ClassLoader#getResource(java.lang.String)).",
              filename) ;
      LOG.error("Actual classpath = {}", Arrays.asList(
          ((URLClassLoader) getClass().getClassLoader()).getURLs()));
      throw ex ;
    } catch(IOException ex) {
      LOG.error(ex.getMessage(), ex);
      throw ex;
    } catch(Exception ex) {
      LOG.error(ex.getMessage(), ex);
      throw new IOException(ex);
    }

    return mappingBuilder.build();
  }

  @Override
  public void close() {
    try{
      table.close();
    }catch(IOException ex){
      LOG.error(ex.getMessage(), ex);
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

  /**
   * Gets the Scanner Caching optimization value
   * @return The value used internally in {@link Scan#setCaching(int)}
   */
  public int getScannerCaching() {
    return this.scannerCaching ;
  }
  
  /**
   * Sets the value for Scanner Caching optimization
   * 
   * @see Scan#setCaching(int)
   * 
   * @param numRows the number of rows for caching {@literal >=} 0
   * @return &lt;&lt;Fluent interface&gt;&gt;
   */
  public HBaseStore<K, T> setScannerCaching(int numRows) {
    if (numRows < 0) {
      LOG.warn("Invalid Scanner Caching optimization value. Cannot set to: {}.", numRows) ;
      return this ;
    }
    this.scannerCaching = numRows ;
    return this ;
  }
  
  private void closeHBaseAdmin(){
    try {
      admin.close();
    } catch (IOException ioe) {
      LOG.error("An error occured whilst closing HBase Admin", ioe);
    }
  }
}
