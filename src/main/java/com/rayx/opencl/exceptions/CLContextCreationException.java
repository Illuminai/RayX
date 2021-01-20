package com.rayx.opencl.exceptions;

public class CLContextCreationException extends CLException {
    public CLContextCreationException(int error) {
        this(null, error);
    }

    public CLContextCreationException(String message, int error) {
        super(message, error);
    }
}
