package org.apache.gora.examples.generated;

import java.util.Set;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.StateManager;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "person")
public class person implements Persistent{
	
	// hash key
	private String hashKey;
	
	// range hash key
    private String rangeKey;
    
    private String firstName;
    
    private String lastName;
    
    private double salary;
    
    private Set<String> visitedPlaces;
    
    public void setNew(boolean pNew){}
    
    public void setDirty(boolean pDirty){}
    
    @DynamoDBHashKey(attributeName="HashKey")
    public String getHashKey() {  return hashKey;  }
    public void setHashKey(String pHashKey) {  this.hashKey = pHashKey;  }
    
    @DynamoDBRangeKey(attributeName="RangeKey")
    public String getRangeKey() {  return rangeKey;  }
    public void setRangeKey(String pRangeKey) {  this.rangeKey = pRangeKey; }
    /*
    @DynamoDBAttribute(attributeName = "ssn")
    public String getSsn() {  return ssn;  }
    public void setSsn(String pSsn) {  this.ssn = pSsn;  }
     */
    @DynamoDBAttribute(attributeName = "FirstName")
    public String getFirstName() {  return firstName;  }
    public void setFirstName(String pFirstName) {  this.firstName = pFirstName;  }
    
    @DynamoDBAttribute(attributeName = "LastName")
    public String getLastName() {  return lastName;  }
    public void setLastName(String pLastName) {  this.lastName = pLastName;  }
    
    @DynamoDBAttribute(attributeName = "Salary")
    public double getSalary() {  return salary; }
    public void setSalary(double pSalary) {  this.salary = pSalary; }
    
    @DynamoDBAttribute(attributeName = "Places")
    public Set<String> getPlacesVisited() { 
    	return visitedPlaces; 
    }    
    public void setPlacesVisited(Set<String> pVisitedPlaces) { this.visitedPlaces = pVisitedPlaces; }
    
    // TODO The persistent abstract class should not force us to rewrite all these methods 
	@Override
	public StateManager getStateManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Persistent newInstance(StateManager stateManager) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getFields() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getField(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getFieldIndex(String field) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public person clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isNew() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setNew() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearNew() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDirty(int fieldIndex) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDirty(String field) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDirty() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDirty(int fieldIndex) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDirty(String field) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearDirty(int fieldIndex) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearDirty(String field) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearDirty() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isReadable(int fieldIndex) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReadable(String field) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setReadable(int fieldIndex) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setReadable(String field) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearReadable(int fieldIndex) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearReadable(String field) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearReadable() {
		// TODO Auto-generated method stub
		
	}

}
