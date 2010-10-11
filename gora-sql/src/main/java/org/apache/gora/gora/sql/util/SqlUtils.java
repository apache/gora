
package org.gora.sql.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL related utilities
 */
public class SqlUtils {
  
  /** Closes the ResultSet silently */
  public static void close(ResultSet rs) {
    if(rs != null) {
      try {
        rs.close();
      } catch (SQLException ignore) { }
    }
  }
  
  /** Closes the Statement silently */
  public static void close(Statement statement) {
    if(statement != null) {
      try {
        statement.close();
      } catch (SQLException ignore) { }
    }
  }
}
