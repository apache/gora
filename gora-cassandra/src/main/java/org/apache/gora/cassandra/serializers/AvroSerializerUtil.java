package org.apache.gora.cassandra.serializers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

public class AvroSerializerUtil {

  /**
   * Threadlocals maintaining reusable binary decoders and encoders.
   */
  private static ThreadLocal<ByteArrayOutputStream> outputStream =
      new ThreadLocal<>();
  
  public static final ThreadLocal<BinaryEncoder> encoders =
      new ThreadLocal<>();

  public static final ThreadLocal<BinaryDecoder> decoders =
      new ThreadLocal<>();
  
  /**
   * Create a {@link java.util.concurrent.ConcurrentHashMap} for the 
   * datum readers and writers. 
   * This is necessary because they are not thread safe, at least not before 
   * Avro 1.4.0 (See AVRO-650).
   * When they are thread safe, it is possible to maintain a single reader and
   * writer pair for every schema, instead of one for every thread.
   * @see <a href="https://issues.apache.org/jira/browse/AVRO-650">AVRO-650</a>
   */
  public static final ConcurrentHashMap<String, SpecificDatumWriter<?>> writerMap = 
      new ConcurrentHashMap<>();
  
  public static final ConcurrentHashMap<String, SpecificDatumReader<?>> readerMap = 
      new ConcurrentHashMap<>();
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T> byte[] serializer(T value, Schema schema) throws IOException{
    SpecificDatumWriter writer = writerMap.get(schema.getFullName());
    if (writer == null) {
      writer = new SpecificDatumWriter(schema);// ignore dirty bits
      writerMap.put(schema.getFullName(),writer);
    }
    
    BinaryEncoder encoderFromCache = encoders.get();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    outputStream.set(bos);
    BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(bos, null);
    if (encoderFromCache == null) {
      encoders.set(encoder);
    }
    
    //reset the buffers
    ByteArrayOutputStream os = outputStream.get();
    os.reset();
    
    writer.write(value, encoder);
    encoder.flush();
    byte[] byteValue = os.toByteArray();
    return byteValue;
  }
  
  public static Object deserializer(Object value, Schema schema) throws IOException{
    String schemaId = schema.getFullName();      
    
    SpecificDatumReader<?> reader = readerMap.get(schemaId);
    if (reader == null) {
      reader = new SpecificDatumReader(schema);// ignore dirty bits
      SpecificDatumReader localReader=null;
      if((localReader=readerMap.putIfAbsent(schemaId, reader))!=null) {
        reader = localReader;
      }
    }
    
    // initialize a decoder, possibly reusing previous one
    BinaryDecoder decoderFromCache = decoders.get();
    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder((byte[])value, null);
    // put in threadlocal cache if the initial get was empty
    if (decoderFromCache==null) {
      decoders.set(decoder);
    }

    Object result = reader.read(null, decoder);
    return result;

  }
}
