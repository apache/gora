package org.apache.gora.geode.store;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.apache.geode.cache.RegionShortcut.REPLICATE;

public class GeodeStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

    ClientCache cache;
    Region<K, T> region;

    @Override
    public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
        super.initialize(keyClass, persistentClass, properties);
        String geodeHostName = "127.0.0.1";//properties.get("");
        int portNumber = 10334;//  properties.get("");
        cache = new ClientCacheFactory().addPoolLocator(geodeHostName, portNumber).create();
    }

    @Override
    public String getSchemaName() {
        return null;
    }

    @Override
    public void createSchema() throws GoraException {
        try {
            Properties properties = cache.getDistributedSystem().getProperties();
            CacheFactory factory = new CacheFactory(properties);
            Cache cache = factory.create();
            RegionFactory rf = cache.createRegionFactory(REPLICATE);
            Region<K, T> tempRegion = rf.create(persistentClass.getSimpleName());
            region = tempRegion;
        } catch (Exception e) {
            throw new GoraException(e);
        }
    }

    @Override
    public void deleteSchema()  {
        region.destroyRegion();
    }

    @Override
    public boolean schemaExists()  {
        return false;
    }

    @Override
    public boolean exists(K key)  {
        for (K existingKey : region.getInterestList()) {
            if (existingKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public T get(K key, String[] fields)  {
        return region.get(key);

    }

    @Override
    public void put(K key, T obj)  {
        region.put(key, obj);
    }

    @Override
    public boolean delete(K key)  {
        region.destroy(key);
        return true;
    }

    @Override
    public long deleteByQuery(Query<K, T> query)  {
        return 0;
    }

    @Override
    public Result<K, T> execute(Query<K, T> query)  {
        return null;
    }

    @Override
    public Query<K, T> newQuery() {
        return null;
    }

    @Override
    public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
        return null;
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }
}
