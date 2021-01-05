package com.rayx.shape;

import java.nio.ByteBuffer;

public class SphereRTC extends Shape {
    private final double radius;

    public SphereRTC(double x, double y, double z, double radius) {
        this(new Vector3d(x, y, z), radius);
    }

    public SphereRTC(Vector3d position, double radius) {
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
