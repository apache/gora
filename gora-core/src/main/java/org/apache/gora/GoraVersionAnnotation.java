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
package org.apache.gora;

import java.lang.annotation.*;

/**
 * A package attribute that captures the version of Gora that was compiled.
 * This implementation reflects that used in Apache Hadoop.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface GoraVersionAnnotation {
 
  /**
   * Get the Gora version
   * @return the version string e.g. "X.Y"
   */
  String version();
  
  /**
   * Get the username .
   * @return the username of the person that compiled Gora
   */
  String user();
  
  /**
   * Get the date when Gora was compiled.
   * @return the date in unix 'date' format
   */
  String date();
  
  /**
   * Get the url for the subversion repository.
   * @return the appropriate Gora URL in SVN
   */
  String url();
  
  /**
   * Get the subversion revision.
   * @return the revision number as a string (eg. "451451")
   */
  String revision();

  /**
   * Get a checksum of the source files from which
   * Gora was compiled.
   * @return a string that uniquely identifies the source
   **/
  String srcChecksum();
}
