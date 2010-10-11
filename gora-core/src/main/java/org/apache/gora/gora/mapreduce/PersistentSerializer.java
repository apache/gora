package org.gora.mapreduce;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.avro.io.BinaryEncoder;
import org.apache.hadoop.io.serializer.Serializer;
import org.gora.avro.PersistentDatumWriter;
import org.gora.persistency.Persistent;

/**
 * Hadoop serializer using {@link PersistentDatumWriter} 
 * with {@link BinaryEncoder}. 
 */
public class PersistentSerializer implements Serializer<Persistent> {

  private PersistentDatumWriter<Persistent> datumWriter;
  private BinaryEncoder encoder;  
  
  public PersistentSerializer() {
    this.datumWriter = new PersistentDatumWriter<Persistent>();
  }
  
  @Override
  public void close() throws IOException {
    encoder.flush();
  }

  @Override
  public void open(OutputStream out) throws IOException {
    encoder = new BinaryEncoder(out);
  }

  @Override
  public void serialize(Persistent persistent) throws IOException {   
    datumWriter.setSchema(persistent.getSchema());
    datumWriter.setPersistent(persistent);
        
    datumWriter.write(persistent, encoder);
  }
}
