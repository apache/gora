package org.apache.gora.persistency.impl;

import java.util.Collection;
import java.util.Set;

import org.apache.gora.persistency.Dirtyable;

public class DirtySetWrapper<T extends Dirtyable> extends
    DirtyCollectionWrapper<T> implements Set<T> {

  DirtySetWrapper(Collection<T> delegate2, DirtyFlag dirtyFlag) {
    super(delegate2, dirtyFlag);
  }

}
