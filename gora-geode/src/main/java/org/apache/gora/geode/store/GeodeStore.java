package org.apache.gora.geode.store;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.gora.geode.query.GeodeQuery;
import org.apache.gora.geode.query.GeodeResult;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;

import java.io.IOException;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.apache.geode.cache.RegionShortcut.REPLICATE;
import static org.apache.gora.geode.store.GeodeStoreParameters.GEODE_USERNAME;
import static org.apache.gora.geode.store.GeodeStoreParameters.GEODE_SERVER_PORT;
import static org.apache.gora.geode.store.GeodeStoreParameters.GEODE_SERVER_HOST;
import static org.apache.gora.geode.store.GeodeStoreParameters.GEODE_PASSWORD;
import static org.apache.gora.geode.store.GeodeStoreParameters.GEODE_REGION_SHORTCUT;
import static org.apache.gora.geode.store.GeodeStoreParameters.PREFERRED_SCHEMA_NAME;


public class GeodeStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

    private ClientCache clientCache;
    private Region<K, T> region;
    private Properties geodeProperties;
    private CacheFactory cacheFactory;

    @Override
    public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
        super.initialize(keyClass, persistentClass, properties);
        String geodeHostName = (String) properties.get(GEODE_SERVER_HOST);
        int portNumber = Integer.parseInt((String) properties.get(GEODE_SERVER_PORT));
        clientCache = new ClientCacheFactory().addPoolLocator(geodeHostName, portNumber).create();
        String userName = properties.getProperty(GEODE_USERNAME);
        String password = properties.getProperty(GEODE_PASSWORD);
        geodeProperties = properties;

        Properties clientProperties = clientCache.getDistributedSystem().getProperties();
        if (userName != null) {
            clientProperties.setProperty("security-username", userName);
            clientProperties.setProperty("security-password", password);
        } else throw new GoraException();
        cacheFactory = new CacheFactory(clientProperties);
    }

    @Override
    /*
      Schema Name can be assigned via Property file using @PREFERRED_SCHEMA_NAME, or else PersistentClass name is used as the default schema name.
     */
    public String getSchemaName() {
        String preferredSchemaName = properties.getProperty(PREFERRED_SCHEMA_NAME);
        if (preferredSchemaName == null) {
            return persistentClass.getSimpleName();
        }
        return preferredSchemaName;
    }

    @Override
    public void createSchema() throws GoraException {
        try {
            Cache cache = cacheFactory.create();
            String regionShortCut = geodeProperties.getProperty(GEODE_REGION_SHORTCUT);
            RegionFactory<K, T> regionFactory;
            if (regionShortCut != null) {
                regionFactory = cache.createRegionFactory(RegionShortcut.valueOf(regionShortCut));
            } else {
                regionFactory = cache.createRegionFactory(REPLICATE);
            }
            region = regionFactory.create(getSchemaName());
        } catch (Exception e) {
            throw new GoraException(e);
        }
    }

    @Override
    public void deleteSchema() {
        region.destroyRegion();
    }

    @Override
    public boolean schemaExists() {
        Properties properties = clientCache.getDistributedSystem().getProperties();
        CacheFactory factory = new CacheFactory(properties);
        Cache cache = factory.create();
        Region<K, T> rf = cache.getRegion(getSchemaName());
        return rf != null;
    }

    @Override
    public boolean exists(K key) {
        for (K existingKey : region.getInterestList()) {
            if (existingKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public T get(K key, String[] fields) {
        return region.get(key);
    }

    @Override
    public void put(K key, T obj) {
        region.put(key, obj);
    }

    @Override
    public boolean delete(K key) {
        region.destroy(key);
        return true;
    }

    @Override
    public long deleteByQuery(Query<K, T> query) throws GoraException {
        try {
            long deletedRows = 0;
            Result<K, T> result = query.execute();
            while (result.next()) {
                if (delete(result.getKey())) {
                    deletedRows++;
                }
            }
            LOG.info("Geode datastore deleted {} rows from Persistent datastore.", deletedRows);
            return deletedRows;
        } catch (Exception e) {
            throw new GoraException(e);
        }
    }

    @Override
    public Result<K, T> execute(Query<K, T> query) {
        K startKey = query.getStartKey();
        K endKey = query.getEndKey();
        NavigableSet<K> cacheEntrySubList = new ConcurrentSkipListSet<>();
        if (startKey != null && endKey != null) {
            boolean isInTheRegion = false;
            for (K key : region.keySet()) {
                if (key == startKey) {
                    isInTheRegion = true;
                }
                if (isInTheRegion) {
                    cacheEntrySubList.add(key);
                }
                if (key == endKey) {
                    break;
                }
            }
        } else {
            // Empty
            cacheEntrySubList = Collections.emptyNavigableSet();
        }
        return new GeodeResult<>(this, query, cacheEntrySubList);
    }


    @Override
    public Query<K, T> newQuery() {
        GeodeQuery<K, T> query = new GeodeQuery<>(this);
        query.setFields(getFieldsToQuery(null));
        return query;
    }

    @Override
    public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
        List<PartitionQuery<K, T>> partitions = new ArrayList<>();
        PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
                query);
        partitionQuery.setConf(this.getConf());
        partitions.add(partitionQuery);
        return partitions;
    }

    @Override
    public void flush() {
        LOG.info("Geode datastore flushed successfully.");
    }

    @Override
    public void close() {

    }
}
