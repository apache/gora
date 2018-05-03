package org.apache.gora.pig.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceSchema.ResourceFieldSchema;
import org.apache.pig.data.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaUtils {

  public static final Logger LOG = LoggerFactory.getLogger(SchemaUtils.class);
  
  /* ============================= CHECK PIG SCHEMA =================================== */
  
  /**
   * Method that performs a check on the Pig Schema that will be used to store. This is, when executing:
   * <pre>
   * STORE relation_data INTO '.' USING org.apache.gora.pig.GoraStorage(
   *                     'java.lang.String',
   *                     'admin.WebPage',
   *                     'baseUrl,status') 
   * </pre>
   * the relation's schema is checked before storing.
   * If there is any error, throws an IOException
   * 
   * @param pigSchema - The Pig Schema to check.
   * @param queryFields - Declared storing fields ("baseUrl" and "status" in the example).
   * @param persistentSchema - The schema of the Persistent entity ("admin.WebPage" in the example).
   * @throws IOException
   */
  public static void checkStoreSchema(ResourceSchema pigSchema, List<String> queryFields, Schema persistentSchema) throws IOException {
    // Expected pig schema: tuple (key, recordfield, recordfield, recordfi...)
    ResourceFieldSchema[] pigFieldsSchemas = pigSchema.getFields();
    String persistentClassName = persistentSchema.getFullName() ;
    
    List<String> pigFieldSchemasNames = new ArrayList<String>(Arrays.asList(pigSchema.fieldNames())) ;
    
    if ( !pigFieldSchemasNames.contains("key") ) {
      LOG.info("Declared Pig fields: " + String.join(",", pigFieldSchemasNames));
      throw new IOException("Expected a field called \"key\", but not found.");
    }

    // All fields are mandatory
    if (pigFieldSchemasNames.containsAll(queryFields)) {
      for (ResourceFieldSchema pigFieldSchema: pigFieldsSchemas) {
        if (queryFields.contains(pigFieldSchema.getName())) {
          Field persistentField = persistentSchema.getField(pigFieldSchema.getName()) ; 
          if (persistentField == null) {
            throw new IOException("Declared field in Pig [" + pigFieldSchema.getName() + "] to store does not exists." + persistentClassName +".") ;
          }
          checkEqualSchema(pigFieldSchema, persistentSchema.getField(pigFieldSchema.getName()).schema()) ;
        }        
      }
    } else {
      throw new IOException("Some fields declared in the constructor (" + String.join(",", queryFields) + ") are missing in the tuples to be saved (" + Arrays.toString(pigSchema.fieldNames()) + ")" ) ;
    }
  }
  
  /**
   * Checks a Pig field schema comparing with avro schema, based on pig field's name (for record fields).
   * 
   * @param pigFieldSchema A Pig field schema
   * @param avroSchema Avro schema related with pig field schema.
   * @throws IOException
   */
  private static void checkEqualSchema(ResourceFieldSchema pigFieldSchema, Schema avroSchema) throws IOException {

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
  private static boolean checkUnionSchema(Schema avroSchema, ResourceFieldSchema pigFieldSchema) throws IOException {
    if (!avroSchema.getType().equals(Type.UNION)) return false ;

    LOG.trace("    checking against UNION");
    for (Schema unionElementSchema: avroSchema.getTypes()) {
      try {
        LOG.trace("    union component {}", unionElementSchema.getType().getName()) ;
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
    throw new IOException("Expected some field defined in '"+avroSchema.getName()+"' for pig schema type '"+DataType.genTypeToNameMap().get(pigFieldSchema.getType()+"'")+"'", new Exception("Union not satisfied")) ;
  }
  
  /* ============================= GENERATE PIG SCHEMA =================================== */
  
  /**  
   * Class created to avoid the StackoverflowException of recursive schemas when creating the
   * schema in #getSchema.
   * It holds the count of how many references to a Record schema has been created in a recursive call
   * to use it only once (not counting the topmost) and the third time will be returned a schemaless tuple (= avro record).
   */
  private static class RecursiveRecordSchema {
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
  
  private static RecursiveRecordSchema recursiveRecordSchema = new RecursiveRecordSchema() ;

  /**
   * Generates a Pig Schema from a Persistent's Avro Schema
   * 
   * @param persistentSchema - The Persistent's Avro Schema to generate a Pig Schema.
   * @param queryFields - Declared query fields in the Storage WITHOUT 'key' field.
   * @param keyClass - Key class of the Persistent.
   * @return
   * @throws IOException
   */
  public static ResourceSchema generatePigSchema(Schema persistentSchema, List<String> queryFields, Class<?> keyClass) throws IOException {
    ResourceFieldSchema[] resourceFieldSchemas = null ;
    
    int numFields = queryFields.size() + 1 ; // We count 'key' field here
    resourceFieldSchemas = new ResourceFieldSchema[numFields] ;
    resourceFieldSchemas[0] = new ResourceFieldSchema().setType(DataType.findType(keyClass)).setName("key") ;
    
    int fieldIndex = 1 ;
    for (String fieldName: queryFields) {
      recursiveRecordSchema.clear() ; // Initialize the recursive schema checker in each field
      Field field = persistentSchema.getField(fieldName) ;
      if (field == null) {
        throw new IOException("Field \"" + fieldName + "\" not found in the entity " + persistentSchema.getFullName() ) ;
      }
      resourceFieldSchemas[fieldIndex++] = avro2ResouceFieldSchema(field.schema()).setName(field.name()) ;      
    }

    ResourceSchema resourceSchema = new ResourceSchema().setFields(resourceFieldSchemas) ;
    return resourceSchema ;
  }
  
  private static ResourceFieldSchema avro2ResouceFieldSchema(Schema schema) throws IOException {

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
          recordFieldSchemas[fieldIndex] = avro2ResouceFieldSchema(schemaField.schema()).setName(schemaField.name()) ;
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
        throw new RuntimeException("Fixed type not implemented") ;
  
      default:
        throw new RuntimeException("Unexpected schema type " + schemaType) ;
    }
    
  }
  
}
