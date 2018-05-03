package org.apache.gora.pig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.gora.mapreduce.GoraMapReduceUtils;
import org.apache.gora.mapreduce.GoraOutputFormat;
import org.apache.gora.mapreduce.GoraRecordWriter;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.pig.mapreduce.PigGoraOutputFormat;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.ClassLoadingUtils;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceSchema.ResourceFieldSchema;
import org.apache.pig.StoreFuncInterface;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.util.UDFContext;
import org.apache.pig.impl.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage to delete rows by key.
 * 
 * 
 * STORE webpages_keys INTO '.' USING org.apache.gora.pig.GoraDeleteStorage(
 *                          'java.lang.String',
 *                          'admin.WebPage',
 *                          'rows') ;
 * 
 * @see GoraStorage
 * 
 * TODO Delete the delete type "values", since will be mandatory to rewrite all the map (Gora 0.4+ does
 *      not implements incremental updates).
 *
 */
public class GoraDeleteStorage implements StoreFuncInterface {

  public static final Logger LOG = LoggerFactory.getLogger(GoraDeleteStorage.class);

  private enum DeleteType { ROWS, VALUES };
  
  /**
   * Key in UDFContext properties that marks config is set (set at backend nodes)  
   */
  private static final String GORA_CONFIG_SET = "gorastorage.config.set" ;
  private static final String GORA_STORE_SCHEMA = "gorastorage.pig.store.schema" ;

  protected Job job;
  protected JobConf localJobConf ; 
  protected String udfcSignature = null ;
  
  protected String keyClassName ;
  protected String persistentClassName ;
  protected Class<?> keyClass;
  protected Class<? extends PersistentBase> persistentClass;
  protected Schema persistentSchema ;
  private   DataStore<?, ? extends PersistentBase> dataStore ;
  protected PigGoraOutputFormat<?,? extends PersistentBase> outputFormat ;
  protected GoraRecordWriter<?,? extends PersistentBase> writer ;
  
  /** What type of delete is configured at constructor: ROWS or VALUES */
  protected DeleteType deleteType = null ;

  /** Map for Pig fields of the tuple being stored: field_name -> (index in tuple, ResourceFieldSchema) */
  private   Map<String, ResourceFieldSchemaWithIndex> writeResourceFieldSchemaMap ;
  
  /**
   * Creates a new GoraDeleteStorage and sets the keyClass from the key class name.
   * @param keyClassName key class. Full name with package (org.apache....)
   * @param persistentClassName persistent class. Full name with package. 
   * @param deleteType 'rows' expect tuples with schema (key:chararray) with the rows to delete <br>
   *                   'values' used to delete elements of map at level-0. expect tuples with schema:
   *                   <pre>
   *                   schema: (key:chararray, outlinks:map[chararray])
   *                   relation: ("key1",["elem1"#"val1","key2"#"val2"]
   *
   *                   schema: (key:chararray, outlinks:tuple(chararray, chararray, ...)
   *                   relation: ("clave1", ("elem1", "elem2"))
   * 
   *                   schema: (key:chararray, outlinks:bag{(chararray)}
   *                   relation: ("clave1", {("elem1", "elem2")}
   *                   </pre>
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   */
  public GoraDeleteStorage(String keyClassName, String persistentClassName, String deleteType) throws InstantiationException, IllegalAccessException {
    super();
    
    this.keyClassName = keyClassName ;
    this.persistentClassName = persistentClassName ;
    try {
      this.keyClass = ClassLoadingUtils.loadClass(keyClassName);
      Class<?> persistentClazz = ClassLoadingUtils.loadClass(persistentClassName);
      this.persistentClass = persistentClazz.asSubclass(PersistentBase.class);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    this.persistentSchema = this.persistentClass.newInstance().getSchema() ;
    this.deleteType = DeleteType.valueOf(deleteType.toUpperCase(Locale.ROOT));
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
      this.dataStore = DataStoreFactory.getDataStore(this.keyClass, this.persistentClass, this.localJobConf) ;
    }
    return this.dataStore ;
  }
  
  /**
   * Returns UDFProperties based on <code>udfcSignature</code>, <code>keyClassName</code> and <code>persistentClassName</code>.
   */
  private Properties getUDFProperties() {
      return UDFContext.getUDFContext().getUDFProperties(this.getClass(), new String[] {this.udfcSignature,this.keyClassName,this.persistentClassName});
  }
  
