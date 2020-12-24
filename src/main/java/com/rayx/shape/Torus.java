package com.rayx.shape;

import java.nio.ByteBuffer;

public class Torus extends Shape {
    private final Vector3d position, rotation;
    private final double smallRadius, bigRadius;

    public Torus(Vector3d position, Vector3d rotation, double smallRadius, double bigRadius) {
        this.position = position;
        this.rotation = rotation;
        this.smallRadius = smallRadius;
        this.bigRadius = bigRadius;
    }

    @Override
    public int getName() {
        return TORUS_SDF;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        buffer.
                putDouble(position.getX()).
                putDouble(position.getY()).
                putDouble(position.getZ()).
                putDouble(rotation.getX()).
                putDouble(rotation.getY()).
                putDouble(rotation.getZ()).
                putDouble(smallRadius).
                putDouble(bigRadius);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Double.BYTES * 8;
    }
}
