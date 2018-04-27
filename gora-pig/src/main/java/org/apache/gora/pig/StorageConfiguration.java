package org.apache.gora.pig;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.tools.ant.filters.StringInputStream;

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
   * Comma separated list of fields, defalts to "*"
   */
  private String fields = "*" ;
  
  /**
   * String with the contents of gora.properties
   */
  private String goraProperties ;
  
  /**
   * String with the XML from gora-xxx-mapping.xml
   */
  private String Mapping ;
  
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
    return properties;
  }
  
  public String getMapping() {
    return Mapping;
  }

  public void setMapping(String mapping) {
    Mapping = mapping;
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
