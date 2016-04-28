package org.apache.gora.dynamodb.example.generated;

import java.util.List;

import org.apache.avro.Schema.Field;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.Tombstone;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Webpage")
public class Webpage implements Persistent {
    private String id;

    @DynamoDBHashKey(attributeName="id") 
    public String getHashKey() {  return id; } 
    public void setHashKey(String pId){  this.id = pId; }

    private String content;
    @DynamoDBAttribute(attributeName = "Content")
    public String getContent() {  return content;  }
    public void setContent(String pContent) {  this.content = pContent;  }

    private String common;
    @DynamoDBAttribute(attributeName = "Common")
    public String getCommon() {  return common;  }
    public void setCommon(String pCommon) {  this.common = pCommon;  }

    private String outlinks;
    @DynamoDBAttribute(attributeName = "Outlinks")
    public String getOutlinks() {  return outlinks;  }
    public void setOutlinks(String pOutlinks) {  this.outlinks = pOutlinks;  }

    private String parsedContent;
    @DynamoDBAttribute(attributeName = "ParsedContent")
    public String getParsedContent() {  return parsedContent;  }
    public void setParsedContent(String pParsedContent) {  this.parsedContent = pParsedContent;  }


    public void setNew(boolean pNew){}
    public void setDirty(boolean pDirty){}
    @Override
    public void clear() { }
    @Override
    public Webpage clone() { return null; }
    @Override
    public boolean isDirty() { return false; }
    @Override
    public boolean isDirty(int fieldIndex) { return false; }
    @Override
    public boolean isDirty(String field) { return false; }
    @Override
    public void setDirty() { }
    @Override
    public void setDirty(int fieldIndex) { }
    @Override
    public void setDirty(String field) { }
    @Override
    public void clearDirty(int fieldIndex) { }
    @Override
    public void clearDirty(String field) { }
    @Override
    public void clearDirty() { }
    @Override
    public Tombstone getTombstone() { return null; }
    @Override
    public List<Field> getUnmanagedFields() { return null; }
    @Override
    public Persistent newInstance() { return new Webpage(); }
}
