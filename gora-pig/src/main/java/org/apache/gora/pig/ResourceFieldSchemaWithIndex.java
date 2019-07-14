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
package org.apache.gora.pig;

import org.apache.pig.ResourceSchema.ResourceFieldSchema;

public class ResourceFieldSchemaWithIndex {
  
  protected ResourceFieldSchema resourceFieldSchema ;
  public ResourceFieldSchema getResourceFieldSchema() {
    return resourceFieldSchema;
  }

  public void setResourceFieldSchema(ResourceFieldSchema resourceFieldSchema) {
    this.resourceFieldSchema = resourceFieldSchema;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  protected int index ;
  
  public ResourceFieldSchemaWithIndex(ResourceFieldSchema resourceFieldSchema, int index) {
    this.resourceFieldSchema = resourceFieldSchema ;
    this.index = index ;
  }
  
}
