package org.apache.gora.pig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.gora.mapreduce.GoraRecordWriter;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceSchema.ResourceFieldSchema;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage to delete rows by key.
 * 
 * Given a relation with schema (key:chararray) rows, the following will delete
 * all rows with that keys:
 *                          
 * <pre>
 * STORE webpages INTO '.' USING org.apache.gora.pig.GoraDeleteStorage('{
 *       "persistentClass": "admin.WebPage",
 *       "goraProperties": "gora.datastore.default=org.apache.gora.hbase.store.HBaseStore\\ngora.datastore.autocreateschema=true\\ngora.hbasestore.scanner.caching=4",
 *       "mapping": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n<gora-odm>\\n<table name=\\"webpage\\">\\n<family name=\\"f\\" maxVersions=\\"1\\"/>\\n</table>\\n<class table=\\"webpage\\" keyClass=\\"java.lang.String\\" name=\\"admin.WebPage\\">\\n<field name=\\"baseUrl\\" family=\\"f\\" qualifier=\\"bas\\"/>\\n<field name=\\"status\\" family=\\"f\\" qualifier=\\"st\\"/>\\n<field name=\\"content\\" family=\\"f\\" qualifier=\\"cnt\\"/>\\n</class>\\n</gora-odm>",
 *       "configuration": {
 *           "hbase.zookeeper.quorum": "hdp4,hdp1,hdp3",
 *           "zookeeper.znode.parent": "/hbase-unsecure"
 *       }
 * }') ;
 * </pre>
 * 
 * @see GoraStorage
 * 
 */
public class GoraDeleteStorage extends GoraStorage {

  public static final Logger LOG = LoggerFactory.getLogger(GoraDeleteStorage.class);
  
  public GoraDeleteStorage(String storageConfigurationString) throws InstantiationException, IllegalAccessException, JsonParseException, JsonMappingException, IOException {
    super(storageConfigurationString);
  }

  
  @Override
  /**
   * Checks that the Pig schema has at least the field "key" with schema chararray.
   * 
   * Sets UDFContext property GORA_STORE_SCHEMA with the schema to send it to the backend.
   */
  public void checkSchema(ResourceSchema pigSchema) throws IOException {

    List<String> pigFieldSchemasNames = new ArrayList<String>(Arrays.asList(pigSchema.fieldNames())) ;
    if ( !pigFieldSchemasNames.contains("key") ) {
      throw new IOException("Expected a field called \"key\" but not found.") ;
    }
    
    for (ResourceFieldSchema fieldSchema: pigSchema.getFields()) {
      if (fieldSchema.getName().equals("key") &&
          fieldSchema.getType() != DataType.CHARARRAY )
      {
        throw new IOException("Expected field \"key\" with schema chararray, but found schema " + DataType.findTypeName(fieldSchema.getType()) + ".") ;
      }
    }
    
    // Save the schema to UDFContext to use it on backend when writing data
    this.getUDFProperties().setProperty(GoraStorage.GORA_STORE_PIG_SCHEMA, pigSchema.toString()) ;
  }
  
  /**
   * If delete type = ROWS => deletes all the rows with the given keys
   * If delete type = VALUES => for all fields with a name related to avro map, deletes the elements
   */
  @SuppressWarnings("unchecked")
  @Override
  public void putNext(Tuple pigTuple) throws IOException {
    String key = (String) pigTuple.get(this.pigFieldKeyIndex) ;
    ((GoraRecordWriter<Object,PersistentBase>) this.writer).delete(key) ;
  }
  
  /**
   * Disabled method since it is used to load data with LOAD command
   */
  @Override
  public ResourceSchema getSchema(String location, Job job) throws IOException {
    throw new IOException(this.getClass().getName() + " can not be used to load data.") ;
  }
  
  @Override
  public void setLocation(String location, Job job) throws IOException {
    throw new IOException(this.getClass().getName() + " can not be used to load data.") ;
  }
  
  @SuppressWarnings("rawtypes")
  @Override
  public void prepareToRead(RecordReader reader, PigSplit split) throws IOException {
    throw new IOException(this.getClass().getName() + " can not be used to load data.") ;
  }
  
  @Override
  public void setUDFContextSignature(String signature) {
    throw new RuntimeException(this.getClass().getName() + " can not be used to load data.") ;
  }
}
