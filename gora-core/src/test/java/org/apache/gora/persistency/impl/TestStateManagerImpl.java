/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.persistency.impl;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.avro.util.Utf8;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.mock.persistency.MockPersistent;
import org.apache.gora.persistency.impl.StateManagerImpl;
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
