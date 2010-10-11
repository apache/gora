
package org.gora.mock.query;

import org.gora.mock.persistency.MockPersistent;
import org.gora.query.impl.QueryBase;
import org.gora.store.DataStore;

public class MockQuery extends QueryBase<String, MockPersistent> {

  public MockQuery() {
    super(null);
  }
  
  public MockQuery(DataStore<String, MockPersistent> dataStore) {
    super(dataStore);
  }

}
