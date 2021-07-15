package org.apache.gora.sql.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;

public class SqlMapping {
    public static final Logger log = LoggerFactory.getLogger(SqlMapping.class);

    private String tableClass;
    private HashMap<String, String> classToTable = new HashMap<>();
    private HashMap<String, String> tableToClass = new HashMap<>();
    private HashMap<String, ColumnType> tables = new HashMap<>();

    public String getTableClass() {
        return tableClass;
    }

    public void setTableClass(String tableClass) {
        this.tableClass = tableClass;
    }

    private void registerTable(String name, ColumnType type) {
        if (tables.containsKey(name) && (tables.get(name) != type))
            throw new IllegalStateException("The field '" + name + "' is already "
                    + "registered with a different type.");
        tables.put(name, type);
    }

    public void registerTableColumn(String columnName,
                                    String columnTableName, String type) {
        try {
            registerTable(columnName,
                    ColumnType.valueOf(type.toUpperCase(Locale.getDefault())));
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
            classToTable.put(columnName, columnTableName);
            tableToClass.put(columnTableName, columnName);
        }
    }

    public String[] getTableColumns() {
        return tableToClass.keySet().toArray(new String[tableToClass.keySet().size()]);
    }

    public String getTableColumnName(String field) {
        return classToTable.get(field);
    }

    protected ColumnType getTableColumnType(String field) {
        return tables.get(field);
    }

    public static enum ColumnType {

        BOOLEAN("boolean"),
        INTEGER("integer"),
        LONG("long"),
        FLOAT("float"),
        SHORT("short"),
        DOUBLE("double"),
        STRING("string");

        private final String stringValue;

        ColumnType(final String s) {
            stringValue = s;
        }

        public String toString() {
            return stringValue;
        }

    }
}
