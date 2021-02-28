package com.rayx.scene.shape;

import com.rayx.core.math.Vector3d;
import com.rayx.opencl.CLTransferable;
import com.rayx.scene.material.Material;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Shape implements CLTransferable {
    public static final long FLAG_SHOULD_RENDER = 1 << 0;

    private Vector3d position, rotation;
    private float size;
    private long id;
    private long flags;
    private final Material material;

    private final ShapeType type;
    private final HashMap<String, Object> properties;

    public Shape(ShapeType type, Vector3d position, Vector3d rotation) {
        this(type, 1, position, rotation, Material.DEFAULT_MATERIAL);
    }

    public Shape(ShapeType type, float size, Vector3d position, Vector3d rotation, Material material) {
        this.type = type;
        this.size = size;
        this.position = position;
        this.rotation = rotation;
        this.material = material;
        flags = 0;
        id = -1;
        this.properties = new HashMap<>();
    }

    public void setPosition(Vector3d position) {
        this.position = position;
    }

    public void setRotation(Vector3d rotation) {
        this.rotation = rotation;
    }

    public Vector3d getRotation() {
        return rotation;
    }

    public Vector3d getPosition() {
        return position;
    }

    public ShapeType getType() {
        return type;
    }

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

    public List<Shape> getSubShapes() {
        ArrayList<Shape> l = new ArrayList<>();
        for(Object o: properties.values()) {
            if(o instanceof Shape) {
                l.addAll(((Shape) o).getSubShapes());
                l.add((Shape) o);
            }
        }
        return l;
    }

    public void writeToByteBuffer(ByteBuffer buffer) {
        assert isValid() :
                "Properties do not match: " + this.properties + " " + type.getFields();
        buffer.
                putLong(getType().getType()).
                putLong(getId()).
                putLong(flags);
        position.putInByteBuffer(buffer);
        rotation.putInByteBuffer(buffer);
        buffer.putFloat(size);
        material.writeToByteBuffer(buffer);
        for(ShapeType.CLField field: type.getFields()) {
            Object val = properties.get(field.getName());
            switch (field.getType()) {
                case FLOAT -> buffer.putFloat((float)val);
                case FLOAT3 -> ((Vector3d)val).putInByteBuffer(buffer);
                case POINTER_SHAPE -> buffer.putLong(((Shape)val).getId());
            }
        }
    }

    //Checks if the properties are all matched
    private boolean isValid() {
        if(properties.size() != type.getFields().size()) {
            return false;
        }
        for (ShapeType.CLField field : type.getFields()) {
            if(!properties.containsKey(field.getName())) {
                return false;
            }
        }
        return switch (type.getShaderType()) {
            case SHAPE -> true;
            case SHAPE_COMBINATION_OF_2 -> type.getFields().stream().anyMatch(u -> u.getName().equals("shape1"))
                    && type.getFields().stream().anyMatch(u -> u.getName().equals("shape2"));
            case POINT_OPERATOR -> type.getFields().stream().anyMatch(u -> u.getName().equals("shape1"));
        };
    }

    public int bytesToInBuffer() {
        return Float.BYTES + 2 * Vector3d.BYTES + 3 * Long.BYTES + material.bytesToInBuffer() + type.getTransferSize();
    }

    public Shape setProperty(String name, float val) {
        properties.put(name, val);
        return this;
    }

    public Shape setProperty(String name, double val) {
        setProperty(name, (float)val);
        return this;
    }

    public Shape setProperty(String name, Vector3d val) {
        properties.put(name, val);
        return this;
    }

    public Shape setProperty(String name, Shape val) {
        properties.put(name, val);
        return this;
    }

    public HashMap<String, Object> getProperties() {
        return properties;
    }

    public void setSize(float size) {
        this.size = size;
    }
}
