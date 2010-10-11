package org.gora.cassandra.client;

/*package*/ class Pair<V1, V2> {
  V1 v1;
  V2 v2;

  Pair(V1 v1, V2 v2) {
    this.v1 = v1;
    this.v2 = v2;
  }

  V1 getFirst() { return v1; }

  V2 getSecond() { return v2; }
}
