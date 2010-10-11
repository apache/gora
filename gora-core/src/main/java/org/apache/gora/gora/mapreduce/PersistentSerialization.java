package org.gora.mapreduce;

import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.Serialization;
import org.apache.hadoop.io.serializer.Serializer;
import org.gora.persistency.Persistent;

public class PersistentSerialization
implements Serialization<Persistent> {

  @Override
  public boolean accept(Class<?> c) {
    return Persistent.class.isAssignableFrom(c);
  }

  @Override
  public Deserializer<Persistent> getDeserializer(Class<Persistent> c) {
    return new PersistentDeserializer(c, true);
  }

  @Override
  public Serializer<Persistent> getSerializer(Class<Persistent> c) {
    return new PersistentSerializer();
  }
}
