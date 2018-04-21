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
