package org.apache.gora.cassandra.query;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import me.prettyprint.hector.api.beans.HColumn;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraSubColumn extends CassandraColumn {
  public static final Logger LOG = LoggerFactory.getLogger(CassandraSubColumn.class);

  private static final String ENCODING = "UTF-8";
  
  private static CharsetEncoder charsetEncoder = Charset.forName(ENCODING).newEncoder();;


  /**
   * Key-value pair containing the raw data.
   */
  private HColumn<String, String> hColumn;

  public String getName() {
    return hColumn.getName();
  }

  /**
   * Deserialize a String into an typed Object, according to the field schema.
   * @see org.apache.gora.cassandra.query.CassandraColumn#getValue()
   */
  public Object getValue() {
    Field field = getField();
    Schema fieldSchema = field.schema();
    Type type = fieldSchema.getType();
    String valueString = hColumn.getValue();
    Object value = null;
    
    switch (type) {
      case STRING:
        value = new Utf8(valueString);
        break;
      case BYTES:
        // convert string to bytebuffer
        value = getByteBuffer(valueString);
        break;
      case INT:
        value = Integer.parseInt(valueString);
        break;
      case LONG:
        value = Long.parseLong(valueString);
        break;
      case FLOAT:
        value = Float.parseFloat(valueString);
        break;
      case ARRAY:
        // convert string to array
        valueString = valueString.substring(1, valueString.length()-1);
        String[] elements = valueString.split(", ");
        
        Type elementType = fieldSchema.getElementType().getType();
        if (elementType == Schema.Type.STRING) {
          // the array type is String
          GenericArray<String> genericArray = new GenericData.Array<String>(elements.length, Schema.createArray(Schema.create(Schema.Type.STRING)));
          for (String element: elements) {
            genericArray.add(element);
          }
          
          value = genericArray;
        } else {
          LOG.info("Element type not supported: " + elementType);
        }
        break;
      default:
        LOG.info("Type not supported: " + type);
    }
    
    return value;

  }

  public void setValue(HColumn<String, String> hColumn) {
    this.hColumn = hColumn;
  }
  
  public static ByteBuffer getByteBuffer(String valueString) {
    ByteBuffer byteBuffer = null;
    try {
      byteBuffer = charsetEncoder.encode(CharBuffer.wrap(valueString));
    } catch (CharacterCodingException cce) {
      LOG.warn("Unable to encode " + valueString + " into " + ENCODING);
    }
    return byteBuffer;
  }
}
