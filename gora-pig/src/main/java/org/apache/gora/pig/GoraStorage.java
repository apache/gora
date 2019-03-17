/*
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
package org.apache.gora.pig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData.Array;
import org.apache.gora.mapreduce.GoraMapReduceUtils;
import org.apache.gora.mapreduce.GoraRecordReader;
import org.apache.gora.mapreduce.GoraRecordWriter;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.pig.mapreduce.PigGoraInputFormat;
import org.apache.gora.pig.mapreduce.PigGoraOutputFormat;
import org.apache.gora.pig.util.PersistentUtils;
import org.apache.gora.pig.util.SchemaUtils;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.ClassLoadingUtils;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.pig.Expression;
import org.apache.pig.LoadCaster;
import org.apache.pig.LoadFunc;
import org.apache.pig.LoadMetadata;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceSchema.ResourceFieldSchema;
import org.apache.pig.ResourceStatistics;
import org.apache.pig.StoreFuncInterface;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.builtin.Utf8StorageConverter;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.util.UDFContext;
import org.apache.pig.impl.util.Utils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage for Apache Pig.
 *
 *
 * <h1>Read tuples</h1>
 * 
 * Example of usage:
 *
 * <pre>
 * set job.name 'GoraPig test';
 * register gora/*.jar;
 * webpage = LOAD '.' USING org.apache.gora.pig.GoraStorage('{
 *       "persistentClass": "admin.WebPage",
 *       "fields": "baseUrl,status,content",
 *       "goraProperties": "gora.datastore.default=org.apache.gora.hbase.store.HBaseStore\\ngora.datastore.autocreateschema=true\\ngora.hbasestore.scanner.caching=4",
 *       "mapping": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n<gora-odm>\\n<table name=\\"webpage\\">\\n<family name=\\"f\\" maxVersions=\\"1\\"/>\\n</table>\\n<class table=\\"webpage\\" keyClass=\\"java.lang.String\\" name=\\"admin.WebPage\\">\\n<field name=\\"baseUrl\\" family=\\"f\\" qualifier=\\"bas\\"/>\\n<field name=\\"status\\" family=\\"f\\" qualifier=\\"st\\"/>\\n<field name=\\"content\\" family=\\"f\\" qualifier=\\"cnt\\"/>\\n</class>\\n</gora-odm>",
 *       "configuration": {
 *           "hbase.zookeeper.quorum": "hdp4,hdp1,hdp3",
 *           "zookeeper.znode.parent": "/hbase-unsecure"
 *       }
 * }') ;
 * dump webpage ;
 * </pre>
 *
 * If the files gora.properties, gora-hbase-mapping.xml and hbase-site.xml are provided
 * through the classpath to Pig client and including inside the registered *.jar, the
 * corresponding configuration parameters can be omitted. 
 *
 * There is another option: "keyClass" but it is omitted since the only key class supported at this
 * moment is "java.lang.String".
 *
 * In the example above, the folder gora that is being registered must contain:
 * <ul>
 *   <li>All the contents of <code>gora-pig/lib</code> (THIS LIST MUST BE REFINED), being the dependences needed for GoraStorage</li>
 *   <li>gora-pig jar</li>
 *   <li>gora-x jar relative to the used backend</li>
 *   <li>A .jar file with the compiled entities (<code>admin.Webpage</code> in the example)</li>
 *   <li>Optionally a .jar file with the gora.properties, mapping and configuration files like HBase's hbase-site.xml.</li>
 * </ul>
 * and the <code>/lib</code> folder of Apache Pig installation must contain all that files too (THIS LIST MUST BE REFINED).
 *
 * <h1>Saving values</h1>
 * 
 * To store values, the usage is:
 * 
 * <pre>
 * STORE webpages INTO '.' USING org.apache.gora.pig.GoraStorage('{
 *       "persistentClass": "admin.WebPage",
 *       "fields": "baseUrl,status,content",
 *       "goraProperties": "gora.datastore.default=org.apache.gora.hbase.store.HBaseStore\\ngora.datastore.autocreateschema=true\\ngora.hbasestore.scanner.caching=4",
 *       "mapping": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n<gora-odm>\\n<table name=\\"webpage\\">\\n<family name=\\"f\\" maxVersions=\\"1\\"/>\\n</table>\\n<class table=\\"webpage\\" keyClass=\\"java.lang.String\\" name=\\"admin.WebPage\\">\\n<field name=\\"baseUrl\\" family=\\"f\\" qualifier=\\"bas\\"/>\\n<field name=\\"status\\" family=\\"f\\" qualifier=\\"st\\"/>\\n<field name=\\"content\\" family=\\"f\\" qualifier=\\"cnt\\"/>\\n</class>\\n</gora-odm>",
 *       "configuration": {
 *           "hbase.zookeeper.quorum": "hdp4,hdp1,hdp3",
 *           "zookeeper.znode.parent": "/hbase-unsecure"
 *       }
 * }') ;
 * </pre>
 * 
 * All the declared fields in the "fields" configuration option must exist in the pig relation schema being written, and if there is a field declared that doesn't exist in
 * the tuple the process fails with an exception.
 * 
 * ... Maybe the behavior should be to accept missing fields and allow to configure some "failIfFieldMissing" option.
 * 
 * <h2>About the source code and integration with Apache Pig</h2>
 *
 * Here is a resume on some insight about how does Pig works, and in what order does it calls the Storage methdos.
 * In this class code <code>UDFContext.getUDFContext().isFrontend()</code> returns true when the Storage is a frontend instance.
 * BUT must know that the Storage classes are instanced several times at frontend. In the example on top, this is the order of calls
 * and the several different instances of GoraStorage:
 *
 * On LOAD at frontend:
 * <pre>
 * GoraStorage constructor()            org.apache.gora.pig.GoraStorage@506a1372 [A]
 * GoraStorage setUDFContextSignature() org.apache.gora.pig.GoraStorage@506a1372 [A]
 * GoraStorage getSchema()              org.apache.gora.pig.GoraStorage@506a1372 [A]
 * GoraStorage constructor()            org.apache.gora.pig.GoraStorage@443faa85 [B]
 * GoraStorage setUDFContextSignature() org.apache.gora.pig.GoraStorage@443faa85 [B]
 * GoraStorage getSchema()              org.apache.gora.pig.GoraStorage@443faa85 [B]
 * </pre>
 *
 * On DUMP at frontend:
 * <pre>
 * GoraStorage constructor()            org.apache.gora.pig.GoraStorage@411a5965 [C]
 * GoraStorage setUDFContextSignature() org.apache.gora.pig.GoraStorage@411a5965 [C]
 * GoraStorage getSchema()              org.apache.gora.pig.GoraStorage@411a5965 [C]
 * GoraStorage setLocation()            org.apache.gora.pig.GoraStorage@411a5965 [C]
 *  == start copying JARS to distributed cache
 * GoraStorage constructor()            org.apache.gora.pig.GoraStorage@2307c09e [D]
 * GoraStorage setUDFContextSignature() org.apache.gora.pig.GoraStorage@2307c09e [D]
 * GoraStorage setLocation()            org.apache.gora.pig.GoraStorage@2307c09e [D]
 * GoraStorage getInputFormat()         org.apache.gora.pig.GoraStorage@2307c09e [D]
 * </pre>
 *
 * On DUMP at backend:
 * <pre>
 * GoraStorage constructor()            org.apache.gora.pig.GoraStorage@680d4a6a [E]
 * GoraStorage setUDFContextSignature() org.apache.gora.pig.GoraStorage@680d4a6a [E]
 * GoraStorage setLocation()            org.apache.gora.pig.GoraStorage@680d4a6a [E]
 * GoraStorage getInputFormat()         org.apache.gora.pig.GoraStorage@680d4a6a [E]
 * GoraStorage setUDFContextSignature() org.apache.gora.pig.GoraStorage@680d4a6a [E]
 * GoraStorage prepareToRead()          org.apache.gora.pig.GoraStorage@680d4a6a [E]
 * GoraStorage getNext()                org.apache.gora.pig.GoraStorage@680d4a6a [E] - repeated until end
 * </pre>
 */
