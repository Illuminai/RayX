package com.rayx.opencl.exceptions;

public class CLKernelCreationException extends CLException {
    public CLKernelCreationException(int error) {
        this(null, error);
    }

    public CLKernelCreationException(String message, int error) {
        super(message, error);
    }
}
