package org.gora.cassandra.query;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.gora.persistency.Persistent;
import org.gora.query.Query;
import org.gora.query.impl.PartitionQueryImpl;

public class CassandraPartitionQuery<K, T extends Persistent>
extends PartitionQueryImpl<K, T> {

  private String startToken;

  private String endToken;

  private String[] endPoints;

  private int splitSize;

  public CassandraPartitionQuery() {
    this.dataStore = null;
  }

  public CassandraPartitionQuery(Query<K, T> baseQuery, String startToken, String endToken, String[] endPoints,
      int splitSize) {
    super(baseQuery);
    this.startToken = startToken;
    this.endToken = endToken;
    this.endPoints = endPoints;
    this.splitSize = splitSize;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    Text.writeString(out, startToken);
    Text.writeString(out, endToken);
    out.writeInt(endPoints.length);
    for (String endPoint : endPoints) {
      Text.writeString(out, endPoint);
    }
    out.writeInt(splitSize);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    startToken = Text.readString(in);
    endToken = Text.readString(in);
    int size = in.readInt();
    endPoints = new String[size];
    for (int i = 0; i < size; i++) {
      endPoints[i] = Text.readString(in);
    }
    splitSize = in.readInt();
  }

  public String getStartToken() {
    return startToken;
  }

  public String getEndToken() {
    return endToken;
  }

  public String[] getEndPoints() {
    return endPoints;
  }

  public int getSplitSize() {
    return splitSize;
  }
}
