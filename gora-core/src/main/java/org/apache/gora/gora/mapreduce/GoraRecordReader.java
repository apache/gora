
package org.gora.mapreduce;

import java.io.IOException;

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.gora.persistency.Persistent;
import org.gora.query.Query;
import org.gora.query.Result;

/**
 * An adapter for Result to Hadoop RecordReader.
 */
public class GoraRecordReader<K, T extends Persistent> 
extends RecordReader<K,T> {

  protected Query<K,T> query;
  protected Result<K,T> result;
  
  public GoraRecordReader(Query<K,T> query) {
    this.query = query;
  }

  public void executeQuery() throws IOException {
    this.result = query.execute();
  }
  
  @Override
  public K getCurrentKey() throws IOException, InterruptedException {
    return result.getKey();
  }

  @Override
  public T getCurrentValue() throws IOException, InterruptedException {
    return result.get();
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    return result.getProgress();
  }

  @Override
  public void initialize(InputSplit split, TaskAttemptContext context)
  throws IOException, InterruptedException { }

  @Override
  public boolean nextKeyValue() throws IOException, InterruptedException {
    if(this.result == null) {
      executeQuery();
    }
    
    return result.next();
  }

  @Override
  public void close() throws IOException {
    result.close();
  }

}
