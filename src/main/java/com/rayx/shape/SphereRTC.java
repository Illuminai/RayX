package com.rayx.shape;

import java.nio.ByteBuffer;

public class SphereRTC extends Shape implements Shape.ShapeRTC {
    private final float radius;

    public SphereRTC(float x, float y, float z, float radius) {
        this(new Vector3d(x, y, z), radius);
    }

    public SphereRTC(Vector3d position, float radius) {
        super(position, new Vector3d(0,0,0), null);
        this.radius = radius;
    }

    @Override
    public float getMaxRadius() {
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
                putFloat(radius);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Float.BYTES;
    }
}
