package com.rayx.scene.shape;

import com.rayx.core.math.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ShapeType {
    public enum ShaderType {
        SHAPE, BOOLEAN_OPERATOR
    }
    
    public enum CLFieldType {
        FLOAT("float", Float.BYTES),
        FLOAT3("float3", Vector3d.BYTES),
        POINTER_SHAPE("__global struct shape_t *", Long.BYTES);
        final String clType;
        final int size;
        CLFieldType(String clType, int size) {
            this.clType = clType;
            this.size = size;
        }
    }
    public static class CLField {
        private final String name;
        private final CLFieldType type;

        public CLField(String name, CLFieldType type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public CLFieldType getType() {
            return type;
        }

        @Override
        public String toString() {
            return "CLField{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    '}';
        }
    }

    private String shapeName;
    private String shapeLiteral;
    private final long type;
    private String structName;
    private String dataPointerName;
    private String functionName;
    private String functionHeader;
    private final String shaderBody;
    private final List<CLField> fields;
    private final ShaderType shaderType;
    
    private long structSize;

    public ShapeType(String shapeName, long type, ShaderType shaderType, String shaderFunctionBody, CLField[] fields) {
        this.type = type;
        this.shaderType = shaderType;
        this.shaderBody = shaderFunctionBody;
        this.fields = new ArrayList<>(Arrays.asList(fields));
        structSize = -1;
        setShapeName(shapeName);
    }

    public long getStructSize() {
        return structSize;
    }

    public void setStructSize(long structSize) {
        this.structSize = structSize;
    }

    public void setShapeName(String shapeName) {
        String capitalizedName = shapeName.substring(0,1).toUpperCase(Locale.ROOT) + shapeName.substring(1);
        this.shapeName = shapeName;
        this.shapeLiteral = shapeName.toUpperCase(Locale.ROOT);
        this.structName = shapeName + "_t";
        this.dataPointerName = "data" + capitalizedName;
        this.functionName = switch (shaderType) {
            case SHAPE -> "distanceShader" + capitalizedName;
            case BOOLEAN_OPERATOR -> "booleanOperator"  + capitalizedName;
        };
        this.functionHeader = switch (shaderType) {
            case SHAPE ->
                    "float " + functionName + "(float3 point, __global struct " +  structName + " * shape)";
            case BOOLEAN_OPERATOR -> "float " + functionName + "(float d1, float d2, __global struct " +  structName + " * shape)";
        };
    }

    public String generateShaderFunction() {
        return functionHeader + "{\n" + shaderBody + "\n}";
    }

    public String generateShaderFunctionHeader() {
        return functionHeader + ";";

    }

    public String generateStruct() {
        StringBuilder builder = new StringBuilder();

        builder.append("struct ").append(structName).append(" {\n");

        for(CLField f: fields) {
            builder.append(f.type.clType).append(" ").append(f.name).append(";\n");
        }

        builder.append("};");
        return builder.toString();
    }

    public String generateCLConverter() {
        StringBuilder builder = new StringBuilder();
        builder.append("shapes[i].shape = ").append(dataPointerName).append(";");

        for(CLField f: fields) {
            builder.append('\n').append(dataPointerName).append("->").append(f.name).append(" = ").append(
                    switch (f.type) {
                        case FLOAT -> "getNextFloat(inputData); inputData += sizeof(float); ";
                        case FLOAT3 -> "getNextFloat3 (inputData); inputData += sizeof(float) * 3; ";
                        case POINTER_SHAPE -> "shapes + getNextLong(inputData); inputData += sizeof(long); ";
                    }
            );
        }

        builder.append("\n").append(dataPointerName).append("++;");

        return builder.toString();
    }

    public String getShapeName() {
        return shapeName;
    }

    public String getShapeLiteral() {
        return shapeLiteral;
    }

    public long getType() {
        return type;
    }

    public String getStructName() {
        return structName;
    }

    /** Name of the pointer where the data is. Valid on host and cl-device side*/
    public String getDataPointerName() {
        return dataPointerName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getShaderBody() {
        return shaderBody;
    }

    public ShaderType getShaderType() {
        return shaderType;
    }

    public List<CLField> getFields() {
        return fields;
    }

    public int getTransferSize() {
        return fields.stream().mapToInt(u -> u.getType().size).sum();
    }
}
