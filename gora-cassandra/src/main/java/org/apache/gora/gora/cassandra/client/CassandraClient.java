package org.gora.cassandra.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.TokenRange;

public interface CassandraClient {

  public void setKeySpace(String keySpace);

  public void setConsistencyLevel(ConsistencyLevel level);

  public Row get(String key, Select select) throws IOException;

  public RowIterable getRange(String startKey, String endKey, int rowCount,
      Select select)
  throws IOException;

  public RowIterable getTokenRange(String startToken, String endToken,
      int rowCount, Select select)
  throws IOException;

  public void mutate(String key, Mutate mutation) throws IOException;

  public Map<String, Map<String, String>> describeKeySpace()
  throws IOException;

  public List<TokenRange> describeRing() throws IOException;

  public List<String> describeSplits(String startToken, String endToken, int size)
  throws IOException;

  public void close();
}
