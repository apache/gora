package org.apache.gora.sql.store;

import org.apache.gora.persistency.impl.PersistentBase;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * Utility builder for create RethinkDB mapping from gora-rethinkdb-mapping.xml.
 */

public class SqlMappingBuilder<K, T extends PersistentBase> {

    public static final Logger log = LoggerFactory.getLogger(SqlMappingBuilder.class);
    public static final String ATT_NAME = "name";
    public static final String ATT_TYPE = "type";
    public static final String TAG_CLASS = "class";
    public static final String TAG_PRIMARY_KEY = "primarykey";
    public static final String ATT_KEY_CLASS = "keyClass";
    public static final String ATT_TABLE = "table";
    public static final String ATT_LENGTH = "length";
    public static final String ATT_COLUMN = "column";


    private final SqlStore<K, T> dataStore;

    private SqlMapping mapping;

    public SqlMappingBuilder(final SqlStore<K, T> store) {
        this.dataStore = store;
        this.mapping = new SqlMapping();
    }

    public SqlMapping build() {
        if (mapping.getTableClass() == null)
            throw new IllegalStateException("Document Class is not specified.");
        return mapping;
    }

    protected SqlMappingBuilder fromFile(String uri) throws IOException {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            InputStream is = getClass().getResourceAsStream(uri);
            if (is == null) {
                String msg = "Unable to load the mapping from classpath resource '" + uri
                        + "' Re-trying local from local file system location.";
                log.warn(msg);
                is = new FileInputStream(uri);
            }
            Document doc = saxBuilder.build(is);
            Element root = doc.getRootElement();
            List<Element> classElements = root.getChildren(TAG_CLASS);
            for (Element classElement : classElements) {
                final Class<T> persistentClass = dataStore.getPersistentClass();
                final Class<K> keyClass = dataStore.getKeyClass();
                if (matchesKeyClassWithMapping(keyClass, classElement)
                        && matchesPersistentClassWithMapping(persistentClass, classElement)) {
                    loadPersistentClass(classElement, persistentClass);
                    break;
                }
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
        return this;
    }

    private boolean matchesPersistentClassWithMapping(final Class<T> persistentClass,
                                                      final Element classElement) {
        return classElement.getAttributeValue(ATT_NAME).equals(persistentClass.getName());
    }

    private boolean matchesKeyClassWithMapping(final Class<K> keyClass,
                                               final Element classElement) {
        return classElement.getAttributeValue(ATT_KEY_CLASS).equals(keyClass.getName());
    }

    private void loadPersistentClass(Element classElement,
                                     Class<T> pPersistentClass) {

        String tableClassForMapping = classElement.getAttributeValue(ATT_TABLE);
        String resolvedTableClass = dataStore.getSchemaName(tableClassForMapping,
                pPersistentClass);
        mapping.setTableClass(resolvedTableClass);

//        Element primaryKey = classElement.getChild(TAG_PRIMARY_KEY);
//        primaryKey.getAttributeValue(ATT_COLUMN), primaryKey.getAttributeValue(ATT_LENGTH)


        List<Element> columns = classElement.getChildren(ATT_COLUMN);
        for (Element column : columns) {
            mapping.registerTableColumn(column.getAttributeValue(ATT_NAME),
                    column.getAttributeValue(ATT_COLUMN),
                    column.getAttributeValue(ATT_TYPE));
        }
    }

}
