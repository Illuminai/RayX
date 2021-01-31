package com.rayx.shape;

import java.nio.ByteBuffer;

public class Vector3d {
    private final float x, y, z;

    public Vector3d(double x, double y, double z) {
        this((float) x, (float) y, (float) z);
    }

    public Vector3d(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3d add(Vector3d vector) {
        return new Vector3d(x + vector.x, y + vector.y, z + vector.z);
    }

    public Vector3d scale(double scalar) {
        return new Vector3d(x * scalar, y * scalar, z * scalar);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public Vector3d normalized() {
        float l = length();
        if(l != 0) {
            return new Vector3d(x / l, y / l, z / l);
        } else {
            //Return null vector
            return this;
        }
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public void putInByteBuffer(ByteBuffer buffer) {
        buffer.putFloat(x).putFloat(y).putFloat(z);
    }
}
