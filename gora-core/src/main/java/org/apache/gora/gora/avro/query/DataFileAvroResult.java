
package org.gora.avro.query;

import java.io.IOException;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableInput;
import org.gora.persistency.Persistent;
import org.gora.query.Query;
import org.gora.query.impl.ResultBase;
import org.gora.store.DataStore;

/**
 * An Avro {@link DataFileReader} backed Result.
 */
public class DataFileAvroResult<K, T extends Persistent> extends ResultBase<K, T> {

  private SeekableInput in;
  private DataFileReader<T> reader;
  private long start;
  private long end;
  
  public DataFileAvroResult(DataStore<K, T> dataStore, Query<K, T> query
      , DataFileReader<T> reader) 
  throws IOException {
    this(dataStore, query, reader, null, 0, 0);
  }
  
  public DataFileAvroResult(DataStore<K, T> dataStore, Query<K, T> query
      , DataFileReader<T> reader, SeekableInput in, long start, long length) 
  throws IOException {
    super(dataStore, query);
    this.reader = reader;
    this.start = start;
    this.end = start + length;
    this.in = in;
    if(start > 0) {
      reader.sync(start);
    }
  }

  @Override
  public void close() throws IOException {
    if(reader != null)
      reader.close();
    reader = null;
  }

  @Override
  public float getProgress() throws IOException {
    if (end == start) {
      return 0.0f;
    } else {
      return Math.min(1.0f, (in.tell() - start) / (float)(end - start));
    }
  }

  @Override
  public boolean nextInner() throws IOException {
    if (!reader.hasNext())
      return false;
    if(end > 0 && reader.pastSync(end))
      return false;
    persistent = reader.next(persistent);
    return true;
  }
  
}
