package com.rayx.opencl.exceptions;

public class CLKernelParameterException extends CLException {
    public CLKernelParameterException(int error) {
        this(null, error);
    }

    public CLKernelParameterException(String message, int error) {
        super(message, error);
    }
}
