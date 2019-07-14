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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.tools.ant.filters.StringInputStream;

/**
 * Class that holds the GoraStorage configuration set at the pig script for the Storage:
 * 
 * LOAD '.' USING org.apache.gora.pig.GoraStorage('{
 *         "persistentClass": "",
 *         "fields": "",
 *         "goraProperties": "",
 *         "mapping": "",
 *         "configuration": {}
 * }') ;
 * 
 */
public class StorageConfiguration {

  /**
   * The datastore key class full name. At this moment only String supported.
   */
  private String keyClass = "java.lang.String";
  
  /**
   * The persistent class full name
   */
  private String persistentClass ;
  
  /**
   * Comma separated list of fields "field1,field2,field3". Defalts to "*"
   */
  private String fields = "*" ;
  
  /**
   * String with the contents of gora.properties
   */
  private String goraProperties ;
  
  /**
   * String with the XML from gora-xxx-mapping.xml
   */
  private String mapping ;
  
  /**
   * Additional configuration that will be copied into Hadoop's Configuration, like HBase connection configuration 
   */
  private Map<String,String> configuration ;

  public String getKeyClass() {
    return keyClass;
  }

//  public void setKeyClass(String keyClass) {
//    this.keyClass = keyClass;
//  }

  public String getPersistentClass() {
    return persistentClass;
  }

  public void setPersistentClass(String persistentClass) {
    this.persistentClass = persistentClass;
  }

  public String getFields() {
    return fields;
  }

  public void setFields(String fields) {
    this.fields = fields;
  }
  
  public List<String> getFieldsAsList() {
    return Arrays.asList(this.fields.split("\\s*,\\s*")) ;
  }

  /**
   * Flags if the fields has the value "*" selecting all fields.
   * @return
   */
  public boolean isAllFieldsQuery() {
    return "*".equals(this.fields) ;
  }
  
  public String getGoraProperties() {
    return goraProperties;
  }

  public void setGoraProperties(String goraProperties) {
    this.goraProperties = goraProperties;
  }

  /**
   * Returns the properties data in a Properties instacne
   * @return
   * @throws IOException 
   */
  public Properties getGoraPropertiesAsProperties() throws IOException {
    Properties properties = new Properties();
    properties.load(new StringInputStream(this.goraProperties));
    
    if (this.getMapping() != null) {
      properties.setProperty("gora.mapping", mapping) ;
    }
    
    return properties;
  }
  
  public String getMapping() {
    return mapping;
  }

  public void setMapping(String mapping) {
    this.mapping = mapping;
  }

  public Map<String,String> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Map<String,String> configuration) {
    this.configuration = configuration;
  }
  
  /**
   * Updates the parameter configuration with the data in this bean. This method does not overwrite the existing key-values!
   * @param hadoopConfiguration
   */
  public void mergeConfiguration(Configuration hadoopConfiguration) {
    this.configuration.forEach((key,value) -> hadoopConfiguration.setIfUnset(key, value));
  }
  
}
