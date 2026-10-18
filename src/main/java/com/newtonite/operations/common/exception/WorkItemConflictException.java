package com.newtonite.operations.common.exception;

public class WorkItemConflictException extends RuntimeException {
    public WorkItemConflictException(String message) {
        super(message);
    }

    public WorkItemConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
