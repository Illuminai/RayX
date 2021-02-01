package com.rayx.opencl.exceptions;

public class CLLinkException extends CLException {
    public CLLinkException(int error) {
        this(null, error);
    }

    public CLLinkException(String message, int error) {
        super(message, error);
    }
}
