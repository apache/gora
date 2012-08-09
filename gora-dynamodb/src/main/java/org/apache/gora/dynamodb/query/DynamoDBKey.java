package org.apache.gora.dynamodb.query;

public class DynamoDBKey<H, R>  {

  private H hashKey;
  private R rangeKey;
  
  public H getHashKey() {
	return hashKey;
  }
  public void setHashKey(H hashKey) {
	this.hashKey = hashKey;
  }
  public R getRangeKey() {
	return rangeKey;
  }
  public void setRangeKey(R rangeKey) {
	this.rangeKey = rangeKey;
  }
  
}
