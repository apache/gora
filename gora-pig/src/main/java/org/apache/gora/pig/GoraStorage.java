package org.apache.gora.pig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Array;
import org.apache.gora.mapreduce.GoraInputFormat;
import org.apache.gora.mapreduce.GoraMapReduceUtils;
import org.apache.gora.mapreduce.GoraOutputFormat;
import org.apache.gora.mapreduce.GoraRecordReader;
import org.apache.gora.mapreduce.GoraRecordWriter;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.pig.mapreduce.GoraInputFormatFactory;
import org.apache.gora.pig.mapreduce.GoraOutputFormatFactory;
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
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.builtin.Utf8StorageConverter;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.util.UDFContext;
import org.apache.pig.impl.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage for Apache Pig.
 *
 * Example usage:
 *
 * <pre>
 * set job.name 'GoraPig test';
 * register gora/*.jar;
 * webpage = LOAD '.' USING org.apache.gora.pig.GoraStorage('java.lang.String','admin.WebPage','baseUrl,status,content') ;
 * dump webpage ;
 * </pre>
 *
 * In the example above, the folder gora must contain:
 * <ul>
 *   <li>All the contents of <code>gora-pig/lib</code></li>
 *   <li>gora-pig jar</li>
 *   <li>A .jar file with the compiled entities (<code>admin.Webpage</code> in the example)</li>
 *   <li>A .jar file with the gora.properties, mapping and configuration files like HBase's hbase-site.xml./li>
 * </ul>
 * and the <code>/lib</code> folder of Apache Pig installation must contain all that files too.
 *
 * Source code:
 *
 * In this class code <code>UDFContext.getUDFContext().isFrontend()</code> returns true when the Storage is a frontend instance.
 * BUT must know that the Storage classes are instanced several times at frontend. In the example above, this is the order of calls
 * and the several different instances:
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
  protected GoraInputFormat<?,? extends PersistentBase> inputFormat ;
  protected GoraRecordReader<?,? extends PersistentBase> reader ;
  protected PigGoraOutputFormat<?,? extends PersistentBase> outputFormat ;
  protected GoraRecordWriter<?,? extends PersistentBase> writer ;
  protected PigSplit split ;
  protected ResourceSchema readResourceSchema ;
  protected ResourceSchema writeResourceSchema ;
  private   Map<String, ResourceFieldSchemaWithIndex> writeResourceFieldSchemaMap ;
  
  /** Fields to load as Query - same as {@link loadSaveFields} but without 'key' */
  protected String[] loadQueryFields ;
  
  /** Setted to 'true' if location is '*'. All fields will be loaded into a tuple when reading,
   * and all tuple fields will be copied to the persistent instance when saving. */
  protected boolean loadSaveAllFields = false ;

  /**
   * Creates a new GoraStorage with implicit "*" fields to load/save.  
   * @param keyClassName
   * @param persistentClassName
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   */
  public GoraStorage(String keyClassName, String persistentClassName) throws InstantiationException, IllegalAccessException {
      this(keyClassName, persistentClassName, "*") ;
  }

  /**
   * Creates a new GoraStorage and set the keyClass from the key class name.
   * @param keyClassName key class. Full name with package (org.apache....)
   * @param persistentClassName persistent class. Full name with package. 
   * @param fields comma separated fields to load/save | '*' for all.
   *   '*' loads all fields from the persistent class to each tuple.
   *   '*' saves all fields of each tuple to persist (not mandatory all fields of the persistent class).
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   */
  public GoraStorage(String keyClassName, String persistentClassName, String csvFields) throws InstantiationException, IllegalAccessException {
    super();
    this.keyClassName = keyClassName ;
    this.persistentClassName = persistentClassName ;
    try {
      this.keyClass = ClassLoadingUtils.loadClass(keyClassName);
      Class<?> persistentClazz = ClassLoadingUtils.loadClass(persistentClassName);
      this.persistentClass = persistentClazz.asSubclass(PersistentBase.class);
    } catch (ClassNotFoundException e) {
    	LOG.error("Error creating instance of key and/or persistent.", e) ;
    	throw new RuntimeException(e);
    }

    this.persistentSchema = this.persistentClass.newInstance().getSchema() ;

    // Populates this.loadQueryFields
    List<String> declaredConstructorFields = new ArrayList<String>() ;
    
    if (csvFields.contains("*")) {
      // Declared fields "*"
      this.setLoadSaveAllFields(true) ;
      for (Field field : this.persistentSchema.getFields()) {
        declaredConstructorFields.add(field.name()) ;
      }
    } else {
      // CSV fields declared in constructor.
      String[] fieldsInConstructor = csvFields.split("\\s*,\\s*") ; // splits "field, field, field, field"
      declaredConstructorFields.addAll(Arrays.asList(fieldsInConstructor)) ;
    }

    this.setLoadQueryFields(declaredConstructorFields.toArray(new String[0])) ;

  }

  /**
   * Returns the internal DataStore for <code>&lt;keyClass,persistentClass&gt;</code>
   * using configuration set in job (from setLocation()).
   * Creates one datastore at first call.
   * @return DataStore for &lt;keyClass,persistentClass&gt;
   * @throws GoraException on DataStore creation error.
   */
  protected DataStore<?, ? extends PersistentBase> getDataStore() throws GoraException {
    if (this.localJobConf == null) {
    	LOG.error("Error integrating gora-pig and Pig. Calling getDataStore(). setLocation()/setStoreLocation() must be called first!") ;
    	throw new GoraException("Calling getDataStore(). setLocation()/setStoreLocation() must be called first!") ;
    }
    if (this.dataStore == null) {
      this.dataStore = DataStoreFactory.getDataStore(this.keyClass, this.persistentClass, this.localJobConf) ;
    }
    return this.dataStore ;
  }
  
  /**
   * Gets the job, initialized the localJobConf (the actual used to create a datastore) and splits from 'location' the fields to load/save
   */
  @Override
  public void setLocation(String location, Job job) throws IOException {
    GoraMapReduceUtils.setIOSerializations(job.getConfiguration(), true) ;
    // All Splits return length==0, but must not be combined (because actually are not ==0)
    job.getConfiguration().setBoolean("pig.noSplitCombination", true);
    this.job = job;
    this.localJobConf = this.initializeLocalJobConfig(job) ;
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
      // Not configured. We load into localConf the configuration and put it in udfProps
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
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public InputFormat getInputFormat() throws IOException {
    this.inputFormat = GoraInputFormatFactory.createInstance(this.keyClass, this.persistentClass);

    Query query = this.getDataStore().newQuery() ;
    if (this.isLoadSaveAllFields() == false) {
      query.setFields(this.getLoadQueryFields()) ;
    }
    GoraInputFormat.setInput(this.job, query, false) ;
    
    inputFormat.setConf(this.job.getConfiguration()) ;
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
          LOG.trace("    Fin de lectura (null)") ;
          return null;
      }
    } catch (Exception e) {
      LOG.error("Error retrieving next key-value.", e) ;
      throw new IOException(e);
    }

    PersistentBase persistentObj;
    Object persistentKey ;

    try {
      persistentKey = this.reader.getCurrentKey() ;
      LOG.trace("    key: {}", persistentKey) ;
      persistentObj = this.reader.getCurrentValue();
    } catch (Exception e) {
      LOG.error("Error reading next key-value.", e) ;
      throw new IOException(e);
    }

    return persistent2Tuple(persistentKey, persistentObj, this.getLoadQueryFields()) ;

  }
  
  /**
   * Creates a pig tuple from a PersistentBase, given some fields in order. It adds "key" field.
   * The resulting tuple has fields (key, field1, field2,...)
   *
   * Internally calls persistentField2PigType(Schema, Object) for each field
   * 
   * @param persistentKey Key of the PersistentBase object
   * @param persistentObj PersistentBase instance
   * @return Tuple with schemafields+1 elements (1<sup>st</sup> element is the row key) 
   * @throws ExecException
   *           On setting tuple field errors
   */
  private static Tuple persistent2Tuple(Object persistentKey, PersistentBase persistentObj, String[] fields) throws ExecException {
    Tuple tuple = TupleFactory.getInstance().newTuple(fields.length + 1);
    Schema avroSchema = persistentObj.getSchema() ;

    tuple.set(0, persistentKey) ;
    
    int fieldIndex = 1 ;
    for (String fieldName : fields) {
      Field field = avroSchema.getField(fieldName) ;
      Schema fieldSchema = field.schema() ;
      Object fieldValue = persistentObj.get(field.pos()) ;
      tuple.set(fieldIndex++, persistentField2PigType(fieldSchema, fieldValue)) ;
    }
    return tuple ;
  }
  
  /**
   * Recursively converts PersistentBase fields to Pig type: Tuple | Bag | String | Long | ...
   * 
   * The mapping is as follows:
   * null         -> null
   * Boolean      -> Boolean
   * Enum         -> Integer
   * ByteBuffer   -> DataByteArray
   * String       -> String
   * Float        -> Float
   * Double       -> Double
   * Integer      -> Integer
   * Long         -> Long
   * Union        -> X
   * Record       -> Tuple
   * Array        -> Bag
   * Map<String,b'> -> HashMap<String,Object>
   * 
   * @param schema Source schema
   * @param data Source data: PersistentBase | String | Long,...
   * @return Pig type: Tuple | Bag | String | Long | ...
   * @throws ExecException 
   */
  @SuppressWarnings("unchecked")
  private static Object persistentField2PigType(Schema schema, Object data) throws ExecException {
    
    Type schemaType = schema.getType();

    switch (schemaType) {
      case NULL:    return null ;
      case BOOLEAN: return (Boolean)data ; 
      case ENUM:    return new Integer(((Enum<?>)data).ordinal()) ;
      case BYTES:   return new DataByteArray(((ByteBuffer)data).array()) ;
      case STRING:  return data.toString() ;
        
      case FLOAT:
      case DOUBLE:
      case INT:
      case LONG:    return data ;

      case UNION:
        int unionIndex = GenericData.get().resolveUnion(schema, data) ;
        Schema unionTypeSchema = schema.getTypes().get(unionIndex) ;
        return persistentField2PigType(unionTypeSchema, data) ;

      case RECORD:
        List<Field> recordFields = schema.getFields() ;
        int numRecordElements = recordFields.size() ;
        
        Tuple recordTuple = TupleFactory.getInstance().newTuple(numRecordElements);
        
        for (int i=0; i<numRecordElements ; i++ ) {
          recordTuple.set(i, persistentField2PigType(recordFields.get(i).schema(), ((PersistentBase)data).get(i))) ;
        }
        return recordTuple ;

      case ARRAY:
        DataBag bag = BagFactory.getInstance().newDefaultBag() ;
        Schema arrValueSchema = schema.getElementType() ;
        for(Object element: (List<?>)data) {
          Object pigElement = persistentField2PigType(arrValueSchema, element) ;
          if (pigElement instanceof Tuple) {
            bag.add((Tuple)pigElement) ;
          } else {
            Tuple arrElemTuple = TupleFactory.getInstance().newTuple(1) ;
            arrElemTuple.set(0, pigElement) ;
            bag.add(arrElemTuple) ;
          }
        }
        return bag ;

      case MAP:
        HashMap<String,Object> map = new HashMap<String,Object>() ;
        for (Entry<CharSequence,?> e : ((Map<CharSequence,?>)data).entrySet()) {
          map.put(e.getKey().toString(), persistentField2PigType(schema.getValueType(), e.getValue())) ;
        }
        return map ;

      case FIXED:
        // TODO: Implement FIXED data type
        LOG.error("FIXED type not implemented") ;
        throw new RuntimeException("Fixed type not implemented") ;

      default:
        LOG.error("Unexpected schema type {}", schemaType) ;
        throw new RuntimeException("Unexpected schema type " + schemaType) ;
    }
  
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
   * Class created to avoid the StackoverflowException of recursive schemas when creating the
   * schema in #getSchema.
   * It holds the count of how many references to a Record schema has been created in a recursive call
   * to use it only once (not counting the topmost) and the third time will be returned a schemaless tuple (= avro record).
   */
  private class RecursiveRecordSchema {
    Map<String,Integer> generatedSchemas = new HashMap<String,Integer>() ;
    public int incSchema(String schemaName) {
      int numReferences = 0 ;
      if (generatedSchemas.containsKey(schemaName)) {
        numReferences = generatedSchemas.get(schemaName) ;
      }
      generatedSchemas.put(schemaName, ++numReferences) ;
      return numReferences ;
    }
    public void decSchema(String schemaName) {
      if (generatedSchemas.containsKey(schemaName)) {
        int numReferences = generatedSchemas.get(schemaName) ;
        if (numReferences > 0) {
          generatedSchemas.put(schemaName, --numReferences) ;
        }
      }
    }
    public void clear() {
      generatedSchemas.clear() ;
    }
    
  } ;
  
  private RecursiveRecordSchema recursiveRecordSchema = new RecursiveRecordSchema() ;
  /**
   * Retrieves the Pig Schema from the declared fields in constructor and the Avro Schema
   * Avro Schema must begin with a record.
   * Pig Schema will be a Tuple (in 1st level) with $0 = "key":rowkey
   */
  @Override
  public ResourceSchema getSchema(String location, Job job) throws IOException {
    // Reuse if already created
    if (this.readResourceSchema != null) return this.readResourceSchema ;
    
    ResourceFieldSchema[] resourceFieldSchemas = null ;
    
    int numFields = this.loadQueryFields.length + 1 ;
    resourceFieldSchemas = new ResourceFieldSchema[numFields] ;
    resourceFieldSchemas[0] = new ResourceFieldSchema().setType(DataType.findType(this.keyClass)).setName("key") ;
    for (int fieldIndex = 1; fieldIndex < numFields ; fieldIndex++) {
      recursiveRecordSchema.clear() ; // Initialize the recursive schema checker in each field
      Field field = this.persistentSchema.getField(this.loadQueryFields[fieldIndex-1]) ;
      if (field == null) {
        LOG.error("Field \"" + this.loadQueryFields[fieldIndex-1] + "\" not found in the entity " + this.persistentClassName ) ;
        throw new IOException("Field \"" + this.loadQueryFields[fieldIndex-1] + "\" not found in the entity " + this.persistentClassName ) ;
      }
      resourceFieldSchemas[fieldIndex] = this.avro2ResouceFieldSchema(field.schema()).setName(field.name()) ;
    }

    ResourceSchema resourceSchema = new ResourceSchema().setFields(resourceFieldSchemas) ;
    // Save Pig schema inside the instance
    this.readResourceSchema = resourceSchema ;
    return this.readResourceSchema ;
  }

  private ResourceFieldSchema avro2ResouceFieldSchema(Schema schema) throws IOException {

    Type schemaType = schema.getType();

    switch (schemaType) {
      case NULL:    return new ResourceFieldSchema().setType(DataType.NULL) ;
      case BOOLEAN: return new ResourceFieldSchema().setType(DataType.BOOLEAN) ; 
      case ENUM:    return new ResourceFieldSchema().setType(DataType.INTEGER) ;
      case BYTES:   return new ResourceFieldSchema().setType(DataType.BYTEARRAY);
      case STRING:  return new ResourceFieldSchema().setType(DataType.CHARARRAY) ;
      case FLOAT:   return new ResourceFieldSchema().setType(DataType.FLOAT) ;
      case DOUBLE:  return new ResourceFieldSchema().setType(DataType.DOUBLE) ;
      case INT:     return new ResourceFieldSchema().setType(DataType.INTEGER) ;
      case LONG:    return new ResourceFieldSchema().setType(DataType.LONG) ;
  
      case UNION:
        // Returns the first not-null type
        if (schema.getTypes().size() != 2) {
          LOG.warn("Field UNION {} must be ['null','othertype']. Maybe wrong definition?") ;
        }
        for (Schema s: schema.getTypes()) {
          if (s.getType() != Type.NULL) return avro2ResouceFieldSchema(s) ;
        }
        LOG.error("Union with only ['null']?") ;
        throw new RuntimeException("Union with only ['null']?") ;
  
      case RECORD:
        // A record in Gora is a Tuple in Pig
        if (recursiveRecordSchema.incSchema(schema.getName()) > 1) {
          // Recursivity detected (and we are 2 levels bellow desired)
          recursiveRecordSchema.decSchema(schema.getName()) ; // So we can put the esquema of bother leafs
          // Return a tuple schema with no fields
          return new ResourceFieldSchema().setType(DataType.TUPLE) ;
        }

        int numRecordFields = schema.getFields().size() ;
        Iterator<Field> recordFields = schema.getFields().iterator();
        ResourceFieldSchema returnRecordResourceFieldSchema = new ResourceFieldSchema().setType(DataType.TUPLE) ;

        ResourceFieldSchema[] recordFieldSchemas = new ResourceFieldSchema[numRecordFields] ;
        for (int fieldIndex = 0; recordFields.hasNext(); fieldIndex++) {
          Field schemaField = recordFields.next();
          recordFieldSchemas[fieldIndex] = this.avro2ResouceFieldSchema(schemaField.schema()).setName(schemaField.name()) ;
        }
        
        returnRecordResourceFieldSchema.setSchema(new ResourceSchema().setFields(recordFieldSchemas)) ;

        return returnRecordResourceFieldSchema ;
          
      case ARRAY:
        // An array in Gora is a Bag in Pig
        // Maybe should be a Map with string(numeric) index to ensure order, but Avro and Pig data model are different :\
        ResourceFieldSchema returnArrayResourceFieldSchema = new ResourceFieldSchema().setType(DataType.BAG) ;
        Schema arrayElementType = schema.getElementType() ;
        
        returnArrayResourceFieldSchema.setSchema(
            new ResourceSchema().setFields(
                new ResourceFieldSchema[]{
                    new ResourceFieldSchema().setType(DataType.TUPLE).setName("t").setSchema(
                        new ResourceSchema().setFields(
                            new ResourceFieldSchema[]{
                                avro2ResouceFieldSchema(arrayElementType)
                            }
                        )
                    )
                }
            )
        ) ;

        return returnArrayResourceFieldSchema ;
  
      case MAP:
        // A map in Gora is a Map in Pig, but in pig is only chararray=>something
        ResourceFieldSchema returnMapResourceFieldSchema = new ResourceFieldSchema().setType(DataType.MAP) ;
        Schema mapValueType = schema.getValueType();
        
        returnMapResourceFieldSchema.setSchema(
            new ResourceSchema().setFields(
                new ResourceFieldSchema[]{
                    avro2ResouceFieldSchema(mapValueType)
                }
            )
        ) ;
        
        return returnMapResourceFieldSchema ;
  
      case FIXED:
        // TODO Implement FIXED data type
        LOG.error("FIXED type not implemented") ;
        throw new RuntimeException("Fixed type not implemented") ;
  
      default:
        LOG.error("Unexpected schema type {}", schemaType) ;
        throw new RuntimeException("Unexpected schema type " + schemaType) ;
    }
    
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
    try {
      this.outputFormat = GoraOutputFormatFactory.createInstance(PigGoraOutputFormat.class, this.keyClass, this.persistentClass);
    } catch (Exception e) {
      LOG.error("Error creating PigGoraOutputFormat", e) ;
      throw new IOException(e) ;
    }
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
   * Checks the pig schema using names, using the first element of the tuple as key (fieldname = 'key').
   * (key:key, name:recordfield, name:recordfield, name:recordfi...)
   * 
   * Sets UDFContext property GORA_STORE_SCHEMA with the schema to send it to the backend.
   * 
   * Not present names for recordfields will be treated as null .
   */
  public void checkSchema(ResourceSchema s) throws IOException {
    // Expected pig schema: tuple (key, recordfield, recordfield, recordfi...)
    ResourceFieldSchema[] pigFieldSchemas = s.getFields();
    
    List<String> pigFieldSchemasNames = new ArrayList<String>(Arrays.asList(s.fieldNames())) ;
    
    if ( !pigFieldSchemasNames.contains("key") ) {
      LOG.error("Error when checking schema. Expected a field called \"key\", but not found.") ;
      throw new IOException("Expected a field called \"key\", but not found.") ;
    }

    // All fields are mandatory
    
    List<String> mandatoryFieldNames = new ArrayList<String>(Arrays.asList(this.loadQueryFields)) ;
    if (pigFieldSchemasNames.containsAll(mandatoryFieldNames)) {
      for (ResourceFieldSchema pigFieldSchema: pigFieldSchemas) {
        LOG.trace("  Pig field {}", pigFieldSchema.getName()) ;
        if (mandatoryFieldNames.contains(pigFieldSchema.getName())) {
          Field persistentField = this.persistentSchema.getField(pigFieldSchema.getName()) ; 
          if (persistentField == null) {
            LOG.error("Declared field in Pig [" + pigFieldSchema.getName() + "] to store does not exists in " + this.persistentClassName +".") ;
            throw new IOException("Declared field in Pig [" + pigFieldSchema.getName() + "] to store does not exists in " + this.persistentClassName +".") ;
          }
          checkEqualSchema(pigFieldSchema, this.persistentSchema.getField(pigFieldSchema.getName()).schema()) ;
        }        
      }
    } else {
      LOG.error("Some fields declared in the constructor ("
                            + Arrays.toString(this.loadQueryFields)
                            + ") are missing in the tuples to be saved ("
                            + Arrays.toString(s.fieldNames()) + ")" ) ;
      throw new IOException("Some fields declared in the constructor ("
                            + Arrays.toString(this.loadQueryFields)
                            + ") are missing in the tuples to be saved ("
                            + Arrays.toString(s.fieldNames()) + ")" ) ;
    }
    
    // Save the schema to UDFContext to use it on backend when writing data
    getUDFProperties().setProperty(GoraStorage.GORA_STORE_SCHEMA, s.toString()) ;
  }
  
  /**
   * Checks a Pig field schema comparing with avro schema, based on pig field's name (for record fields).
   * 
   * @param pigFieldSchema A Pig field schema
   * @param avroSchema Avro schema related with pig field schema.
   * @throws IOException
   */
  private void checkEqualSchema(ResourceFieldSchema pigFieldSchema, Schema avroSchema) throws IOException {

    byte pigType  = pigFieldSchema.getType() ;
    String fieldName = pigFieldSchema.getName() ;

    Type avroType = avroSchema.getType() ;

    // Switch that checks if avro type matches pig type, or if avro is union and some nested type matches pig type.
    switch (pigType) {
      case DataType.BAG: // Avro Array
        LOG.trace("    Bag") ;
        if (!avroType.equals(Type.ARRAY) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig BAG with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        checkEqualSchema(pigFieldSchema.getSchema().getFields()[0].getSchema().getFields()[0], avroSchema.getElementType()) ;
        break ;
      case DataType.BOOLEAN:
        LOG.trace("    Boolean") ;
        if (!avroType.equals(Type.BOOLEAN) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig BOOLEAN with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        break ;
      case DataType.BYTEARRAY:
        LOG.trace("    Bytearray") ;
        if (!avroType.equals(Type.BYTES) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig BYTEARRAY with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        break ;
      case DataType.CHARARRAY: // String
        LOG.trace("    Chararray") ;
        if (!avroType.equals(Type.STRING) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig CHARARRAY with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        break;
      case DataType.DOUBLE:
        LOG.trace("    Double") ;
        if (!avroType.equals(Type.DOUBLE) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig DOUBLE with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        break ;
      case DataType.FLOAT:
        LOG.trace("    Float") ;
        if (!avroType.equals(Type.FLOAT) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig FLOAT with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        break ;
      case DataType.INTEGER: // Int or Enum
        LOG.trace("    Integer") ;
        if (!avroType.equals(Type.INT) && !avroType.equals(Type.ENUM) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig INTEGER with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        break ;
      case DataType.LONG:
        LOG.trace("    Long") ;
        if (!avroType.equals(Type.LONG) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig LONG with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        break ;
      case DataType.MAP: // Avro Map
        LOG.trace("    Map") ;
        if (!avroType.equals(Type.MAP) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig MAP with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        break ;
      case DataType.NULL: // Avro nullable??
        LOG.trace("    Type Null") ;
        if(!avroType.equals(Type.NULL) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig NULL with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        break ;
      case DataType.TUPLE: // Avro Record
        LOG.trace("    Tuple") ;
        if (!avroType.equals(Type.RECORD) && !checkUnionSchema(avroSchema, pigFieldSchema))
          throw new IOException("Can not convert field [" + fieldName + "] from Pig TUPLE(record) with schema " + pigFieldSchema.getSchema() + " to avro " + avroType.name()) ;
        break ;
      default:
        throw new IOException("Unexpected Pig schema type " + DataType.genTypeToNameMap().get(pigType) + " for avro schema field " + avroSchema.getName() +": " + avroType.name()) ;
    }
    
  }

  /**
   * Checks and tries to match a pig field schema with an avro union schema. 
   * @param avroSchema Schema with
   * @param pigFieldSchema
   * @return true: if a match is found
   *         false: if avro schema is not UNION
   * @throws IOException(message, Exception()) if avro schema is UNION but not match is found for pig field schema.
   */
  private boolean checkUnionSchema(Schema avroSchema, ResourceFieldSchema pigFieldSchema) throws IOException {
    if (!avroSchema.getType().equals(Type.UNION)) return false ;

    for (Schema unionElementSchema: avroSchema.getTypes()) {
      try {
        LOG.trace("    Checking pig schema '{}' with avro union '[{},...]'", DataType.findTypeName(pigFieldSchema.getType()), unionElementSchema.getType().getName()) ;
        checkEqualSchema(pigFieldSchema, unionElementSchema) ;
        return true ;
      }catch (IOException e){
        // Exception from inner union, rethrow
        if (e.getCause() != null) {
          throw e ;
        }
        // else ignore
      }
    }
    // throws IOException(message,Exception()) to mark nested union exception.
    LOG.error("Expected some field defined in '"+avroSchema.getName()+"' for pig schema type '"+DataType.genTypeToNameMap().get(pigFieldSchema.getType()+"'")) ;
    throw new IOException("Expected some field defined in '"+avroSchema.getName()+"' for pig schema type '"+DataType.genTypeToNameMap().get(pigFieldSchema.getType()+"'")+"'", new Exception("Union not satisfied")) ;
  }
  
  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void prepareToWrite(RecordWriter writer) throws IOException {
    this.writer = (GoraRecordWriter<?,? extends PersistentBase>) writer ;
    
    // Get the schema of data to write from UDFContext (coming from frontend checkSchema())
    String strSchema = getUDFProperties().getProperty(GoraStorage.GORA_STORE_SCHEMA) ;
    if (strSchema == null) {
      LOG.error("Could not find schema in UDF context. Should have been set in checkSchema() in frontend.") ;
      throw new IOException("Could not find schema in UDF context. Should have been set in checkSchema() in frontend.") ;
    }

    LOG.info("Schema read from frontend to write: " + strSchema) ;
    // Parse de the schema from string stored in properties object
    this.writeResourceSchema = new ResourceSchema(Utils.getSchemaFromString(strSchema)) ;
    if (LOG.isTraceEnabled()) LOG.trace(this.writeResourceSchema.toString()) ;
    this.writeResourceFieldSchemaMap = new HashMap<String, ResourceFieldSchemaWithIndex>() ;
    int index = 0 ;
    for (ResourceFieldSchema fieldSchema : this.writeResourceSchema.getFields()) {
      this.writeResourceFieldSchemaMap.put(fieldSchema.getName(),
                                           new ResourceFieldSchemaWithIndex(fieldSchema, index++)) ;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void putNext(Tuple t) throws IOException {

    PersistentBase persistentObj = this.dataStore.newPersistent() ;

    if (LOG.isTraceEnabled()) LOG.trace("key: {}", t.get(0)) ;
    for (String fieldName : this.loadQueryFields) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("  Put fieldname: {}", fieldName) ;
        LOG.trace("      resourcefield schema: {}", this.writeResourceFieldSchemaMap.get(fieldName).getResourceFieldSchema()) ;
        LOG.trace("      value: {} - {}",this.writeResourceFieldSchemaMap.get(fieldName).getIndex(), t.get(this.writeResourceFieldSchemaMap.get(fieldName).getIndex())) ;
      }

      assert persistentObj != null ;
            
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
      
      persistentObj.put(fieldName,
                        this.writeField(persistentField.schema(),
                                        pigFieldSchema,
                                        t.get(writeResourceFieldSchemaWithIndex.getIndex()))) ;
    }

    try {
      ((GoraRecordWriter<Object,PersistentBase>) this.writer).write(t.get(0), (PersistentBase) persistentObj) ;
    } catch (InterruptedException e) {
      LOG.error("Error writing the tuple.", e) ;
      throw new IOException(e) ;
    }
  }

  /**
   * Converts one pig field data to PersistentBase Data.
   * 
   * @param avroSchema PersistentBase schema used to create new nested records
   * @param field Pig schema of the field being converted
   * @param pigData Pig data relative to the schema
   * @return PersistentBase data
   * @throws IOException
   */
  private Object writeField(Schema avroSchema, ResourceFieldSchema field, Object pigData) throws IOException {

    // If data is null, return null (check if avro schema is right)
    if (pigData == null) {
      if (avroSchema.getType() != Type.UNION && avroSchema.getType() != Type.NULL) {
        throw new IOException("Tuple field " + field.getName() + " is null, but Avro Schema is not union nor null") ;
      } else {
        return null ;
      }
    }
    
    // If avroSchema is union, it will not be the null field, so select the proper one
    if (avroSchema.getType() == Type.UNION) {
      //TODO Resolve the proper schema      
      avroSchema = avroSchema.getTypes().get(1) ;
    }
    
    switch(field.getType()) {
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

        if (field.getSchema() == null) {
            throw new IOException("The map being written does not have schema.") ;
        }
        
        for(Entry<String,Object> pigEntry : pigMap.entrySet()) {
          goraMap.put(pigEntry.getKey(), this.writeField(avroSchema.getValueType(), field.getSchema().getFields()[0], pigEntry.getValue())) ;
        }
        return goraMap ;
        
      case DataType.BAG: // Pig Bag -> Avro Array
        if (LOG.isTraceEnabled()) LOG.trace("    Writing bag.") ;
        Array<Object> persistentArray = new Array<Object>((int)((DataBag)pigData).size(),avroSchema) ;
        for (Object pigArrayElement: (DataBag)pigData) {
          if (avroSchema.getElementType().getType() == Type.RECORD) {
            // If element type is record, the mapping Persistent->PigType deletes one nested tuple:
            // We want the map as: map((a1,a2,a3), (b1,b2,b3),...) instead of map(((a1,a2,a3)), ((b1,b2,b3)), ...)
            persistentArray.add(this.writeField(avroSchema.getElementType(), field.getSchema().getFields()[0], pigArrayElement)) ;
          } else {
            // Every bag has a tuple as element type. Since this is not a record, that "tuple" container must be ignored
            persistentArray.add(this.writeField(avroSchema.getElementType(), field.getSchema().getFields()[0].getSchema().getFields()[0], ((Tuple)pigArrayElement).get(0))) ;
          }
        }
        return persistentArray ;
        
      case DataType.TUPLE: // Pig Tuple -> Avro Record
        if (LOG.isTraceEnabled()) LOG.trace("    Writing tuple.") ;
        try {
          PersistentBase persistentRecord = (PersistentBase) ClassLoadingUtils.loadClass(avroSchema.getFullName()).newInstance();
          
          ResourceFieldSchema[] tupleFieldSchemas = field.getSchema().getFields() ;
          
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
        throw new IOException("Unexpected field " + field.getName() +" with Pig type "+ DataType.genTypeToNameMap().get(field.getType())) ;
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

  public boolean isLoadSaveAllFields() {
    return loadSaveAllFields;
  }

  public void setLoadSaveAllFields(boolean loadSaveAllFields) {
    this.loadSaveAllFields = loadSaveAllFields;
  }

  public String[] getLoadQueryFields() {
    return loadQueryFields;
  }

  public void setLoadQueryFields(String[] loadQueryFields) {
    this.loadQueryFields = loadQueryFields;
  }

}
