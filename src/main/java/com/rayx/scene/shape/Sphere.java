package com.rayx.scene.shape;

import com.rayx.core.math.Vector3d;

import java.nio.ByteBuffer;

public class Sphere extends Shape {
    private final float radius;

    public Sphere(float x, float y, float z, float radius) {
        this(new Vector3d(x, y, z), radius);
    }

    public Sphere(Vector3d position, float radius) {
        super(position, new Vector3d(0, 0, 0), null);
        this.radius = radius;
    }

    @Override
    public float getMaxRadius() {
        return radius;
    }

    @Override
    public int getShape() {
        return SPHERE;
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