public class GoraStorage extends LoadFunc implements StoreFuncInterface, LoadMetadata {

  public static final Logger LOG = LoggerFactory.getLogger(GoraStorage.class);
  
  /**
   * Key in UDFContext properties used to pass the STORE Pig Schema from frontend to backend.
   */
  protected static final String GORA_STORE_PIG_SCHEMA = "gorastorage.pig.store.schema" ;

  /**
   * Job that Pig configures
   */
  protected Job job;
  
  /**
   * Local job with the local core-site.xml + UDFProperties data
   */
  protected JobConf localJobConf ; 
  
  /**
   * Signature for each different GoraStore (based on configuration), that determines the UDFProperties to use
   */
  protected String udfcSignature = null ;
  
  /**
   * The GoraStorage configuration setted at constructor (converted from json to bean)
   */
  protected StorageConfiguration storageConfiguration ;
  
  protected Class<?> keyClass;
  protected Class<? extends PersistentBase> persistentClass;
  protected Schema persistentSchema ;
  private   DataStore<?, ? extends PersistentBase> dataStore ;
  protected PigGoraInputFormat<?,? extends PersistentBase> inputFormat ;
  protected GoraRecordReader<?,? extends PersistentBase> reader ;
  protected PigGoraOutputFormat<?,? extends PersistentBase> outputFormat ;
  protected GoraRecordWriter<?,? extends PersistentBase> writer ;
  protected PigSplit split ;
  protected ResourceSchema readResourceSchema ;
  protected ResourceSchema writeResourceSchema ;
  protected Map<String, ResourceFieldSchemaWithIndex> writeResourceFieldSchemaMap ;
  
