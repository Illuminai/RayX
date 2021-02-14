package com.rayx.scene.shape.sdf;

import com.rayx.core.math.Vector3d;
import com.rayx.scene.material.Material;
import com.rayx.scene.shape.Shape;

import java.nio.ByteBuffer;

public class Sphere extends Shape {

    private float radius;

    public Sphere(float x, float y, float z, float radius) {
        this(new Vector3d(x, y, z), radius);
    }

    public Sphere(Vector3d position, float radius) {
        super(position, new Vector3d(0, 0, 0), null);
        this.radius = radius;
    }

    public Sphere(Vector3d position, float radius, Material material) {
        super(position, new Vector3d(0, 0, 0), material, null);
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

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}
