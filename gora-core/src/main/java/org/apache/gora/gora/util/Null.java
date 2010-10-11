
package org.gora.util;

/**
 * Placeholder for Null type arguments
 */
public class Null {

  private static final Null INSTANCE = new Null();
  
  public Null() {
  }
  
  public static Null get() {
    return INSTANCE;
  }
}
