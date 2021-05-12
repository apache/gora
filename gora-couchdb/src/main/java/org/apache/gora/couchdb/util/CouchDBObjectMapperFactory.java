/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.couchdb.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ektorp.CouchDbConnector;
import org.ektorp.impl.ObjectMapperFactory;
import org.ektorp.impl.jackson.EktorpJacksonModule;

/**
 * This class is implemantation of {@link org.ektorp.impl.ObjectMapperFactory}.
 * Created a object mapper instance.
 */
public class CouchDBObjectMapperFactory implements ObjectMapperFactory {

  private ObjectMapper instance;
  private boolean writeDatesAsTimestamps = false;

  /**
   * Create a object mapper instance
   *
   * @return the synchornized {@link ObjectMapper} instance.
   */
  public synchronized ObjectMapper createObjectMapper() {
    if (instance == null) {
      instance = new ObjectMapper();
      applyDefaultConfiguration(instance);
    }
    return instance;
  }

  /**
   * Create a object mapper object via couchdb connector
   *
   * @param connector a instantiated {@link CouchDbConnector}
   * @return  the synchornized {@link ObjectMapper} instance.
   */
  public synchronized ObjectMapper createObjectMapper(CouchDbConnector connector) {
    this.createObjectMapper();
    instance.registerModule(new EktorpJacksonModule(connector, instance));
    return instance;
  }

  /**
   * Apply default configuration
   *
   * @param om a instantiated {@link ObjectMapper} object
   */
  private void applyDefaultConfiguration(ObjectMapper om) {
    om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, this.writeDatesAsTimestamps);
    om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

}