package com.rayx.shape;

import java.nio.ByteBuffer;

public class PlaneRTC extends Shape implements Shape.ShapeRTC{
    private final Vector3d normal;

    public PlaneRTC(Vector3d position, Vector3d normal) {
        super(position, null);
        this.normal = normal;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        buffer.
                putDouble(normal.getX()).
                putDouble(normal.getY()).
                putDouble(normal.getZ());
    }

    @Override
    public double getMaxRadius() {
        return -1;
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Double.BYTES * 3;
    }

    @Override
    public int getName() {
        return PLANE_RTC;
    }
}
