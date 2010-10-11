
package org.gora.mapreduce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.gora.query.PartitionQuery;
import org.gora.util.IOUtils;

/**
 * InputSplit using {@link PartitionQuery}s. 
 */
public class GoraInputSplit extends InputSplit 
  implements Writable, Configurable {

  protected PartitionQuery<?,?> query;
  private Configuration conf;
  
  public GoraInputSplit() {
  }
  
  public GoraInputSplit(Configuration conf, PartitionQuery<?,?> query) {
    setConf(conf);
    this.query = query;
  }
  
  @Override
  public Configuration getConf() {
    return conf;
  }
  
  @Override
  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  @Override
  public long getLength() throws IOException, InterruptedException {
    return 0;
  }

  @Override
  public String[] getLocations() {
    return query.getLocations();
  }

  public PartitionQuery<?, ?> getQuery() {
    return query;
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    try {
      query = (PartitionQuery<?, ?>) IOUtils.deserialize(conf, in, null);
    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void write(DataOutput out) throws IOException {
    IOUtils.serialize(getConf(), out, query);
  }
  
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof GoraInputSplit) {
      return this.query.equals(((GoraInputSplit)obj).query);
    }
    return false;
  }
}