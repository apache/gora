package org.apache.gora.hbase.store;

import java.util.ArrayList;
import java.util.List;

/**
 * Class with the table info returned by HBaseMetadataAnalyzer with the information
 * about a HBase table.
 * 
 * - Column families
 */
public class HBaseTableMetadata {
    
    List<String> columnFamilies = new ArrayList<>() ;

    /**
     * Column families present in a given table at HBase
     */
    public List<String> getColumnFamilies() {
        return columnFamilies;
    }

    public void setColumnFamilies(List<String> columnFamilies) {
        this.columnFamilies = columnFamilies;
    } 
    
}
