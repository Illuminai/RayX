package com.rayx.shape;

import java.nio.ByteBuffer;

public class TorusSDF extends Shape implements Shape.ShapeSDF {
    private final Vector3d rotation;
    private final double smallRadius, bigRadius;

    public TorusSDF(Vector3d position, Vector3d rotation, double smallRadius, double bigRadius) {
        super(position, null);
        this.rotation = rotation;
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
                putDouble(rotation.getX()).
                putDouble(rotation.getY()).
                putDouble(rotation.getZ()).
                putDouble(smallRadius).
                putDouble(bigRadius);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Double.BYTES * 5;
    }
}
