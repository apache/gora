package org.apache.gora.examples.generated;
import java.util.Set;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.StateManager;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "person")
public class person implements Persistent {
    private String ssn;
    private String date;

    @DynamoDBHashKey(attributeName="ssn") 
    public String getHashKey() {  return ssn; } 
    public void setHashKey(String pSsn){  this.ssn = pSsn; }
    @DynamoDBRangeKey(attributeName="date") 
    public String getRangeKey() {  return date; } 
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
    public StateManager getStateManager() { return null; }
    @Override
    public Persistent newInstance(StateManager stateManager) { return null; }
    @Override
    public String[] getFields() { return null; }
    @Override
    public String getField(int index) {	return null; }
    @Override
    public int getFieldIndex(String field) { return 0; }
    @Override
    public void clear() { }
    @Override
    public person clone() {	return null; }
    @Override
    public boolean isNew() { return false; }
    @Override
    public void setNew() { }
    @Override
    public void clearNew() {	}
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
    public boolean isReadable(int fieldIndex) {	return false; }
    @Override
    public boolean isReadable(String field) { return false; }
    @Override
    public void setReadable(int fieldIndex) { }
    @Override
    public void setReadable(String field) { }
    @Override
    public void clearReadable(int fieldIndex) { }
    @Override
    public void clearReadable(String field) { }
    @Override
    public void clearReadable() { }
}