  /** Fields to load as Query - but without 'key' - */
  protected List<String> loadQueryFields ;
  
  private ObjectMapper mapper = new ObjectMapper() ;
  
  /**
   * Private attribute to hold the cache of the index of the "key" field in the pig tuple when STORE in executed
   */
  protected int pigFieldKeyIndex ;
  
  /**
   * Creates a new GoraStorage to load/save.
   * 
   * The constructor must have a JSON configuration string with the following fields (optional values in brackets):
   * 
   * <pre>
   * {
   *     ["keyClass": "",]
   *     "persistentClass: "",
   *     "fields": "",
   *     ["goraProperties": "",]
   *     ["mapping": "",]
   *     ["configuration": {}]
   * }
   * </pre>
   * 
   * <ul>
   *   <li>keyClass: full class name with namespace of the key class. By default "java.lang.String".</li>
   *   <li>persistentClass: full class name with namespace of the compiled PersistentBase class.</li>
   *   <li>fields: comma-separated list of fields to load, from the first level of fields in the persistent class.. Can use "*" to denote to load all fields, or to save all columns of the pig tuples.</li>
   *   <li>goraProperties: string with the content to use as gora.properties. If this key is missing, will try to load the values from the local file gora.properties.</li>
   *   <li>mapping: string with the XML content to use as gora-xxx-mapping.xml. If this key is missing, will try to load from local file.</li>
   *   <li>configuration: object with string key:values to override in the job configuration for hadoop.
   * </ul>
   * 
   * Example of usage:
   * <pre>
   * webpage = LOAD '.' USING org.apache.gora.pig.GoraStorage('{
   *   "persistentClass": "admin.WebPage",
   *   "fields": "baseUrl,status",
   *   "goraProperties": "gora.datastore.default=org.apache.gora.hbase.store.HBaseStore
   *                      gora.datastore.autocreateschema=true
   *                      gora.hbasestore.scanner.caching=1000",
   *   "mapping": "<?xml version=\\"1.0\\" encoding="UTF-8\\"?>
   *               <gora-odm>
   *                 <table name=\\"webpage\\">
   *                   <family name=\\"f\\" maxVersions=\\"1\\"/>
   *                 </table>
   *                 <class table=\\"webpage\\" keyClass=\\"java.lang.String\\" name=\\"admin.WebPage\\">
   *                   <field name=\\"baseUrl\\" family=\\"f\\" qualifier=\\"bas\\"/>
   *                   <field name=\\"status\\" family=\\"f\\" qualifier=\\"st\\"/>
   *                 </class>
   *               </gora-odm>",
   *   "configuration": {
   *     "hbase.zookeeper.quorum": "hdp4,hdp1,hdp3",
   *     "zookeeper.znode.parent": "/hbase-unsecure"
   *   }
   * }') ;
   * </pre>
   * 
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  public GoraStorage(String storageConfigurationString) throws InstantiationException, IllegalAccessException, JsonParseException, JsonMappingException, IOException {
    super() ;
    this.storageConfiguration = this.mapper.readValue(storageConfigurationString, StorageConfiguration.class) ;
    
    try {
      this.keyClass = ClassLoadingUtils.loadClass(this.storageConfiguration.getKeyClass());
      this.persistentClass = ClassLoadingUtils.loadClass(this.storageConfiguration.getPersistentClass()).asSubclass(PersistentBase.class);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Error creating instance of key and/or persistent.",e);
    }
    
    this.persistentSchema = this.persistentClass.newInstance().getSchema() ;

    // Populate this.loadQueryFields
    List<String> declaredConstructorFields = new ArrayList<String>() ;
    if (this.storageConfiguration.isAllFieldsQuery()) {
      for (Field field : this.persistentSchema.getFields()) {
        declaredConstructorFields.add(field.name()) ;
      }
    } else {
      declaredConstructorFields.addAll(this.storageConfiguration.getFieldsAsList()) ;
    }
    this.loadQueryFields = declaredConstructorFields ;
  }

  /**
   * Returns the internal DataStore for <code>&lt;keyClass,persistentClass&gt;</code>
   * using configuration set in job (from setLocation()).
   * Creates one datastore at first call.
   * @return DataStore for &lt;keyClass,persistentClass&gt;
   * @throws GoraException on DataStore creation error.
   */
  @SuppressWarnings("rawtypes")
  protected DataStore getDataStore() throws GoraException {
    if (this.dataStore == null) {      
      try {
        this.dataStore = DataStoreFactory.getDataStore(
            this.storageConfiguration.getKeyClass(),
            this.storageConfiguration.getPersistentClass(),
            this.storageConfiguration.getGoraPropertiesAsProperties(),
            this.localJobConf
            ) ;
      } catch (IOException e) {
        throw new GoraException(e);
      }
    }
    return this.dataStore ;
  }
  
