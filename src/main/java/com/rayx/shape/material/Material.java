package com.rayx.shape.material;

import com.rayx.opencl.CLTransferable;
import com.rayx.shape.Vector3d;

import java.nio.ByteBuffer;

public class Material implements CLTransferable {
    public static final long MATERIAL_REFLECTION = 0x1;
    public static final long MATERIAL_REFRACTION = 0x2;

    public static final Material DEFAULT_MATERIAL = reflectionMaterial(new Vector3d(0, 1, 0), 1);

    private final long type;
    private final Vector3d color;
    private final float lumen;
    private final float refractionIndex;

    private Material(long type, Vector3d color, float lumen, float refractionIndex) {
        this.type = type;
        this.color = color;
        this.lumen = lumen;
        this.refractionIndex = refractionIndex;
    }

    @Override
    public int bytesToInBuffer() {
        return Long.BYTES + 2 * Float.BYTES + Vector3d.BYTES;
    }

    @Override
    public void writeToByteBuffer(ByteBuffer buffer) {
        buffer.putLong(type);
        color.putInByteBuffer(buffer);
        buffer.putFloat(lumen);
        buffer.putFloat(refractionIndex);
    }

    public static Material reflectionMaterial(Vector3d color, float lumen) {
        return new Material(MATERIAL_REFLECTION, color, lumen, 0);
    }

    public static Material refractionMaterial(Vector3d color, float lumen, float refractionIndex) {
        return new Material(MATERIAL_REFRACTION, color, lumen, refractionIndex);
    }
}
