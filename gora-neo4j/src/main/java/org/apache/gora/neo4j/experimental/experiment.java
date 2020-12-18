/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.neo4j.experimental;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import org.apache.gora.neo4j.mapping.Neo4jMapping;
import org.apache.gora.neo4j.mapping.Neo4jMappingBuilder;
import org.apache.gora.neo4j.mapping.Property;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Renderer;

public class experiment {

  public static void main(String[] args) throws SQLException, IOException {

    experiment experiment = new experiment();
    experiment.testNeo4jMapping();

  }

  public void testNeo4jMapping() throws SQLException, IOException {

    Neo4jMappingBuilder neo4jMappingBuilder = new Neo4jMappingBuilder(null);
    String path = "src/test/resources/gora-neo4j-mapping.xml";

    File file = new File(path);
    FileInputStream fileInputStream = new FileInputStream(file);
    Neo4jMapping readMapping = neo4jMappingBuilder.readMapping(fileInputStream);

    System.out.println(readMapping.getLabel());
    System.out.println(readMapping.getNodeKey().getName());
    System.out.println(readMapping.getNodeKey().getSqltype());
    for (Iterator<Map.Entry<String, Property>> it = readMapping.getProperties().entrySet().iterator(); it.hasNext();) {
      Map.Entry<String, Property> next = it.next();
      System.out.println(next.getKey());
      System.out.println(next.getValue().getName());
      System.out.println(next.getValue().getSqltype());

    }

  }

  private static void testNeo4jCon() throws SQLException {
    try (Connection con = DriverManager.getConnection("jdbc:neo4j:bolt://localhost/?flatten=-1", "neo4j", "admin")) {

      //Insert 10 Persons
      for (int i = 0; i < 10; i++) {
        Node named = Cypher.node("Person").named("person_" + i).withProperties("name", Cypher.literalOf("Tom Hanks " + i));
        Statement build = Cypher.create(named).build();
        Renderer defaultRenderer = Renderer.getDefaultRenderer();
        String render = defaultRenderer.render(build);
        try (PreparedStatement stmt = con.prepareStatement(render)) {
          stmt.execute();
        }
      }
      //Query 10 Persosn by name
      for (int i = 0; i < 10; i++) {
        Node named = Cypher.node("Person").withProperties("name", Cypher.literalOf("Tom Hanks " + i)).named("p");
        Statement build = Cypher.match(named).returning(named).build();
        Renderer defaultRenderer = Renderer.getDefaultRenderer();
        String render = defaultRenderer.render(build);
        try (PreparedStatement stmt = con.prepareStatement(render)) {
          try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
              System.out.println("Hello, " + rs.getString("p.name"));
            }
          }
        }
      }
    }
  }

}