  /**
   * Gets the job, initializes the localJobConf (the actual used to create a datastore)
   */
  @Override
  public void setLocation(String location, Job job) throws IOException {
    GoraMapReduceUtils.setIOSerializations(job.getConfiguration(), true) ;
    // All Splits return length==0, but must not be combined (because actually are not ==0)
    job.getConfiguration().setBoolean("pig.noSplitCombination", true);
    this.mergeGoraStorageConfigurationInto(job.getConfiguration());
    this.job = job;
    this.localJobConf = new JobConf(job.getConfiguration()) ;
    this.mergeGoraStorageConfigurationInto(this.localJobConf) ;
  }

  /**
   * Returns UDFProperties based on <code>udfcSignature</code>, <code>keyClassName</code> and <code>persistentClassName</code>.
   * @throws IOException - When the conversion bean->json fails
   * @throws JsonMappingException - When the conversion bean->json fails
   * @throws JsonGenerationException - When the conversion bean->json fails
   */
  protected Properties getUDFProperties() throws JsonGenerationException, JsonMappingException, IOException {
    return UDFContext.getUDFContext().getUDFProperties(this.getClass(), new String[] {this.udfcSignature});
  }
  
  /**
   * Merges the configuration from the constructor into a Configuration. If the keys exists, they are not overwritten.
   * 
   * @param configuration - The configuration to update with the non-existant keys-values
   * @return
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonGenerationException 
   */
  private void mergeGoraStorageConfigurationInto(Configuration configuration) {
    GoraMapReduceUtils.setIOSerializations(configuration, true) ;

    // Set the constructor configuration into the hadoop configuration
    this.storageConfiguration.mergeConfiguration(configuration) ;
  }
  
  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public InputFormat getInputFormat() throws IOException {
    this.inputFormat = new PigGoraInputFormat() ;
    
    // Here is where the QUERY is set
    Query query = this.getDataStore().newQuery() ;
    if (!this.storageConfiguration.isAllFieldsQuery()) {
      query.setFields(this.loadQueryFields.toArray(new String[0])) ;
    }
    this.inputFormat.setQuery(query);
    
    this.inputFormat.setStorageConfiguration(this.storageConfiguration);
    this.inputFormat.setDataStore(this.getDataStore()) ;
    this.inputFormat.setConf(this.localJobConf);
    
    return this.inputFormat ; 
  }

  @Override
  public LoadCaster getLoadCaster() throws IOException {
    return new Utf8StorageConverter();
  }

  @Override
  @SuppressWarnings({ "rawtypes" })
  public void prepareToRead(RecordReader reader, PigSplit split)
      throws IOException {
    this.reader = (GoraRecordReader<?, ?>) reader;
    this.split = split;
  }

