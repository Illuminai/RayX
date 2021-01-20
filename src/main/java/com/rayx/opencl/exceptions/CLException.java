package com.rayx.opencl.exceptions;

public class CLException extends RuntimeException {
    private final int error;

    public CLException(String message, int error) {
        super(message == null ? Integer.toString(error) : message);
        this.error = error;
    }

    public CLException(int error) {
        this(null, error);
    }

    public int getError() {
        return error;
    }
}
