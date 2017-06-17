package org.apache.gora.aerospike.store;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Info;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class AerospikeParameters {
  private String host;
  private int port;
  private String user;
  private String password;
  private AerospikeMapping aerospikeMapping;
  private boolean singleBin;

  // Property names
  private static final String AS_SERVER_IP = "server.ip";
  private static final String AS_SERVER_port = "server.port";

  // Default property values
  private static final String DEFAULT_SERVER_IP = "localhost";
  private static final String DEFAULT_SERVER_PORT = "3000";

  public static final Logger LOG = LoggerFactory.getLogger(AerospikeParameters.class);

  public AerospikeParameters(AerospikeMapping aerospikeMapping, Properties properties) throws NumberFormatException {
    this.aerospikeMapping = aerospikeMapping;
    this.host = properties.getProperty(AS_SERVER_IP, DEFAULT_SERVER_IP);
    try {
      this.port = Integer.parseInt(properties.getProperty(AS_SERVER_port, DEFAULT_SERVER_PORT));
    } catch (NumberFormatException e) {
      LOG.error(e.getMessage(), e);
      throw e;
    }
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public AerospikeMapping getAerospikeMapping() {
    return aerospikeMapping;
  }

  public void setAerospikeMapping(AerospikeMapping aerospikeMapping) {
    this.aerospikeMapping = aerospikeMapping;
  }

  public boolean isSingleBin() {
    return singleBin;
  }

  public void setSingleBin(boolean singleBin) {
    this.singleBin = singleBin;
  }


  // Some database calls need to know how the server is configured.
  public void setServerSpecificParameters(AerospikeClient client) throws Exception {
    Node node = client.getNodes()[0];
    String namespaceFilter = "namespace/" + aerospikeMapping.getNamespace();
    String namespaceTokens = Info.request(null, node, namespaceFilter);

    if (namespaceTokens == null) {
      throw new Exception("Failed to get namespace info");
    }

    singleBin = parseBoolean(namespaceTokens, "single-bin");
  }

  private static boolean parseBoolean(String namespaceTokens, String name) {
    String search = name + '=';
    int begin = namespaceTokens.indexOf(search);

    if (begin < 0) {
      return false;
    }

    begin += search.length();
    int end = namespaceTokens.indexOf(';', begin);

    if (end < 0) {
      end = namespaceTokens.length();
    }

    String value = namespaceTokens.substring(begin, end);
    return Boolean.parseBoolean(value);
  }

  // ToDo : check
  public String getBinName(String name) {
    return singleBin ? "" : name;
  }
}
