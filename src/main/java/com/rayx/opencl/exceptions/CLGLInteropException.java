package com.rayx.opencl.exceptions;

public class CLGLInteropException extends CLException {
    public CLGLInteropException(int error) {
        this(null, error);
    }

    public CLGLInteropException(String message, int error) {
        super(message, error);
    }
}
