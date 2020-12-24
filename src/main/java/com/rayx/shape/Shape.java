package com.rayx.shape;

import java.nio.ByteBuffer;

public abstract class Shape {

    /** Used to determine the size of shape_t */
    public static final int SHAPE = 0x0;
    /** A sphere, rendered using raytracing*/
    public static final int SPHERE_RTC = 0x1;
    /** A torus, rendered using sdf*/
    public static final int TORUS_SDF = 0x2;

    /** Is the same for all shapes of a class*/
    public abstract int getName();

    public void writeToByteBuffer(ByteBuffer buffer) {
        buffer.putInt(getName());
    }

    public int bytesToInBuffer() {
        return Integer.BYTES;
    }
}
