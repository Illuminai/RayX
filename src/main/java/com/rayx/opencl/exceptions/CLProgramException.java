package com.rayx.opencl.exceptions;

public class CLProgramException extends CLException {
    public CLProgramException(int error) {
        this(null, error);
    }

    public CLProgramException(String message, int error) {
        super(message, error);
    }
}
