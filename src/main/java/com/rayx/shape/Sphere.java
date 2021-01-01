package com.rayx.shape;

import java.nio.ByteBuffer;

public class Sphere extends Shape {
    private final double radius;

    public Sphere(double x, double y, double z, double radius) {
        this(new Vector3d(x, y, z), radius);
    }

    public Sphere(Vector3d position, double radius) {
        super(position);
        this.radius = radius;
    }

    @Override
    public double getMaxRadius() {
        return radius;
    }

    @Override
    public int getName() {
        return SPHERE_RTC;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        buffer.
                putDouble(radius);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Double.BYTES;
    }
}
