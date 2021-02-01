package com.rayx.opencl.exceptions;

public class CLBufferCreationException extends CLException {
    public CLBufferCreationException(int error) {
        this(null, error);
    }

    public CLBufferCreationException(String message, int error) {
        super(message, error);
    }
}
