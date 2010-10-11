
package org.gora.query.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.gora.persistency.Persistent;
import org.gora.query.Query;

/**
 * Keeps a {@link FileSplit} to represent the partition boundaries.
 * FileSplitPartitionQuery is best used with existing {@link InputFormat}s.
 */
public class FileSplitPartitionQuery<K, T extends Persistent>
  extends PartitionQueryImpl<K,T> {

  private FileSplit split;

  public FileSplitPartitionQuery() {
    super();
  }

  public FileSplitPartitionQuery(Query<K, T> baseQuery, FileSplit split)
    throws IOException {
    super(baseQuery, split.getLocations());
    this.split = split;
  }

  public FileSplit getSplit() {
    return split;
  }

  public long getLength() {
    return split.getLength();
  }

  public long getStart() {
    return split.getStart();
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    split.write(out);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    if(split == null)
      split = new FileSplit(null, 0, 0, null); //change to new FileSplit() once hadoop-core.jar is updated
    split.readFields(in);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof FileSplitPartitionQuery) {
      return super.equals(obj) &&
      this.split.equals(((FileSplitPartitionQuery)obj).split);
    }
    return false;
  }

}
