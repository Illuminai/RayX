package com.rayx.shape;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class UnionSDF extends Shape implements Shape.ShapeSDF {
    private final Shape shape1, shape2;

    public UnionSDF(Vector3d position, Vector3d rotation, Shape shape1, Shape shape2) {
        super(position, rotation, Arrays.asList(shape1, shape2));
        this.shape1 = shape1;
        this.shape2 = shape2;
    }

    @Override
    public int getName() {
        return UNION_SDF;
    }

    @Override
    public double getMaxRadius() {
        return Math.max(shape1.getMaxRadius() + shape1.getPosition().length(),
                shape2.getMaxRadius() + shape2.getPosition().length());
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        buffer.putLong(shape1.getId());
        buffer.putLong(shape2.getId());
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + 2 * Long.BYTES;
    }
}
