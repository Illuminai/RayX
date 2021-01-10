package com.rayx.shape;

import java.nio.ByteBuffer;

public class BoxSDF extends Shape implements Shape.ShapeSDF {
    private final Vector3d dimensions;

    public BoxSDF(Vector3d position, Vector3d rotation, Vector3d dimensions) {
        super(position, rotation, null);
        this.dimensions = dimensions;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        super.writeToByteBuffer(buffer);
        dimensions.putInByteBuffer(buffer);
    }

    @Override
    public int bytesToInBuffer() {
        return super.bytesToInBuffer() + 3 * Double.BYTES;
    }

    @Override
    public int getName() {
        return BOX_SDF;
    }

    @Override
    public double getMaxRadius() {
        //TODO
        return 10;
    }
}
