package com.rayx.scene.shape.sdf;

import com.rayx.core.math.Vector3d;
import com.rayx.scene.material.Material;
import com.rayx.scene.shape.Shape;

import java.nio.ByteBuffer;

public class Plane extends Shape {
    private final Vector3d normal;

    public Plane(Vector3d position, Vector3d normal, Material material) {
        super(position, new Vector3d(0, 0, 0), material, null);
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
    public int getShape() {
        return PLANE;
    }
}
