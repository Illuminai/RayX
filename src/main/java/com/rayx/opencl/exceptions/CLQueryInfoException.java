package com.rayx.opencl.exceptions;

public class CLQueryInfoException extends CLException {
    public CLQueryInfoException(int error) {
        this(null, error);
    }

    public CLQueryInfoException(String message, int error) {
        super(message, error);
    }
}
