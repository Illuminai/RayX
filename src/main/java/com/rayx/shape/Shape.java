package com.rayx.shape;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Shape {
    // Marker interfaces
    public  interface ShapeRTC {
    }
    public interface ShapeSDF {
    }
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

    private final Vector3d position, rotation;
    private long id;
    private boolean shouldRender;
    private final List<Shape> subShapes;

    public Shape(Vector3d position, Vector3d rotation, List<Shape> subShapes) {
        this.position = position;
        this.rotation = rotation;
        this.subShapes = subShapes == null ? new ArrayList<>(0) : subShapes;
        shouldRender = false;
        id = -1;
    }

    public Vector3d getPosition() {
        return position;
    }

    /** Is the same for all shapes of a class*/
    public abstract int getName();

    public abstract double getMaxRadius();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setShouldRender(boolean shouldRender) {
        this.shouldRender = shouldRender;
    }

    public void writeToByteBuffer(ByteBuffer buffer) {
        buffer.
                putLong(getName()).
                putLong(getId()).
                putLong(shouldRender ? 1 : 0).
                putDouble(getMaxRadius()).
                putDouble(position.getX()).
                putDouble(position.getY()).
                putDouble(position.getZ()).
                putDouble(rotation.getX()).
                putDouble(rotation.getY()).
                putDouble(rotation.getZ());
    }

    /** Must be equal for every instance of a class */
    public int bytesToInBuffer() {
        return 7 * Double.BYTES + 3 * Long.BYTES;
    }

    public List<Shape> getSubShapes() {
        return subShapes;
    }
}
