package org.apache.gora.pig.util;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentUtils {

  public static final Logger LOG = LoggerFactory.getLogger(PersistentUtils.class);
  
  /**
   * Creates a pig tuple from a PersistentBase, given some fields in order. It adds "key" field.
   * The resulting tuple has fields (key, field1, field2,...)
   *
   * Internally calls persistentField2PigType(Schema, Object) for each field
   * 
   * @param persistentKey Key of the PersistentBase object
   * @param persistentObj PersistentBase instance
   * @param queryFields - Declared query fields in the Storage WITHOUT 'key' field.
   * @return Tuple with schemafields+1 elements (1<sup>st</sup> element is the row key) 
   * @throws ExecException
   *           On setting tuple field errors
   */
  public static Tuple persistent2Tuple(Object persistentKey, PersistentBase persistentObj, List<String> queryFields) throws ExecException {
    Tuple tuple = TupleFactory.getInstance().newTuple(queryFields.size() + 1);
    Schema avroSchema = persistentObj.getSchema() ;

    tuple.set(0, persistentKey) ;
    
    int fieldIndex = 1 ;
    for (String fieldName : queryFields) {
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

  
}
