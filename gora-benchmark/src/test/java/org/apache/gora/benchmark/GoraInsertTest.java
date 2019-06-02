package org.apache.gora.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.gora.util.GoraException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.StringByteIterator;

import generated.User;

public class GoraInsertTest {
  

  private static final String TABLE = "users";
  private GoraBenchmarkClient client;
  private static HashMap<String, ByteIterator> DATA_TO_INSERT;
  private static HashMap<String, ByteIterator> DATA_TO_UPDATE;
  
  //Setup is executed before each test. Use @BeforeClass if you want to execute a code block just once.
  @Before
  public void setUp() throws Exception {
    DATA_TO_INSERT = new HashMap<>();
    DATA_TO_UPDATE = new HashMap<>();
    for(int i=0; i < 10; i++) {
      DATA_TO_INSERT.put("field"+i, new StringByteIterator("value"+i));
      DATA_TO_UPDATE.put("field"+i, new StringByteIterator("updated"+i));
    }
    Properties p = new Properties();
    p.setProperty("key.class", "java.lang.String");
    p.setProperty("persistent.class", "generated.User");
    client = new GoraBenchmarkClient();
    client.setProperties(p);
    client.init();
  }
  
  @After
  public void cleanUp() throws Exception{
    if(client != null)
      client.cleanup();
    client = null;
  }
  
  private User readRecord(String key) throws GoraException {
    User u = client.dataStore.get(key);
    return u;
  }
  
  @Test
  public void testClientInitialisation() {
    assertNotNull(client);
  }
  

  @Test
  public void testInsert() throws GoraException{ 
    int result1 = client.insert(TABLE, "key1", DATA_TO_INSERT);
    int result2 = client.insert(TABLE, "key2", DATA_TO_INSERT);
    int result3 = client.insert(TABLE, "key3", DATA_TO_INSERT);
    assertEquals(result1, 0);
    assertEquals(result2, 0);
    assertEquals(result3, 0);
  }
  
  
  @Test
  public void   testRead() {
    HashMap<String, ByteIterator> results = new HashMap<>();
    Set<String> fields = new HashSet<>();
    //fields.add("field0");
    int result = client.read(TABLE, "key1", fields, results);
    assertEquals(0, result);
    assertEquals(DATA_TO_INSERT.size(), results.size());
    assertEquals(DATA_TO_INSERT.get("field0").toString(), results.get("field0").toString());
    assertEquals(DATA_TO_INSERT.get("field0").toString(), "value0");
  }
  
  @Test
  public void testScan(){
    Vector<HashMap<String, ByteIterator>> results = new Vector<HashMap<String, ByteIterator>>();
    Set<String> fields = new HashSet<>();
    //fields.add("field0");
    int result = client.scan(TABLE, "key1", 2, fields, results);
    assertEquals(result,0);
    assertEquals(results.size(),2);
  }
  
  @Test
  public void testUpdate() throws GoraException{
    int result = client.update(TABLE, "key1", DATA_TO_UPDATE);
    assertEquals(result,0);
    User u = readRecord("key1");
    assertEquals("updated0", u.getField0().toString());
    //Read Record from 
  }
  
 
 

}
