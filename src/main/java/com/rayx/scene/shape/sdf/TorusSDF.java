package com.rayx.scene.shape.sdf;

import com.rayx.core.math.Vector3d;
import com.rayx.scene.shape.Shape;

import java.nio.ByteBuffer;

public class TorusSDF extends Shape {

    private float smallRadius, bigRadius;

    public TorusSDF(Vector3d position, Vector3d rotation, float smallRadius, float bigRadius) {
        super(position, rotation, null);
        this.smallRadius = smallRadius;
        this.bigRadius = bigRadius;
    }

    @Override
    public int getShape() {
        return TORUS;
    }

    @Override
    public float getMaxRadius() {
        return smallRadius + bigRadius;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        buffer.
                putFloat(smallRadius).
                putFloat(bigRadius);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + Float.BYTES * 2;
    }
}
