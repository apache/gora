
package org.gora.persistency;

/**
 * Persistency state of an object or field.
 */
public enum State {
  
  /** The object is newly loaded */
  NEW,
  
  /** The value of the field has not been changed after loading*/
  CLEAN,
  
  /** The value of the field has been altered*/
  DIRTY,
  
  /** The object or field is deleted */
  DELETED
}
