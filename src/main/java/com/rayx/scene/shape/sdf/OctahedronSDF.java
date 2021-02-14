package com.rayx.scene.shape.sdf;

import com.rayx.core.math.Vector3d;
import com.rayx.scene.shape.Shape;

import java.nio.ByteBuffer;

public class OctahedronSDF extends Shape {

    private float size;

    public OctahedronSDF(Vector3d position, float size) {
        super(position, new Vector3d(0, 0, 0), null);
        this.size = size;
    }

    @Override
    public int getShape() {
        return OCTAHEDRON;
    }

    @Override
    public float getMaxRadius() {
        return new Vector3d(size,size,size).length();
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        buffer.putFloat(size);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Float.BYTES;
    }
}
