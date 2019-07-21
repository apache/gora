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
package org.apache.gora.jet;

import org.apache.gora.persistency.impl.PersistentBase;

public class JetInputOutputFormat<KeyOut, ValueOut extends PersistentBase> {
  public KeyOut key;
  public ValueOut value;

  public JetInputOutputFormat(KeyOut key, ValueOut value) {
    this.key = key;
    this.value = value;
  }

  public KeyOut getKey() {
    return key;
  }

  public void setKey(KeyOut key) {
    this.key = key;
  }

  public ValueOut getValue() {
    return value;
  }

  public void setValue(ValueOut value) {
    this.value = value;
  }
}
