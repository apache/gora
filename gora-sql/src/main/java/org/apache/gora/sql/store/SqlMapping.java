package org.apache.gora.sql.store;

import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SqlMapping {
    public static final Logger log = LoggerFactory.getLogger(SqlMapping.class);

    private String tableClass;

    private String primaryKey;
    private HashMap<String, SQLDataType> classToTable = new HashMap<>();
    //private HashMap<String, String> tableToClass = new HashMap<>();
    private HashMap<String, SQLDataType> tables = new HashMap<>();

    public String getTableClass() {
        return tableClass;
    }
    public String getPrimaryKey() { return primaryKey; }

    public void setTableClass(String tableClass) {
        this.tableClass = tableClass;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    private void registerTable(String name, SQLDataType type) {
        if (tables.containsKey(name) && (tables.get(name) != type))
            throw new IllegalStateException("The field '" + name + "' is already "
                    + "registered with a different type.");
        tables.put(name, type);
    }

    public void registerTableColumn(String columnName,
                                    String columnTableName, String type) {
        try {
            registerTable(columnTableName,
                    SQLDataType.valueOf(type.toUpperCase(Locale.getDefault())));
        } catch (final IllegalArgumentException e) {
            throw new IllegalStateException("Declared '" + columnName
                    + "' for class field '" + columnTableName
                    + "' is not supported by JOOQ SQL");
        }

        if (classToTable.containsKey(columnName)) {
            if (!classToTable.get(columnName).equals(columnTableName)) {
                throw new IllegalStateException("The class field '" + columnName
                        + "' is already registered in the mapping"
                        + " with the document field '"
                        + classToTable.get(columnName)
                        + " which differs from the new one '" + columnTableName + "'.");
            }
        } else {
            classToTable.put(columnTableName, SQLDataType.valueOf(type.toUpperCase(Locale.getDefault())));
            //tableToClass.put(type.toUpperCase(Locale.getDefault()), columnName);
        }
    }

//    public String[] getTableColumns() {
//        return tableToClass.keySet().toArray(new String[tableToClass.keySet().size()]);
//    }

    public Map<String, SQLDataType> getAllColumns() {
        return classToTable;
    }

//    public String getTableColumnName(String field) {
//        return classToTable.get(field);
//    }

    protected SQLDataType getTableColumnType(String field) {
        return tables.get(field);
    }

    public static enum SQLDataType {

        BOOLEAN("boolean"),
        INTEGER("integer"),
        LONG("long"),
        FLOAT("float"),
        SHORT("short"),
        DOUBLE("double"),
        VARCHAR("varchar");

        private final String stringValue;

        SQLDataType(final String s) {
            stringValue = s;
        }

        public String toString() {
            return stringValue;
        }
    }
}
