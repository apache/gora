package org.gora.mapreduce;

import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.hadoop.io.serializer.Deserializer;
import org.gora.avro.PersistentDatumReader;
import org.gora.persistency.Persistent;
import org.gora.util.AvroUtils;

/**
* Hadoop deserializer using {@link PersistentDatumReader}
* with {@link BinaryDecoder}.
*/
public class PersistentDeserializer
   implements Deserializer<Persistent> {

  private BinaryDecoder decoder;
  private Class<? extends Persistent> persistentClass;
  private boolean reuseObjects;
  private PersistentDatumReader<Persistent> datumReader;

  public PersistentDeserializer(Class<? extends Persistent> c, boolean reuseObjects) {
    this.persistentClass = c;
    this.reuseObjects = reuseObjects;
    try {
      Schema schema = AvroUtils.getSchema(persistentClass);
      datumReader = new PersistentDatumReader<Persistent>(schema, true);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void open(InputStream in) throws IOException {
    /* It is very important to use a direct buffer, since Hadoop
     * supplies an input stream that is only valid until the end of one
     * record serialization. Each time deserialize() is called, the IS
     * is advanced to point to the right location, so we should not
     * buffer the whole input stream at once.
     */
    decoder = new DecoderFactory().configureDirectDecoder(true)
      .createBinaryDecoder(in, decoder);
  }

  @Override
  public void close() throws IOException { }

  @Override
  public Persistent deserialize(Persistent persistent) throws IOException {
    return datumReader.read(reuseObjects ? persistent : null, decoder);
  }
}
