package com.rayx.shape;

import com.rayx.opencl.CLTransferable;
import com.rayx.shape.material.Material;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class Shape implements CLTransferable {
    public static final long FLAG_SHOULD_RENDER = 1 << 0;

    /**
     * Used to determine the size of shape_t
     */
    public static final int SHAPE = 0x0;
    /**
     * A sphere, rendered using raytracing
     */
    public static final int SPHERE = 0x1;
    /**
     * A torus, rendered using sdf
     */
    public static final int TORUS = 0x2;
    /**
     * A plane, rendered using raytracing
     */
    public static final int PLANE = 0x3;
    /**
     * A subtraction SDF
     */
    public static final int SUBTRACTION = 0x4;
    /**
     * A box, render using SDF
     */
    public static final int BOX = 0x5;
    /**
     * An union SDF
     */
    public static final int UNION = 0x6;
    /**
     * An intersection SDF
     */
    public static final int INTERSECTION = 0x7;

    private final Vector3d position, rotation;
    private long id;
    private long flags;
    private final Material material;
    private final List<Shape> subShapes;

    public Shape(Vector3d position, Vector3d rotation, List<Shape> subShapes) {
        this(position, rotation, Material.DEFAULT_MATERIAL, subShapes);
    }

    public Shape(Vector3d position, Vector3d rotation) {
        this(position, rotation, Material.DEFAULT_MATERIAL, null);
    }

    public Shape(Vector3d position, Vector3d rotation, Material material, List<Shape> subShapes) {
        this.position = position;
        this.rotation = rotation;
        this.material = material;
        this.subShapes = subShapes == null ? new ArrayList<>(0) : subShapes;
        flags = 0;
        id = -1;
    }

    public Vector3d getPosition() {
        return position;
    }

    /**
     * Is the same for all shapes of a class
     */
    public abstract int getShape();

    public abstract float getMaxRadius();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setShouldRender(boolean shouldRender) {
        if (shouldRender) {
            flags |= FLAG_SHOULD_RENDER;
        } else {
            flags &= ~FLAG_SHOULD_RENDER;
        }
    }

    public void writeToByteBuffer(ByteBuffer buffer) {
        buffer.
                putLong(getShape()).
                putLong(getId()).
                putLong(flags).
                putFloat(getMaxRadius());
        position.putInByteBuffer(buffer);
        rotation.putInByteBuffer(buffer);
        material.writeToByteBuffer(buffer);
    }

    /**
     * Must be equal for every instance of a class
     */
    public int bytesToInBuffer() {
        return Float.BYTES + 2 * Vector3d.BYTES + 3 * Long.BYTES + material.bytesToInBuffer();
    }

    public List<Shape> getSubShapes() {
        return subShapes;
    }
}
