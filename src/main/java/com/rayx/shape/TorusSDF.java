package com.rayx.shape;

import java.nio.ByteBuffer;

public class TorusSDF extends Shape {
    private final float smallRadius, bigRadius;

    public TorusSDF(Vector3d position, Vector3d rotation, float smallRadius, float bigRadius) {
        super(position, rotation, null);
        this.smallRadius = smallRadius;
        this.bigRadius = bigRadius;
    }

    @Override
    public int getShape() {
        return TORUS;
    }

    @Override
    public float getMaxRadius() {
        return smallRadius + bigRadius;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        buffer.
                putFloat(smallRadius).
                putFloat(bigRadius);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Float.BYTES * 2;
    }
}
