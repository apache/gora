
package org.gora.avro.store;

import java.io.IOException;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.hadoop.fs.Path;
import org.gora.avro.mapreduce.FsInput;
import org.gora.avro.query.DataFileAvroResult;
import org.gora.persistency.Persistent;
import org.gora.query.Query;
import org.gora.query.Result;
import org.gora.query.impl.FileSplitPartitionQuery;
import org.gora.util.OperationNotSupportedException;

/**
 * DataFileAvroStore is file based store which uses Avro's 
 * DataFile{Writer,Reader}'s as a backend. This datastore supports 
 * mapreduce.
 */
public class DataFileAvroStore<K, T extends Persistent> extends AvroStore<K, T> {

  public DataFileAvroStore() {
  }
  
  private DataFileWriter<T> writer;
  
  @Override
  public T get(K key, String[] fields) throws java.io.IOException {
    throw new OperationNotSupportedException(
        "Avro DataFile's does not support indexed retrieval");
  };
  
  @Override
  public void put(K key, T obj) throws java.io.IOException {
    getWriter().append(obj);
  };
  
  private DataFileWriter<T> getWriter() throws IOException {
    if(writer == null) {
      writer = new DataFileWriter<T>(getDatumWriter());
      writer.create(schema, getOrCreateOutputStream());
    }
    return writer;
  }
  
  @Override
  protected Result<K, T> executeQuery(Query<K, T> query) throws IOException {
    return new DataFileAvroResult<K, T>(this, query
        , createReader(createFsInput()));
  }
 
  @Override
  protected Result<K,T> executePartial(FileSplitPartitionQuery<K,T> query) 
    throws IOException {
    FsInput fsInput = createFsInput();
    DataFileReader<T> reader = createReader(fsInput);
    return new DataFileAvroResult<K, T>(this, query, reader, fsInput
        , query.getStart(), query.getLength());
  }
  
  private DataFileReader<T> createReader(FsInput fsInput) throws IOException {
    return new DataFileReader<T>(fsInput, getDatumReader());
  }
  
  private FsInput createFsInput() throws IOException {
    Path path = new Path(getInputPath());
    return new FsInput(path, getConf());
  }
  
  @Override
  public void flush() throws IOException {
    super.flush();
    if(writer != null) {
      writer.flush();
    }
  }
  
  @Override
  public void close() throws IOException {
    if(writer != null)  
      writer.close(); //hadoop 0.20.2 HDFS streams do not allow 
                      //to close twice, so close the writer first 
    writer = null;
    super.close();
  }
}