  @Override
  public Tuple getNext() throws IOException {

    try {
      if (!this.reader.nextKeyValue()) {
          return null;
      }
    } catch (Exception e) {
      throw new IOException("Error retrieving next key-value.", e);
    }

    PersistentBase persistentObj;
    Object persistentKey ;

    try {
      persistentKey = this.reader.getCurrentKey() ;
      persistentObj = this.reader.getCurrentValue();
    } catch (Exception e) {
      throw new IOException("Error reading next key-value.", e);
    }

    return PersistentUtils.persistent2Tuple(persistentKey, persistentObj, this.loadQueryFields) ;
  }
    
  @Override
  public void setUDFContextSignature(String signature) {
    this.udfcSignature = signature;
  }

  @Override
  public String relativeToAbsolutePath(String location, Path curDir) 
      throws IOException {
    // Do nothing
    return location ;
  }
  
  /**
   * Retrieves the Pig Schema from the declared fields in constructor and the Avro Schema
   * Avro Schema must begin with a record.
   * Pig Schema will be a Tuple (in 1st level) with $0 = "key":rowkey
   */
  @Override
  public ResourceSchema getSchema(String location, Job job) throws IOException {
    // Reuse if already created
    if (this.readResourceSchema != null) return this.readResourceSchema ;
    
    // Save Pig schema inside the instance
    this.readResourceSchema = SchemaUtils.generatePigSchema(this.persistentSchema, this.loadQueryFields, this.keyClass) ;
    return this.readResourceSchema ;
  }
  
  @Override
  public ResourceStatistics getStatistics(String location, Job job)
      throws IOException {
    // TODO Not implemented
    return null;
  }

  @Override
  /**
   * Disabled by now (returns null).
   * Later we will consider only one partition key: row key
   */
  public String[] getPartitionKeys(String location, Job job) throws IOException {
    // TODO Disabled by now
    return null ;
    //return new String[] {"key"} ;
  }

  @Override
  /**
   * Ignored by now since getPartitionKeys() return null
   */
  public void setPartitionFilter(Expression partitionFilter) throws IOException {
    // TODO Ignored since getPartitionsKeys() return null
    throw new IOException() ;
  }

