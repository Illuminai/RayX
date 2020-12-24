package com.rayx.shape;

import java.nio.ByteBuffer;

public class Sphere extends Shape {
    private final Vector3d position;
    private final double radius;

    public Sphere(double x, double y, double z, double radius) {
        this.position = new Vector3d(x, y, z);
        this.radius = radius;
    }

    public Sphere(Vector3d position, double radius) {
        this.position = position;
        this.radius = radius;
    }

    @Override
    public int getName() {
        return SPHERE_RTC;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        buffer.
                putDouble(position.getX()).
                putDouble(position.getY()).
                putDouble(position.getZ()).
                putDouble(radius);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Double.BYTES * 4;
    }
}
