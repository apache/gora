package org.gora.cassandra.client;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.thrift.KeySlice;
import org.apache.cassandra.thrift.SlicePredicate;

public class RangeSliceGet
implements Callable<Pair<ColumnParent, List<KeySlice>>> {

  private Cassandra.Client client;
  private String keySpace;
  private KeyRange keyRange;
  private ColumnParent parent;
  private SlicePredicate predicate;
  private ConsistencyLevel consistencyLevel;

  public RangeSliceGet(Cassandra.Client client, String keySpace, KeyRange keyRange,
      ColumnParent parent, SlicePredicate predicate,
      ConsistencyLevel consistencyLevel) {
    this.client = client;
    this.keySpace = keySpace;
    this.keyRange = keyRange;
    this.parent = parent;
    this.predicate = predicate;
    this.consistencyLevel = consistencyLevel;
  }

  @Override
  public Pair<ColumnParent, List<KeySlice>> call() throws Exception {
    return new Pair<ColumnParent, List<KeySlice>>(parent,
        client.get_range_slices(keySpace, parent, predicate, keyRange, consistencyLevel));
  }

}
