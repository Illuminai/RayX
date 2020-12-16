package com.rayx.shape;

import com.rayx.opencl.CLObjectTransfer;

import java.nio.ByteBuffer;

public class Sphere implements CLObjectTransfer.Transferable {
    private final Vector3d position;
    private final double radius;

    public Sphere(Vector3d position, double radius) {
        this.position = position;
        this.radius = radius;
    }

    @Override
    public void transferTo(ByteBuffer destination) {
        destination.
                putDouble(position.getX()).
                putDouble(position.getY()).
                putDouble(position.getZ()).
                putDouble(radius);
    }
}
