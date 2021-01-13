package com.rayx.shape;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class Shape {
    // Marker interfaces
    public  interface ShapeRTC {
    }
    public interface ShapeSDF {
    }

    public static final long FLAG_SHOULD_RENDER = 1 << 0;

    /** Used to determine the size of shape_t */
    public static final int SHAPE = 0x0;
    /** A sphere, rendered using raytracing*/
    public static final int SPHERE_RTC = 0x1;
    /** A torus, rendered using sdf*/
    public static final int TORUS_SDF = 0x2;
    /** A plane, rendered using raytracing*/
    public static final int PLANE_RTC = 0x3;
    /** A subtraction SDF*/
    public static final int SUBTRACTION_SDF = 0x4;
    /** A box, render using SDF */
    public static final int BOX_SDF = 0x5;
    /** An union SDF */
    public static final int UNION_SDF = 0x6;
    /** An intersection SDF */
    public static final int INTERSECTION_SDF = 0x7;

    private final Vector3d position, rotation;
    private long id;
    private long flags;
    private final List<Shape> subShapes;
    private float lumen;

    public Shape(Vector3d position, Vector3d rotation, List<Shape> subShapes) {
        this.position = position;
        this.rotation = rotation;
        this.subShapes = subShapes == null ? new ArrayList<>(0) : subShapes;
        flags = 0;
        id = -1;
    }

    public Vector3d getPosition() {
        return position;
    }

    /** Is the same for all shapes of a class*/
    public abstract int getName();

    public abstract float getMaxRadius();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setShouldRender(boolean shouldRender) {
        if(shouldRender) {
            flags |= FLAG_SHOULD_RENDER;
        } else {
            flags &= ~FLAG_SHOULD_RENDER;
        }
    }

    public float getLumen() {
        return lumen;
    }

    public void setLumen(float lumen) {
        this.lumen = lumen;
    }

    public void writeToByteBuffer(ByteBuffer buffer) {
        buffer.
                putLong(getName()).
                putLong(getId()).
                putLong(flags).
                putFloat(getMaxRadius()).
                putFloat(lumen);
        position.putInByteBuffer(buffer);
        rotation.putInByteBuffer(buffer);
    }

    /** Must be equal for every instance of a class */
    public int bytesToInBuffer() {
        return 8 * Float.BYTES + 3 * Long.BYTES;
    }

    public List<Shape> getSubShapes() {
        return subShapes;
    }
}
