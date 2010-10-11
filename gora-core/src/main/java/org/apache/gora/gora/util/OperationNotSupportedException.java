
package org.gora.util;

/**
 * Operation is not supported or implemented.
 */
public class OperationNotSupportedException extends RuntimeException {

  private static final long serialVersionUID = 2929205790920793629L;

  public OperationNotSupportedException() {
    super();
  }

  public OperationNotSupportedException(String message, Throwable cause) {
    super(message, cause);
  }

  public OperationNotSupportedException(String message) {
    super(message);
  }

  public OperationNotSupportedException(Throwable cause) {
    super(cause);
  }
}
