
package org.gora.persistency.impl;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.avro.util.Utf8;
import org.gora.examples.generated.Employee;
import org.gora.mock.persistency.MockPersistent;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for {@link StateManagerImpl}
 */
public class TestStateManagerImpl {

  private StateManagerImpl stateManager;
  private MockPersistent persistent;
  
  @Before
  public void setUp() {
    this.stateManager = new StateManagerImpl();
    this.persistent = new MockPersistent(stateManager);
  }
  
  @Test
  public void testDirty() {
    Assert.assertFalse(stateManager.isDirty(persistent));
    stateManager.setDirty(persistent);
    Assert.assertTrue(stateManager.isDirty(persistent));
  }
  
  @Test
  public void testDirty2() {
    Assert.assertFalse(stateManager.isDirty(persistent, 0));
    Assert.assertFalse(stateManager.isDirty(persistent, 1));
    stateManager.setDirty(persistent, 0);
    Assert.assertTrue(stateManager.isDirty(persistent, 0));
    Assert.assertFalse(stateManager.isDirty(persistent, 1));
  }
  
  @Test
  public void testClearDirty() {
    Assert.assertFalse(stateManager.isDirty(persistent));
    stateManager.setDirty(persistent, 0);
    stateManager.clearDirty(persistent);
    Assert.assertFalse(this.stateManager.isDirty(persistent));
  }
  
  @Test
  public void testReadable() throws IOException {
    Assert.assertFalse(stateManager.isReadable(persistent, 0));
    Assert.assertFalse(stateManager.isReadable(persistent, 1));
    stateManager.setReadable(persistent, 0);
    Assert.assertTrue(stateManager.isReadable(persistent, 0));
    Assert.assertFalse(stateManager.isReadable(persistent, 1));
  }

  @Test
  public void testReadable2() {
    stateManager = new StateManagerImpl();
    Employee employee = new Employee(stateManager);
    Assert.assertFalse(stateManager.isReadable(employee, 0));
    Assert.assertFalse(stateManager.isReadable(employee, 1));
    employee.setName(new Utf8("foo"));
    Assert.assertTrue(stateManager.isReadable(employee, 0));
    Assert.assertFalse(stateManager.isReadable(employee, 1));
  }
  
  @Test
  public void testClearReadable() {
    stateManager.setReadable(persistent, 0);
    stateManager.clearReadable(persistent);
    Assert.assertFalse(stateManager.isReadable(persistent, 0));
  }
  
  @Test
  public void testIsNew() {
    //newly created objects should be new
    Assert.assertTrue(persistent.isNew());
  }
  
  @Test
  public void testNew() {
    stateManager.setNew(persistent);
    Assert.assertTrue(persistent.isNew());
  }
  
  @Test
  public void testClearNew() {
    stateManager.clearNew(persistent);
    Assert.assertFalse(persistent.isNew());
  }
  
}