  @Override
  public String relToAbsPathForStoreLocation(String location, Path curDir)
      throws IOException {
    // Do nothing
    return location ;
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public OutputFormat getOutputFormat() throws IOException {
    this.outputFormat = new PigGoraOutputFormat() ;
    this.outputFormat.setStorageConfiguration(this.storageConfiguration);
    this.outputFormat.setDataStore(this.getDataStore()) ;
    this.outputFormat.setConf(this.localJobConf);
    return this.outputFormat ; 
  }

  @Override
  public void setStoreLocation(String location, Job job) throws IOException {
    GoraMapReduceUtils.setIOSerializations(job.getConfiguration(), true) ;
    this.job = job ;
    this.localJobConf = new JobConf(job.getConfiguration()) ;
    this.mergeGoraStorageConfigurationInto(this.localJobConf);
  }

  @Override
  /**
   * Set the schema for data to be stored.  This will be called on the
   * front end during planning if the store is associated with a schema.
   * 
   * Checks the pig schema using names, using the first element of the tuple as key (fieldname = 'key').
   * (key:key, name:recordfield, name:recordfield, name:recordfi...)
   * 
   * Sets UDFContext property GORA_STORE_SCHEMA with the schema to send it to the backend.
   * 
   * Not present names for recordfields will be treated as null .
   * 
   * @param pigSchema to be checked
   * @throws IOException if this schema is not acceptable.
   */
  public void checkSchema(ResourceSchema pigSchema) throws IOException {
    SchemaUtils.checkStoreSchema(pigSchema, this.loadQueryFields, this.persistentSchema);
    // Save the schema to UDFContext to use it on backend when writing data
    this.getUDFProperties().setProperty(GoraStorage.GORA_STORE_PIG_SCHEMA, pigSchema.toString()) ;
  }
  
  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void prepareToWrite(RecordWriter writer) throws IOException {
    // Get the schema of data to write from UDFContext (coming from frontend checkSchema())
    String strSchema = this.getUDFProperties().getProperty(GoraStorage.GORA_STORE_PIG_SCHEMA) ;
    this.writer = (GoraRecordWriter<?,? extends PersistentBase>) writer ;

    // Parse de the schema from string stored in properties object
    this.writeResourceSchema = new ResourceSchema(Utils.getSchemaFromString(strSchema)) ;
    this.writeResourceFieldSchemaMap = new HashMap<String, ResourceFieldSchemaWithIndex>() ;
    int index = 0 ;
    for (ResourceFieldSchema fieldSchema : this.writeResourceSchema.getFields()) {
      this.writeResourceFieldSchemaMap.put(fieldSchema.getName(),
                                           new ResourceFieldSchemaWithIndex(fieldSchema, index++)) ;
    }
    this.pigFieldKeyIndex = this.writeResourceFieldSchemaMap.get("key").getIndex() ;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void putNext(Tuple pigTuple) throws IOException {

    PersistentBase persistentObj = this.dataStore.newPersistent() ;

    if (LOG.isTraceEnabled()) LOG.trace("key: {}", pigTuple.get(pigFieldKeyIndex)) ;
    for (String fieldName : this.loadQueryFields) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("  Put fieldname: {}", fieldName) ;
        LOG.trace("      resourcefield schema: {}", this.writeResourceFieldSchemaMap.get(fieldName).getResourceFieldSchema()) ;
        LOG.trace("      value: {} - {}",this.writeResourceFieldSchemaMap.get(fieldName).getIndex(), pigTuple.get(this.writeResourceFieldSchemaMap.get(fieldName).getIndex())) ;
      }
      
      ResourceFieldSchemaWithIndex writeResourceFieldSchemaWithIndex = this.writeResourceFieldSchemaMap.get(fieldName) ;
      if (writeResourceFieldSchemaWithIndex == null) {
        if (LOG.isTraceEnabled()) LOG.trace("Field {} defined in constructor not found in the tuple to persist, skipping field", fieldName) ;
        continue ;
      }
      
      Field persistentField = persistentSchema.getField(fieldName) ;
      if (persistentField == null) {
        throw new IOException("Field " + fieldName + " does not exist in the Gora's Avro schema.") ;
      }
      
      ResourceFieldSchema pigFieldSchema = writeResourceFieldSchemaWithIndex.getResourceFieldSchema() ;
      if (pigFieldSchema == null) {
        throw new IOException("The field " + fieldName + " does not have a Pig schema when writing.") ;
      }

      //TODO Move this put to PersistentUtils
      //TODO Here is used the resourceFieldSchema and the index. Think about optimize if possible
      //TODO Find a better name to this.writeField, like 'tupleToPersistent'
      int persistentFieldIndex = persistentObj.getSchema().getField(fieldName).pos() ;
      persistentObj.put(persistentFieldIndex,
                        this.writeField(persistentField.schema(), pigFieldSchema, pigTuple.get(writeResourceFieldSchemaWithIndex.getIndex()))) ;
      persistentObj.setDirty(persistentFieldIndex);
    }

    try {
      ((GoraRecordWriter<Object,PersistentBase>) this.writer).write(pigTuple.get(pigFieldKeyIndex), (PersistentBase) persistentObj) ;
    } catch (InterruptedException e) {
      throw new IOException("Error writing the tuple.",e) ;
    }
  }
  
