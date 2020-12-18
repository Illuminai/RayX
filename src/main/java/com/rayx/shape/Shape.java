package com.rayx.shape;

import java.nio.ByteBuffer;

public abstract class Shape {

    /** Used to determine the size of shape_t */
    public static final int SHAPE = 0x0;
    public static final int SPHERE = 0x1;
    public static final int TORUS = 0x2;

    /** Is the same for all shapes of a class*/
    public abstract int getName();

    public void writeToByteBuffer(ByteBuffer buffer) {
        buffer.putInt(getName());
    }

    public int bytesToInBuffer() {
        return Integer.BYTES;
    }
}
