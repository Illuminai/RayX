package com.rayx.opencl.exceptions;

public class CLFreeException extends CLException {
    public CLFreeException(int error) {
        this(null, error);
    }

    public CLFreeException(String message, int error) {
        super(message, error);
    }
}
