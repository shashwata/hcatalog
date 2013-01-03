package com.google.common.util.concurrent;

public class UncheckedExecutionException extends RuntimeException {
	  /**
	   * Creates a new instance with {@code null} as its detail message.
	   */
	  protected UncheckedExecutionException() {}

	  /**
	   * Creates a new instance with the given detail message.
	   */
	  protected UncheckedExecutionException( String message) {
	    super(message);
	  }

	  /**
	   * Creates a new instance with the given detail message and cause.
	   */
	  public UncheckedExecutionException( String message,  Throwable cause) {
	    super(message, cause);
	  }

	  /**
	   * Creates a new instance with the given cause.
	   */
	  public UncheckedExecutionException( Throwable cause) {
	    super(cause);
	  }

	  private static final long serialVersionUID = 0;
}