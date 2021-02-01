package com.rayx.opencl.exceptions;

public class CLKernelExecutionException extends CLException {
    public CLKernelExecutionException(int error) {
        this(null, error);
    }

    public CLKernelExecutionException(String message, int error) {
        super(message, error);
    }
}
