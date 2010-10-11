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

package org.apache.gora.store;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.gora.persistency.Persistent;

/** FileBackedDataStore supplies necessary interfaces to set input 
 * and output paths for data stored which are file based.   
 */
public interface FileBackedDataStore<K, T extends Persistent> extends DataStore<K, T> {

  public void setInputPath(String inputPath);
  
  public void setOutputPath(String outputPath);
  
  public String getInputPath();
  
  public String getOutputPath();
  
  public void setInputStream(InputStream inputStream);
  
  public void setOutputStream(OutputStream outputStream);

  public InputStream getInputStream();
  
  public OutputStream getOutputStream();
  
}
