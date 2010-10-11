
package org.gora.query;

import org.gora.persistency.Persistent;

/**
 * PartitionQuery divides the results of the Query to multi partitions, so that 
 * queries can be run locally on the nodes that hold the data. PartitionQuery's are 
 * used for generating Hadoop InputSplits.
 */
public interface PartitionQuery<K, T extends Persistent> extends Query<K, T> {

  /* PartitionQuery interface relaxes the dependency of DataStores to Hadoop*/
  
  /**
   * Returns the locations on which this partial query will run locally.
   * @return the addresses of machines
   */
  public String[] getLocations();
  
}
