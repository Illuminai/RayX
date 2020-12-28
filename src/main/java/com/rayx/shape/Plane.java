package com.rayx.shape;

import java.nio.ByteBuffer;

public class Plane extends Shape {
    private final Vector3d position, normal;

    public Plane(Vector3d position, Vector3d normal) {
        this.position = position;
        this.normal = normal;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        buffer.
                putDouble(position.getX()).
                putDouble(position.getY()).
                putDouble(position.getZ()).
                putDouble(normal.getX()).
                putDouble(normal.getY()).
                putDouble(normal.getZ());
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Double.BYTES * 6;
    }

    @Override
    public int getName() {
        return PLANE_RTC;
    }
}
