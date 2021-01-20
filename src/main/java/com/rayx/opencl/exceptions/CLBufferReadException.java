package com.rayx.opencl.exceptions;

public class CLBufferReadException extends CLException {
    public CLBufferReadException(int error) {
        this(null, error);
    }

    public CLBufferReadException(String message, int error) {
        super(message, error);
    }
}
