package org.apache.gora.dynamodb.example.generated;

import java.util.List;
import java.util.Set;

import org.apache.avro.Schema.Field;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.Tombstone;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Person")
public class Person implements Persistent {
    private double ssn;

    @DynamoDBHashKey(attributeName="ssn") 
    public double getHashKey() {  return ssn; } 
    public void setHashKey(double pSsn){  this.ssn = pSsn; }

    private String date;

    @DynamoDBRangeKey(attributeName="date") 
    public String getRangeKey() { return date; } 
    public void setRangeKey(String pDate){  this.date = pDate; }

    private String lastName;
    @DynamoDBAttribute(attributeName = "LastName")
    public String getLastName() {  return lastName;  }
    public void setLastName(String pLastName) {  this.lastName = pLastName;  }

    private Set<String> visitedplaces;
    @DynamoDBAttribute(attributeName = "Visitedplaces")
    public Set<String> getVisitedplaces() {  return visitedplaces;  }
    public void setVisitedplaces(Set<String> pVisitedplaces) {  this.visitedplaces = pVisitedplaces;  }

    private double salary;
    @DynamoDBAttribute(attributeName = "Salary")
    public double getSalary() {  return salary;  }
    public void setSalary(double pSalary) {  this.salary = pSalary;  }

    private String firstName;
    @DynamoDBAttribute(attributeName = "FirstName")
    public String getFirstName() {  return firstName;  }
    public void setFirstName(String pFirstName) {  this.firstName = pFirstName;  }


    public void setNew(boolean pNew){}
    public void setDirty(boolean pDirty){}
    @Override
    public void clear() { }
    @Override
    public void clearField(String Field) { }
    @Override
    public Person clone() { return null; }
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
    public Persistent newInstance() { return new Person(); }
}
