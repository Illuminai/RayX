package com.rayx.opencl;

import java.nio.ByteBuffer;

public interface CLTransferable {
    int bytesToInBuffer();

    public void writeToByteBuffer(ByteBuffer buffer);
}