  /**
   * Converts one pig field data to PersistentBase Data.
   * 
   * @param avroSchema PersistentBase schema used to create new nested records
   * @param pigField Pig schema of the field being converted
   * @param pigData Pig data relative to the schema
   * @return PersistentBase data
   * @throws IOException
   */
  private Object writeField(Schema avroSchema, ResourceFieldSchema pigField, Object pigData) throws IOException {

    // If data is null, return null (check if avro schema is right)
    if (pigData == null) {
      if (avroSchema.getType() != Type.UNION && avroSchema.getType() != Type.NULL) {
        throw new IOException("Tuple field " + pigField.getName() + " is null, but Avro Schema is not union nor null") ;
      } else {
        return null ;
      }
    }
    
    // If avroSchema is union, it will not be the null field, so select the proper one
    // ONLY SUPPORT 2 ELEMENTS UNION!
    if (avroSchema.getType() == Type.UNION) {
      if (avroSchema.getTypes().get(0).getType() == Schema.Type.NULL) {
        avroSchema = avroSchema.getTypes().get(1) ;        
      } else {
        avroSchema = avroSchema.getTypes().get(0) ;
      }
    }
    
    switch(pigField.getType()) {
      case DataType.DOUBLE:
      case DataType.FLOAT:
      case DataType.LONG:
      case DataType.BOOLEAN:
      case DataType.NULL:
        if (LOG.isTraceEnabled()) LOG.trace("    Writing double, float, long, boolean or null.") ;
        return (Object)pigData ;
      
      case DataType.CHARARRAY:
        if (LOG.isTraceEnabled()) LOG.trace("    Writing chararray.") ;
        return pigData.toString() ;
      
      case DataType.INTEGER:
        if (LOG.isTraceEnabled()) LOG.trace("    Writing integer/enum.") ;
        if (avroSchema.getType() == Type.ENUM) {
          return AvroUtils.getEnumValue(avroSchema, ((Number)pigData).intValue());
        }else{
          return ((Number)pigData).intValue() ;
        }
          
      case DataType.BYTEARRAY:
        if (LOG.isTraceEnabled()) LOG.trace("    Writing bytearray.") ;
        return ByteBuffer.wrap(((DataByteArray)pigData).get()) ;
      
      case DataType.MAP: // Pig Map -> Avro Map
        if (LOG.isTraceEnabled()) LOG.trace("   Writing map.") ;
        @SuppressWarnings("unchecked")
        Map<String,Object> pigMap = (Map<String,Object>) pigData ;
        Map<String,Object> goraMap = new HashMap<String, Object>(pigMap.size()) ;

        if (pigField.getSchema() == null) {
            throw new IOException("The map being written does not have schema.") ;
        }
        
        for(Entry<String,Object> pigEntry : pigMap.entrySet()) {
          goraMap.put(pigEntry.getKey(), this.writeField(avroSchema.getValueType(), pigField.getSchema().getFields()[0], pigEntry.getValue())) ;
        }
        return goraMap ;
        
      case DataType.BAG: // Pig Bag -> Avro Array
        if (LOG.isTraceEnabled()) LOG.trace("    Writing bag.") ;
        Array<Object> persistentArray = new Array<Object>((int)((DataBag)pigData).size(),avroSchema) ;
        for (Object pigArrayElement: (DataBag)pigData) {
          if (avroSchema.getElementType().getType() == Type.RECORD) {
            // If element type is record, the mapping Persistent->PigType deletes one nested tuple:
            // We want the map as: map((a1,a2,a3), (b1,b2,b3),...) instead of map(((a1,a2,a3)), ((b1,b2,b3)), ...)
            persistentArray.add(this.writeField(avroSchema.getElementType(), pigField.getSchema().getFields()[0], pigArrayElement)) ;
          } else {
            // Every bag has a tuple as element type. Since this is not a record, that "tuple" container must be ignored
            persistentArray.add(this.writeField(avroSchema.getElementType(), pigField.getSchema().getFields()[0].getSchema().getFields()[0], ((Tuple)pigArrayElement).get(0))) ;
          }
        }
        return persistentArray ;
        
      case DataType.TUPLE: // Pig Tuple -> Avro Record
        if (LOG.isTraceEnabled()) LOG.trace("    Writing tuple.") ;
        try {
          PersistentBase persistentRecord = (PersistentBase) ClassLoadingUtils.loadClass(avroSchema.getFullName()).newInstance();
          
          ResourceFieldSchema[] tupleFieldSchemas = pigField.getSchema().getFields() ;
          
          for (int i=0; i<tupleFieldSchemas.length; i++) {
            persistentRecord.put(tupleFieldSchemas[i].getName(),
                this.writeField(avroSchema.getField(tupleFieldSchemas[i].getName()).schema(),
                                tupleFieldSchemas[i],
                                ((Tuple)pigData).get(i))) ;
          }
          return persistentRecord ;
        } catch (InstantiationException e) {
          throw new IOException(e) ;
        } catch (IllegalAccessException e) {
          throw new IOException(e) ;
        } catch (ClassNotFoundException e) {
          throw new IOException(e) ;
        }
        
      default:
        throw new IOException("Unexpected field " + pigField.getName() +" with Pig type "+ DataType.genTypeToNameMap().get(pigField.getType())) ;
    }
    
  }

  @Override
  public void setStoreFuncUDFContextSignature(String signature) {  
    this.udfcSignature = signature ;
  }

  @Override
  public void cleanupOnFailure(String location, Job job) throws IOException {
    if (dataStore != null){
      dataStore.flush();
      dataStore.close();
    }
  }

  @Override
  public void cleanupOnSuccess(String location, Job job) throws IOException {
    if (dataStore != null){
      dataStore.flush();
      dataStore.close();
    }
  }

}
