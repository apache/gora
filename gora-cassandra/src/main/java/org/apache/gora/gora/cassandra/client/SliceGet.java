package org.gora.cassandra.client;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.SlicePredicate;

public class SliceGet implements Callable<Pair<ColumnParent, List<ColumnOrSuperColumn>>> {

  private Cassandra.Client client;
  private String keySpace;
  private String key;
  private ColumnParent parent;
  private SlicePredicate predicate;
  private ConsistencyLevel consistencyLevel;

  public SliceGet(Cassandra.Client client, String keySpace, String key,
      ColumnParent parent, SlicePredicate predicate,
      ConsistencyLevel consistencyLevel) {
    this.client = client;
    this.keySpace = keySpace;
    this.key = key;
    this.parent = parent;
    this.predicate = predicate;
    this.consistencyLevel = consistencyLevel;
  }

  @Override
  public Pair<ColumnParent, List<ColumnOrSuperColumn>> call()
  throws Exception {
    return new Pair<ColumnParent, List<ColumnOrSuperColumn>>(parent,
        client.get_slice(keySpace, key, parent, predicate, consistencyLevel));
  }

}
