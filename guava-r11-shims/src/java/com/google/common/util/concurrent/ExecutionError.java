package com.google.common.util.concurrent;

public class ExecutionError extends Error {
	  /**
	   * Creates a new instance with {@code null} as its detail message.
	   */
	  protected ExecutionError() {}

	  /**
	   * Creates a new instance with the given detail message.
	   */
	  protected ExecutionError(String message) {
	    super(message);
	  }

	  /**
	   * Creates a new instance with the given detail message and cause.
	   */
	  public ExecutionError(String message, Error cause) {
	    super(message, cause);
	  }

	  /**
	   * Creates a new instance with the given cause.
	   */
	  public ExecutionError(Error cause) {
	    super(cause);
	  }

	  private static final long serialVersionUID = 0;
	}