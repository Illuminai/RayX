package com.rayx.shape.material;

import com.rayx.opencl.CLTransferable;
import com.rayx.shape.Vector3d;

import java.nio.ByteBuffer;

public class Material implements CLTransferable {
    public static final long MATERIAL_REFLECTION = 0x1;

    public static final Material DEFAULT_MATERIAL = new Material(MATERIAL_REFLECTION, new Vector3d(0, 1, 0), 0);

    private final long type;
    private final Vector3d color;
    private final float lumen;

    public Material(long type, Vector3d color, float lumen) {
        this.type = type;
        this.color = color;
        this.lumen = lumen;
    }

    @Override
    public int bytesToInBuffer() {
        return Long.BYTES + Float.BYTES + Vector3d.BYTES;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        buffer.putLong(type);
        color.putInByteBuffer(buffer);
        buffer.putFloat(lumen);
    }
}
