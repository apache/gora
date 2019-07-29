/*
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
package org.apache.gora.kudu;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.kudu.store.KuduStore;
import org.apache.gora.kudu.utils.KuduBackendConstants;
import org.apache.kudu.test.KuduTestHarness;

/**
 * Helper class for third part tests using gora-kudu backend.
 *
 * @see GoraTestDriver
 */
public class GoraKuduTestDriver extends GoraTestDriver {

  public KuduTestHarness harness = new KuduTestHarness();

  public GoraKuduTestDriver() {
    super(KuduStore.class);
  }

  @Override
  public void setUpClass() throws Exception {
    harness.before();
    conf.set(KuduBackendConstants.PROP_MASTERADDRESSES, harness.getMasterAddressesAsString());
  }

  @Override
  public void tearDownClass() throws Exception {
    harness.after();
  }

}
