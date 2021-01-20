package com.rayx.opencl.exceptions;

public class CLCompileException extends CLException {
    public CLCompileException(int error) {
        this(null, error);
    }

    public CLCompileException(String message, int error) {
        super(message, error);
    }
}
