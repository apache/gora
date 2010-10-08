
package org.gora.avro.query;

import java.io.EOFException;
import java.io.IOException;

import org.apache.avro.AvroTypeException;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.gora.avro.store.AvroStore;
import org.gora.persistency.Persistent;
import org.gora.query.impl.ResultBase;

/**
 * Adapter to convert DatumReader to Result.
 */
public class AvroResult<K, T extends Persistent> extends ResultBase<K, T> {

  private DatumReader<T> reader;
  private Decoder decoder;
  
  public AvroResult(AvroStore<K,T> dataStore, AvroQuery<K,T> query
      , DatumReader<T> reader, Decoder decoder) {
    super(dataStore, query);
    this.reader = reader;
    this.decoder = decoder;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public float getProgress() throws IOException {
    //TODO: FIXME
    return 0;
  }

  @Override
  public boolean nextInner() throws IOException {
    try {
      persistent = reader.read(persistent, decoder);
      
    } catch (AvroTypeException ex) {
      //TODO: it seems that avro does not respect end-of file and return null
      //gracefully. Report the issue.
      return false;
    } catch (EOFException ex) {
      return false;
    }
    
    return persistent != null;
  }  
}