  private JobConf initializeLocalJobConfig(Job job) {
    Properties udfProps = getUDFProperties();
    Configuration jobConf = job.getConfiguration();
    GoraMapReduceUtils.setIOSerializations(jobConf, true) ;
    JobConf localConf = new JobConf(jobConf); // localConf starts as a copy of jobConf
    if (udfProps.containsKey(GORA_CONFIG_SET)) {
      // Already configured (maybe from frontend to backend)
      for (Entry<Object, Object> entry : udfProps.entrySet()) {
        localConf.set((String) entry.getKey(), (String) entry.getValue());
      }
    } else {
      // Not configured. We load to localConf the configuration and put it in udfProps
      Configuration goraConf = new Configuration();
      for (Entry<String, String> entry : goraConf) {
        // JobConf may have some conf overriding ones in hbase-site.xml
        // So only copy hbase config not in job config to UDFContext
        // Also avoids copying core-default.xml and core-site.xml
        // props in hbaseConf to UDFContext which would be redundant.
        if (localConf.get(entry.getKey()) == null) {
          udfProps.setProperty(entry.getKey(), entry.getValue());
          localConf.set(entry.getKey(), entry.getValue());
        }
      }
      udfProps.setProperty(GORA_CONFIG_SET, "true");
    }
    return localConf;
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
    GoraOutputFormat.setOutput(this.job, this.getDataStore(), false) ;
    this.outputFormat.setConf(this.job.getConfiguration()) ;
    return this.outputFormat ; 
  }

  @Override
  public void setStoreLocation(String location, Job job) throws IOException {
    GoraMapReduceUtils.setIOSerializations(job.getConfiguration(), true) ;
    this.job = job ;
    this.localJobConf = this.initializeLocalJobConfig(job) ;
  }

