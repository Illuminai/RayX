package com.rayx.shape;

import java.nio.ByteBuffer;

public class PlaneRTC extends Shape implements Shape.ShapeRTC{
    private final Vector3d normal;

    public PlaneRTC(Vector3d position, Vector3d normal) {
        super(position, new Vector3d(0,0,0), null);
        this.normal = normal;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        normal.putInByteBuffer(buffer);
    }

    @Override
    public float getMaxRadius() {
        return -1;
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Float.BYTES * 3;
    }

    @Override
    public int getName() {
        return PLANE_RTC;
    }
}
