package com.rayx.shape;

import java.nio.ByteBuffer;

public class SubtractionSDF extends Shape {
    private final Shape shape1, shape2;

    public SubtractionSDF(Vector3d position, Shape shape1, Shape shape2) {
        super(position);
        this.shape1 = shape1;
        this.shape2 = shape2;
    }

    @Override
    public int getName() {
        return SUBTRACTION_SDF;
    }

    @Override
    public double getMaxRadius() {
        return Math.max(shape1.getMaxRadius() + shape1.getPosition().length(),
                shape2.getMaxRadius() + shape2.getPosition().length());
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        shape1.writeToByteBuffer(buffer);
        shape2.writeToByteBuffer(buffer);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + shape1.bytesToInBuffer() + shape2.bytesToInBuffer();
    }
}
