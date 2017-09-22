package org.apache.gora.hbase.store;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

public class HBaseStoreMetadataAnalyzer extends DataStoreMetadataAnalyzer {

    private Connection hbaseConnection ;
    
    @Override
    public void initialize() throws GoraException {
        try {
            this.hbaseConnection = ConnectionFactory.createConnection(this.getConf()) ;
        } catch (IOException e) {
            throw new GoraException(e) ;
        }
    }
    
    @Override
    public String getType() {
        return "HBASE" ;
    }

    @Override
    public List<String> getTablesNames() throws GoraException {
        try {
            Admin hbaseAdmin = this.hbaseConnection.getAdmin();
            TableName[] tableNames = hbaseAdmin.listTableNames();
            List<String> names = Arrays.stream(tableNames).map(tableNameInfo -> tableNameInfo.getNameAsString()).collect(Collectors.toList()) ;
            hbaseAdmin.close();
            return names;
        } catch (Exception e) {
            throw new GoraException(e) ;
        }
    }

    @Override
    public HBaseTableMetadata getTableInfo(String tableName) throws GoraException {
        try {
            Admin hbaseAdmin = this.hbaseConnection.getAdmin();
            TableName hbaseTableName =  TableName.valueOf(tableName);
            HTableDescriptor tableDescriptor = hbaseAdmin.getTableDescriptor(hbaseTableName) ;
            HBaseTableMetadata tableMetadata = new HBaseTableMetadata() ;
            tableMetadata.getColumnFamilies().addAll(Arrays.stream(tableDescriptor.getColumnFamilies()).map(hcolumn -> hcolumn.getNameAsString()).collect(Collectors.toList())) ;
            hbaseAdmin.close();
            return tableMetadata;
        } catch (Exception e) {
            throw new GoraException(e) ;
        }
    }

    public void close() throws IOException {
        if (this.hbaseConnection != null) {
            this.hbaseConnection.close();
            this.hbaseConnection = null ;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }
    
}
