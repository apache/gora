
package org.gora.persistency;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;

/**
 * An {@link ArrayList} based implementation of Avro {@link GenericArray}.
 */
public class ListGenericArray<T> implements GenericArray<T>
  , Comparable<ListGenericArray<T>> {

  private static final int LIST_DEFAULT_SIZE = 10;
  
  private List<T> list;
  private Schema schema;

  public ListGenericArray(Schema schema, List<T> list) {
    this.schema = schema;
    this.list = list;
  }

  public ListGenericArray(Schema schema) {
    this(LIST_DEFAULT_SIZE, schema);
  }
  
  public ListGenericArray(int size, Schema schema) {
    this.schema = schema;
    this.list = new ArrayList<T>(size);
  }

  @Override
  public void add(T element) {
    list.add(element);
  }

  @Override
  public void clear() {
    list.clear();
  }

  @Override
  public T peek() {
    return null;
  }

  @Override
  public long size() {
    return list.size();
  }

  @Override
  public Iterator<T> iterator() {
    return list.iterator();
  }

  @Override
  public Schema getSchema() {
    return schema;
  }

  @Override
  public int hashCode() {
    return GenericData.get().hashCode(this, schema);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof ListGenericArray)) return false;
    ListGenericArray that = (ListGenericArray)obj;
    if (!schema.equals(that.schema))
      return false;
    return this.compareTo(that) == 0;
  }

  @Override
  public int compareTo(ListGenericArray<T> o) {
    return GenericData.get().compare(this, o, schema);
  }
  
  @Override
  public String toString() {
    return list.toString();
  }
}
