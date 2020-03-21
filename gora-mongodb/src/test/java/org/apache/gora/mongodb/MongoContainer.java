package org.apache.gora.mongodb;

import com.mongodb.ServerAddress;
import org.testcontainers.containers.FixedHostPortGenericContainer;

public class MongoContainer extends FixedHostPortGenericContainer<MongoContainer> {

    public static final int MONGO_PORT = 27017;

    public MongoContainer(String version) {
        super("mongo:" + version);
        withExposedPorts(MONGO_PORT);
    }

    public ServerAddress getServerAddress() {
        String ipAddress = getContainerIpAddress();
        int port = getMongoPort();
        return new ServerAddress(ipAddress, port);
    }

    public int getMongoPort() {
        return getMappedPort(MONGO_PORT);
    }
}