  @Override
  /**
   * Checks that the Pig schema has at least the field "key" with schema chararray, mandatory for ROWS and VALUES delete types.
   * 
   * Sets UDFContext property GORA_STORE_SCHEMA with the schema to send it to the backend.
   */
  public void checkSchema(ResourceSchema s) throws IOException {
    List<String> pigFieldSchemasNames = new ArrayList<String>(Arrays.asList(s.fieldNames())) ;
    if ( !pigFieldSchemasNames.contains("key") ) {
      throw new IOException("Expected a field called \"key\" but not found.") ;
    }
    
    for (ResourceFieldSchema fieldSchema: s.getFields()) {
      if (fieldSchema.getName().equals("key") &&
          fieldSchema.getType() != DataType.CHARARRAY )
      {
        throw new IOException("Expected field \"key\" with schema chararray, but found schema " + DataType.findTypeName(fieldSchema.getType()) + ".") ;
      }
    }
    
    // Save the schema to UDFContext to use it on backend when writing data
    getUDFProperties().setProperty(GoraDeleteStorage.GORA_STORE_SCHEMA, s.toString()) ;
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void prepareToWrite(RecordWriter writer) throws IOException {
    this.writer = (GoraRecordWriter<?,? extends PersistentBase>) writer ;
    
    // Get the schema of data to write from UDFContext (coming from frontend checkSchema())
    String strSchema = getUDFProperties().getProperty(GoraDeleteStorage.GORA_STORE_SCHEMA) ;
    if (strSchema == null) {
      throw new IOException("Could not find schema in UDF context. Should have been set in checkSchema() in frontend.") ;
    }
    
    // Parse de the schema from string stored in properties object
    ResourceSchema writeResourceSchema = new ResourceSchema(Utils.getSchemaFromString(strSchema)) ;
    this.writeResourceFieldSchemaMap = new HashMap<String, ResourceFieldSchemaWithIndex>() ;
    if (LOG.isTraceEnabled()) LOG.trace(writeResourceSchema.toString()) ;
    
    int index = 0 ;
    for (ResourceFieldSchema fieldSchema : writeResourceSchema.getFields()) {
      this.writeResourceFieldSchemaMap.put(fieldSchema.getName(),
                                           new ResourceFieldSchemaWithIndex(fieldSchema, index++)) ;
    }

  }

  /**
   * If delete type = ROWS => deletes all the rows with the given keys
   * If delete type = VALUES => for all fields with a name related to avro map, deletes the elements
   */
  @SuppressWarnings("unchecked")
  @Override
  public void putNext(Tuple t) throws IOException {
    if (!this.writeResourceFieldSchemaMap.containsKey("key")) {
      throw new IOException("Expected field \"key\" not found in tuple " + t.toString()) ;
    }
    
    String key = (String) t.get(this.writeResourceFieldSchemaMap.get("key").getIndex()) ;
    LOG.trace("delete key: {}", key) ;
    
    switch (this.deleteType) {
      case ROWS :
        ((GoraRecordWriter<Object,PersistentBase>) this.writer).delete(key) ;
        LOG.trace("  deleted row") ;
        break ;
        
      case VALUES :
        
        PersistentBase persistentWithDeletes ;  
        try {
          persistentWithDeletes = this.persistentClass.newInstance() ;
        } catch (InstantiationException e) {
          throw new IOException(e) ;
        } catch (IllegalAccessException e) {
          throw new IOException(e) ;
        }

        // TODO Precalculate what fields are maps as optimization
        
        // Find the fields that are maps
        for (Entry<String,ResourceFieldSchemaWithIndex> entry: this.writeResourceFieldSchemaMap.entrySet()) {
          String fieldName = entry.getKey() ;
          Field avroField = this.persistentSchema.getField(fieldName) ;
          if (avroFieldIsMap(avroField)) {
            try {
              LOG.trace("  deleting in {}", fieldName) ;
              setDeletes(persistentWithDeletes, fieldName, t.get(entry.getValue().getIndex()), entry.getValue().getResourceFieldSchema()) ;
            } catch (Exception e) {
              throw new IOException(e) ;
            }
          }
        }

        try {
          ((GoraRecordWriter<Object,PersistentBase>) this.writer).write(key, (PersistentBase) persistentWithDeletes) ;
        } catch (InterruptedException e) {
          throw new IOException(e) ;
        }

        this.getDataStore().put(key, persistentWithDeletes) ;
        
        break ;
        
      default:
        throw new IOException("Unexpected delete type [" + this.deleteType.toString() + "]. Row with key [" + key + "] ignored") ;
    }
  }

  /**
   * Checks if an avro field is a map or if it is an union with a map 
   * @param field
   * @return
   */
  private boolean avroFieldIsMap(Field field) {
    if (field != null) {
      if (field.schema().getType() == Type.MAP) return true ;
      if (field.schema().getType() == Type.UNION) {
        for (Schema unionFieldSchema: field.schema().getTypes()) {
          if (unionFieldSchema.getType() == Type.MAP) return true ;
        }
      }
    }
    return false ;
  }
  
  /**
   * Given a persistent instance to hold the deletes and the field name of the map to add deletes to, adds
   * all the elements listed in the pig field.
   * The format of the pig field can be:
   * 
   * (key:chararray, outlinks:map[chararray])
   * ("clave1",["elem1"#"val1","elem2"#"val2"])
   *
   * (key:chararray, outlinks:tuple(chararray, chararray, ...)
   * ("clave1", ("elem1", "elem2"))
   *
   * (key:chararray, outlinks:bag{(chararray)}
   * ("clave1", {("elem1"), ("elem2")}
   * 
   * @param persistent Persistent instance that will hold deleted for maps
   * @param fieldName name of the field to add to the deletes
   * @param pigField field with the element's name to delete
   */
  private void setDeletes(PersistentBase persistent, String fieldName, Object pigField, ResourceFieldSchema pigFieldSchema) throws ExecException, Exception {
    @SuppressWarnings("rawtypes")
    
    Map deleteHashMap = (Map) persistent.get(fieldName) ;
    switch (pigFieldSchema.getType()) {
      case DataType.BAG:
        DataBag bag = DataType.toBag(pigField) ;
        for(Tuple t: bag){
          LOG.trace("    {}", t.get(0)) ;
          deleteHashMap.remove(t.get(0)) ;
        }
        persistent.setDirty(fieldName) ;
        break ;
        
      case DataType.MAP:
        Map<String,Object> map = DataType.toMap(pigField) ;
        for (String mapKey: map.keySet()) {
          LOG.trace("    {}", mapKey) ;
          deleteHashMap.remove(mapKey) ;
        }
        persistent.setDirty(fieldName) ;
        break ;
        
      case DataType.TUPLE:
        Tuple tuple = DataType.toTuple(pigField) ;
        for (Object elementKey: tuple) {
          LOG.trace("    {}", elementKey) ;
          deleteHashMap.remove( elementKey) ;
        }
        persistent.setDirty(fieldName) ;
        break ;
        
      default:
        throw new Exception("Unexpected pig field [" + pigField + "] of type " + DataType.genTypeToNameMap().get(pigFieldSchema.getType()) +" when trying to delete map values.") ;
    }
  }
  
  @Override
  public void setStoreFuncUDFContextSignature(String signature) {
    this.udfcSignature = signature ;
  }

  @Override
  public void cleanupOnFailure(String location, Job job) throws IOException {
  }

  @Override
  public void cleanupOnSuccess(String location, Job job) throws IOException {
    if (dataStore != null) dataStore.flush() ;
  }
  
}
