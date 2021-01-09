package com.rayx.shape;

import java.nio.ByteBuffer;

public class TorusSDF extends Shape implements Shape.ShapeSDF {
    private final double smallRadius, bigRadius;

    public TorusSDF(Vector3d position, Vector3d rotation, double smallRadius, double bigRadius) {
        super(position, rotation,null);
        this.smallRadius = smallRadius;
        this.bigRadius = bigRadius;
    }

    @Override
    public int getName() {
        return TORUS_SDF;
    }

    @Override
    public double getMaxRadius() {
        return smallRadius + bigRadius;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        buffer.
                putDouble(smallRadius).
                putDouble(bigRadius);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Double.BYTES * 2;
    }
}
