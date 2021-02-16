package com.rayx.opencl;

import com.rayx.core.math.Vector3d;
import com.rayx.scene.material.Material;
import com.rayx.scene.shape.Shape;
import com.rayx.scene.shape.ShapeType;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_MEM_WRITE_ONLY;

public class CLContext {
    public static final String KERNEL_GET_STRUCT_SIZE = "defaultKernelGetStructSize",
            KERNEL_FILL_BUFFER_DATA = "defaultKernelFillDataBuffer",
            KERNEL_RENDER = "defaultKernelRender",
            KERNEL_RENDER_DEBUG = "defaultKernelRenderDebug",
            KERNEL_BENCHMARK_DOUBLE = "defaultKernelBenchmarkDouble",
            KERNEL_BENCHMARK_FLOAT = "defaultKernelBenchmarkFloat";

    private final long device;
    private long context;
    private long commandQueue;
    private HashMap<String, CLKernel> kernels;
    private HashMap<String, CLProgram> programs;
    private HashMap<String, CLMemoryObject> memoryObjects;

    private String fileShapesH, fileShapesCL,
            fileJavaToClH, fileJavaToClCL, fileRenderCL;
    private String putShapesToMemoryFunctionHeader;

    private final List<ShapeType> registeredShapes;
    private long shapeTypeCount;

    private int baseShapeStructSize;

    /**
     * Only {@link CLManager} should create instances
     */
    CLContext(long device, long context, long commandQueue) {
        this.context = context;
        this.device = device;
        this.commandQueue = commandQueue;
        kernels = new HashMap<>();
        programs = new HashMap<>();
        memoryObjects = new HashMap<>();
        this.registeredShapes = new ArrayList<>();
        shapeTypeCount = 0;

        fileShapesH = null;
        fileShapesCL = null;
        fileJavaToClH = null;
        fileJavaToClCL = null;
        fileRenderCL = null;

        addDefaultShapes();
    }

    public void freeMemoryObject(String id) {
        CLMemoryObject p = getMemoryObject(id);
        p.delete();
        memoryObjects.remove(id);
    }

    public void freeAllMemoryObjects() {
        memoryObjects.values().forEach(CLMemoryObject::delete);
        memoryObjects.clear();
    }

    public void destroy() {
        kernels.values().forEach(CLKernel::destroy);
        kernels.clear();
        kernels = null;

        programs.values().forEach(CLProgram::destroy);
        programs.clear();
        programs = null;

        memoryObjects.values().forEach(CLMemoryObject::delete);
        memoryObjects.clear();
        memoryObjects = null;

        CLManager.freeCommandQueueInternal(commandQueue);
        commandQueue = 0;

        CLManager.destroyContextInternal(this);
        this.context = 0;
    }

    public void getMemoryObjectValue(String id, ByteBuffer destination) {
        getMemoryObject(id).getValue(destination);
    }

    public CLProgram getProgramObject(String id) {
        assert programs.containsKey(id) : "No program object: " + id;
        return programs.get(id);
    }

    public CLMemoryObject getMemoryObject(String id) {
        assert memoryObjects.containsKey(id) : "No memory object: " + id;
        return memoryObjects.get(id);
    }

    public void addMemoryObject(String id, CLMemoryObject memoryObject) {
        assert !memoryObjects.containsKey(id) : "Memory object already exists: " + id;
        memoryObjects.put(id, memoryObject);
    }

    public Map<String, CLMemoryObject> getAllMemoryObjects() {
        return memoryObjects;
    }

    public long getContext() {
        return context;
    }

    public long getDevice() {
        return device;
    }

    public long getCommandQueue() {
        return commandQueue;
    }

    public void destroyProgram(String name) {
        assert kernels.values().stream().
                noneMatch(k -> name.equals(k.getProgramId())) : "Kernels depend on program \"" + name + "\"";
        assert kernels.containsKey(name);
        kernels.remove(name).destroy();
    }

    public void addKernelObject(CLKernel kernel) {
        assert !kernels.containsKey(kernel.kernelId) : kernel.getKernelId();
        kernels.put(kernel.kernelId, kernel);
    }

    public void addProgramObject(CLProgram program) {
        assert !programs.containsKey(program.programId);
        programs.put(program.programId, program);
    }

    public CLKernel getKernelObject(String id) {
        assert kernels.containsKey(id);
        return kernels.get(id);
    }

    public void destroyKernel(String name) {
        assert kernels.containsKey(name);
        CLKernel k = kernels.remove(name);
        k.destroy();
    }

    public long getStructSize(long type) {
        if(type == -1) {
            return baseShapeStructSize;
        }
        return getShapeType(type).getStructSize();
    }

    public String getTypeName(long type) {
        return getShapeType(type).getShapeName();
    }

    private void addDefaultShapes() {
        registerShape("sphere", ShapeType.ShaderType.SHAPE,
                """
                return length(point) - 1;
                """);
        registerShape("torus",ShapeType.ShaderType.SHAPE, """
                float2 q =
                    (float2){length(point.yz) - shape->radiusBig, point.x};
                return length(q) - shape->radiusSmall;
                        """, new ShapeType.CLField("radiusSmall", ShapeType.CLFieldType.FLOAT)
                , new ShapeType.CLField("radiusBig", ShapeType.CLFieldType.FLOAT));
        registerShape("plane", ShapeType.ShaderType.SHAPE,"""
                return dot(point, shape->normal);
                """, new ShapeType.CLField("normal", ShapeType.CLFieldType.FLOAT3));
        //TODO optimize
        registerShape("box", ShapeType.ShaderType.SHAPE,"""
                float3 p = point;
                float3 dimensions = shape->dimensions;
                float3 q = fabs((float4){p, 0}).xyz - dimensions;
                return length(max(q,(float3)0.0)) + min((float)max(q.x,max(q.y,q.z)),(float)0.0);
                """, new ShapeType.CLField("dimensions", ShapeType.CLFieldType.FLOAT3));
        registerShape("octahedron", ShapeType.ShaderType.SHAPE,"""
                float3 p = fabs(point);
                float m = p.x + p.y + p.z - 1;
                float3 q;
                            
                if ( 3.0*p.x < m ) {
                    q = p.xyz;
                } else if ( 3.0*p.y < m ) {
                    q = p.yzx;
                } else if ( 3.0*p.z < m ) {
                    q = p.zxy;
                } else {
                    return m * 0.57735027;
                }
                            
                float k = clamp(0.5f*(q.z-q.y+1.f),0.0f,1.f);
                return length((float3){q.x,q.y-1.f+k,q.z-k});
                """);

        registerShape("subtraction", ShapeType.ShaderType.BOOLEAN_OPERATOR,"""
                    return max(-d1, d2);
                """, new ShapeType.CLField("shape1", ShapeType.CLFieldType.POINTER_SHAPE),
                new ShapeType.CLField("shape2", ShapeType.CLFieldType.POINTER_SHAPE));
        registerShape("union", ShapeType.ShaderType.BOOLEAN_OPERATOR,"""
                    return min(d1, d2);
                """, new ShapeType.CLField("shape1", ShapeType.CLFieldType.POINTER_SHAPE),
                new ShapeType.CLField("shape2", ShapeType.CLFieldType.POINTER_SHAPE));
        registerShape("intersection", ShapeType.ShaderType.BOOLEAN_OPERATOR,"""
                    return max(d1, d2);
                """, new ShapeType.CLField("shape1", ShapeType.CLFieldType.POINTER_SHAPE),
                new ShapeType.CLField("shape2", ShapeType.CLFieldType.POINTER_SHAPE));

        registerShape("mandelbulb", ShapeType.ShaderType.SHAPE,"""
                            float3 c = (float3) {shape->phi+1,shape->phi+1,shape->phi+1};
                                           float3 orbit = point;
                                               float dz = 1;
                                    
                                               for (int i = 0; i < 20; i++)
                                               {
                                                   float r = length(orbit);
                                                   float o = acos(orbit.z/r);
                                                   float p = atan(orbit.y/orbit.x);
                                    
                                                   dz = 8*r*r*r*r*r*r*r*dz;
                                    
                                                   r = r*r*r*r*r*r*r*r;
                                                   o = 8*o;
                                                   p = 8*p;
                                    
                                                   orbit = (float3){r*sin(o) * cos(p),
                                                           r*sin(o) * sin(p),
                                                           r*cos(o)} + c;
                                    
                                                   if (dot(orbit, orbit) > 4.0) break;
                                               }
                                               float z = length(orbit);
                                               return 0.5*z*log(z)/dz;
                        """, new ShapeType.CLField("phi", ShapeType.CLFieldType.FLOAT));
    }

    public void initialize() {
        StringBuilder compileOptions = new StringBuilder("-cl-fast-relaxed-math " +
                " -cl-kernel-arg-info" +
                " -Werror" +
                " -D EPSILON=((float)0.00001)" +
                " -D FLAG_SHOULD_RENDER=" + Shape.FLAG_SHOULD_RENDER +
                " -D SHAPE=-1" +
                " -D MATERIAL_REFLECTION=" + Material.MATERIAL_REFLECTION +
                " -D MATERIAL_REFRACTION=" + Material.MATERIAL_REFRACTION);
        for(ShapeType type: registeredShapes) {
            compileOptions.append(" -D ").append(type.getShapeLiteral()).append("=").append(type.getType());
        }
        System.out.println("Compile Options: " + compileOptions);
        generateCLFiles();
        compileFiles(compileOptions.toString());
    }

    private void compileFiles(String compileOptions) {
        createBenchmarks(compileOptions);

        //From least dependent to most dependent
        //----------- H E A D E R S -----------
        //math.h
        CLManager.putProgramFromFile(this, null,
                "clcode/default/headers/math.h",
                compileOptions);
        //shapes.h
        CLManager.putProgramFromString(this,
                new String[]{
                        "clcode/default/headers/math.h"
                },
                "clcode/default/headers/shapes.h",
                fileShapesH,
                compileOptions);
        //java_to_cl.h
        CLManager.putProgramFromString(this,
                new String[]{"clcode/default/headers/shapes.h",
                        "clcode/default/headers/math.h"},
                "clcode/default/headers/java_to_cl.h",
                    fileJavaToClH,
                compileOptions);
        //render.h
        CLManager.putProgramFromFile(this,
                new String[]{"clcode/default/headers/shapes.h",
                        "clcode/default/headers/math.h"},
                "clcode/default/headers/render.h",
                compileOptions);

        //----------- C O D E -----------
        //math.cl
        CLManager.putProgramFromFile(this, new String[]{
                        "clcode/default/headers/math.h"
                },
                "clcode/default/implementation/math.cl",
                compileOptions);
        //shapes.cl
        CLManager.putProgramFromString(this,
                new String[]{"clcode/default/headers/shapes.h",
                        "clcode/default/headers/math.h"},
                "clcode/default/implementation/shapes.cl",
                fileShapesCL,
                compileOptions);

        //java_to_cl.cl
        CLManager.putProgramFromString(this,
                new String[]{
                        "clcode/default/headers/math.h",
                        "clcode/default/headers/java_to_cl.h",
                        "clcode/default/headers/shapes.h"},
                "clcode/default/implementation/java_to_cl.cl",
                fileJavaToClCL,
                compileOptions);
        //render.cl
        CLManager.putProgramFromString(this,
                new String[]{"clcode/default/headers/render.h",
                        "clcode/default/headers/shapes.h",
                        "clcode/default/headers/math.h"},
                "clcode/default/implementation/render.cl",
                fileRenderCL,
                compileOptions);

        //----------- E X E C U T A B L E - P R O G R A M S -----------
        CLManager.putExecutableProgram(this,
                new String[]{
                        "clcode/default/implementation/shapes.cl",
                        "clcode/default/implementation/java_to_cl.cl",
                        "clcode/default/implementation/math.cl"
                },
                "javaToCLProgram");
        CLManager.putExecutableProgram(this,
                new String[]{
                        "clcode/default/implementation/shapes.cl",
                        "clcode/default/implementation/math.cl",
                        "clcode/default/implementation/render.cl"
                },
                "renderProgram");
        //----------- K E R N E L S -----------
        //Query struct sizes in opencl
        CLManager.putKernel(this, "getShapeSizes",
                KERNEL_GET_STRUCT_SIZE, "javaToCLProgram");

        //Transfer data from RAM to VRAM
        CLManager.putKernel(this, "putShapesInMemory",
                KERNEL_FILL_BUFFER_DATA, "javaToCLProgram");
        //Run renderer
        CLManager.putKernel(this, "render",
                KERNEL_RENDER, "renderProgram");

        // Debug preview
        CLManager.putKernel(this, "renderDebug",
                KERNEL_RENDER_DEBUG, "renderProgram");
        //----------- I N I T I A L I Z E -----------
        getStructSizes();
    }

    private void generateFileShapesH() {
        StringBuilder code = new StringBuilder();
        code.append(CLManager.readFromFile("/clcode/default/headers/shapes.h"));
        for(ShapeType t: registeredShapes) {
            code.append(t.generateStruct()).append('\n');

            code.append(t.generateShaderFunctionHeader()).append('\n');
        }

        code.append("#endif");

        this.fileShapesH = code.toString();
    }

    private void generateFileShapesCL() {
        StringBuilder code = new StringBuilder();
        code.append(CLManager.readFromFile("/clcode/default/implementation/shapes.cl"));
        for(ShapeType t: registeredShapes) {
            code.append(t.generateShaderFunction()).append('\n');
        }

        this.fileShapesCL = code.toString();
    }

    private void generatePutShapesToMemoryFunctionHeader() {
        StringBuilder code = new StringBuilder();

        code.append(
                "__kernel void putShapesInMemory(int numShapes, __global char* inputData, __global struct shape_t* shapes");

        for(ShapeType t: registeredShapes) {
            code.append(",\n__global struct ").
                    append(t.getStructName()).append(" * ").append(t.getDataPointerName());
        }
        code.append(')');
        putShapesToMemoryFunctionHeader = code.toString();
    }

    private void generateFileJavaToClH() {
        StringBuilder code = new StringBuilder();
        code.append(CLManager.readFromFile("/clcode/default/headers/java_to_cl.h"));

        code.append(putShapesToMemoryFunctionHeader).append(";\n");

        code.append("#endif");

        this.fileJavaToClH = code.toString();
    }

    private void generateFileJavaToClCL() {
        StringBuilder code = new StringBuilder();
        code.append(CLManager.readFromFile("/clcode/default/implementation/java_to_cl.cl"));

        code.append("""
                __kernel void getShapeSizes(int numShapes,
                                            __global int* shapesInQuestion,
                                            __global int* result) {
                    
                    for(int i = 0; i < numShapes; i++) {
                        int shape = shapesInQuestion[i];
                        switch(shape) {
                            case SHAPE: result[i] = sizeof(struct shape_t); break;""");

        for(ShapeType t: registeredShapes) {
            code.append("\n            case ").append(t.getShapeLiteral())
                    .append(": result[i] = sizeof(struct ")
                    .append(t.getStructName())
                    .append("); break;");
        }
        code.append("""
                                
                            default: result[i] = -1; break;
                        }
                    }
                }
                """);

        code.append(putShapesToMemoryFunctionHeader).append('{');

        code.append("""
                    for(int i = 0; i < numShapes; i++) {
                        for(int k = 0; k < sizeof(struct shape_t) / sizeof(float); k++) {
                            __global float* tmp= (__global float*)&shapes[i];
                            tmp[k] = -101.101;
                        }
                        long shape = getNextLong(inputData); inputData += sizeof(long);
                        long id = getNextLong(inputData); inputData += sizeof(long);
                        long flags = getNextLong(inputData); inputData += sizeof(long);
                        float3 position = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
                        float3 rotation = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
                        float size = getNextFloat(inputData); inputData += sizeof(float);
                        //Get Material
                        long materialType = getNextLong(inputData); inputData += sizeof(long);
                        float3 materialColor = getNextFloat3 (inputData); inputData += sizeof(float) * 3;
                        float materialLumen = getNextFloat(inputData); inputData += sizeof(float);
                        float materialRefractionIndex = getNextFloat(inputData); inputData += sizeof(float);
                        struct matrix3x3 rotMatrix = rotationMatrix(rotation);
                        struct material_t material;
                                
                        shapes[i] = (struct shape_t){shape, id, flags,
                                        (struct material_t) {
                                            materialType,
                                            materialColor,
                                            materialLumen,
                                            //Initialize just one union
                                            {.refraction = {materialRefractionIndex}}
                                        },
                                        position, rotation,
                                        size,
                                        rotMatrix,
                                        inverse(rotMatrix),
                                    0};
                        
                        switch(shape) {
                """);

        for(ShapeType t: registeredShapes) {
            code.append("\n            ");
            code.append("case ").append(t.getShapeLiteral()).append(":\n");
            code.append(t.generateCLConverter()).append("\nbreak;");
        }

        code.append("""
                    }
                }
            }
            """);//End switch, for-loop, and function

        this.fileJavaToClCL = code.toString();
    }

    private void generateFileRenderCL() {
        StringBuilder code = new StringBuilder();
        code.append(CLManager.readFromFile("/clcode/default/implementation/render.cl"));
        // TODO define max stack size
        code.append("""
                
                float oneStepSDF(float3 point, __global struct shape_t* shape) {
                    int index = 0;
                    struct oneStepSDFArgs_t stack[10];
                    stack[0] = (struct oneStepSDFArgs_t){point, shape, 0, 0, 0};
                    do {
                        float3 point = stack[index].point;
                        __global struct shape_t* shape = stack[index].shape;
                        switch(shape->type) {""");

        for(ShapeType t: registeredShapes) {
            code.append("\n            case ").append(t.getShapeLiteral()).append(":\n");
            switch (t.getShaderType()) {
                case SHAPE -> {
                    code.append("                stack[index].d1 = ");
                    code.append(t.getFunctionName()).append("(point/shape->size, shape->shape) * shape->size;\n");
                    code.append("""
                                                index--;
                                                continue;
                                """);
                }
                case BOOLEAN_OPERATOR -> {
                    code.append("""
                                        {
                                        __global struct shape_t* shape1 =
                                            ((__global struct intersection_t*)shape->shape)->shape1;
                                        __global struct shape_t* shape2 =
                                            ((__global struct intersection_t*)shape->shape)->shape2;
                                        switch(stack[index].status) {
                                            case 0:
                                                stack[index].status = 1;
                                                index++;
                                                stack[index] = (struct oneStepSDFArgs_t) {
                                                    matrixTimesVector(shape1->rotationMatrix,
                                                        point/shape->size - shape1->position),
                                                    shape1,
                                                    0, 0, 0
                                                };
                                                continue;
                                            case 1:
                                                stack[index].status = 2;
                                                stack[index].d1 = stack[index + 1].d1 * shape->size;
                                                index++;
                                                stack[index] = (struct oneStepSDFArgs_t) {
                                                    matrixTimesVector(shape2->rotationMatrix,
                                                        point/shape->size - shape2->position),
                                                    shape2,
                                                    0, 0, 0
                                                };
                                                continue;
                                            case 2:
                                                stack[index].d2 = stack[index + 1].d1 * shape->size;
                                                stack[index].d1 = """);
                    code.append(t.getFunctionName()).append("(stack[index].d1, stack[index].d2, shape->shape);\n");
                    code.append("""
                                            index--;
                                            continue;
                                        }}
                            """);
                }
            }
        }

        code.append("""
                            default:
                                return 0;
                        }
                    } while(index >= 0);
                    return stack[0].d1;
                }
                """);//End switch, do-while and function
        this.fileRenderCL = code.toString();
    }

    private void generateCLFiles() {
        generateFileShapesH();
        generateFileShapesCL();
        generatePutShapesToMemoryFunctionHeader();
        generateFileJavaToClH();
        generateFileJavaToClCL();
        generateFileRenderCL();
    }

    public void createBenchmarks(String compileOptions) {
        CLManager.putProgramFromFile(this, null,
                "clcode/default/headers/benchmark.h",
                compileOptions);
        CLManager.putProgramFromFile(this,
                new String[]{"clcode/default/headers/benchmark.h"},
                "clcode/default/implementation/benchmark.cl",
                compileOptions);

        CLManager.putExecutableProgram(this,
                new String[]{
                        "clcode/default/implementation/benchmark.cl"
                },
                "benchmarkProgram");

        CLManager.putKernel(this, "testPerformanceDouble",
                KERNEL_BENCHMARK_DOUBLE, "benchmarkProgram");
        CLManager.putKernel(this, "testPerformanceFloat",
                KERNEL_BENCHMARK_FLOAT, "benchmarkProgram");
    }

    public void runBenchmarks() {
        System.out.println("Running benchmarks...");
        final int amount = 20000;

        double[] vals = new Random(1).doubles(4 * amount).toArray();

        float[] f = new float[vals.length];
        for (int i = 0; i < f.length; i++) {
            f[i] = (float) vals[i];
        }

        CLManager.allocateMemory(this, CL_MEM_READ_ONLY, vals, "benchmarkDoubleIn");
        CLManager.allocateMemory(this, CL_MEM_READ_ONLY, f, "benchmarkFloatIn");
        CLManager.allocateMemory(this, CL_MEM_WRITE_ONLY,
                amount * Double.BYTES, "benchmarkDoubleOut");
        CLManager.allocateMemory(this, CL_MEM_WRITE_ONLY,
                amount * Float.BYTES, "benchmarkFloatOut");

        CLKernel dKern = getKernelObject(KERNEL_BENCHMARK_DOUBLE);
        dKern.setParameterPointer(0, "benchmarkDoubleIn");
        dKern.setParameterPointer(1, "benchmarkDoubleOut");
        dKern.setParameter1i(2, amount);

        CLKernel fKern = getKernelObject(KERNEL_BENCHMARK_FLOAT);
        fKern.setParameterPointer(0, "benchmarkFloatIn");
        fKern.setParameterPointer(1, "benchmarkFloatOut");
        fKern.setParameter1i(2, amount);

        dKern.run(new long[]{1}, null);
        fKern.run(new long[]{1}, null);

        long t = System.currentTimeMillis();
        dKern.run(new long[]{amount}, null);
        System.out.println("Double: " + (System.currentTimeMillis() - t));
        /*{
            ByteBuffer dbuffer =
                    BufferUtils.createByteBuffer(amount * Double.BYTES);
            this.getMemoryObject("benchmarkDoubleOut").getValue(dbuffer);
            double[] dres = new double[amount];
            System.out.println("Double input : " + Arrays.toString(f));
            dbuffer.asDoubleBuffer().get(dres);
            System.out.println("Double result: " + Arrays.toString(dres));
        }*/

        t = System.currentTimeMillis();
        fKern.run(new long[]{amount}, null);
        System.out.println("Float : " + (System.currentTimeMillis() - t));
        /*{
            ByteBuffer fbuffer =
                    BufferUtils.createByteBuffer(amount * Float.BYTES);
            this.getMemoryObject("benchmarkFloatOut").getValue(fbuffer);
            float[] fres = new float[amount];
            System.out.println("Float input : " + Arrays.toString(f));
            fbuffer.asFloatBuffer().get(fres);
            System.out.println("Float result: " + Arrays.toString(fres));
        }*/

        freeMemoryObject("benchmarkDoubleIn");
        freeMemoryObject("benchmarkDoubleOut");
        freeMemoryObject("benchmarkFloatIn");
        freeMemoryObject("benchmarkFloatOut");
    }

    public void registerShape(String shapeName, ShapeType.ShaderType type,
                              String distanceShader, ShapeType.CLField... fields) {
        registeredShapes.add(new ShapeType(shapeName, shapeTypeCount++, type,
                distanceShader, fields));
    }

    public List<ShapeType> getRegisteredShapes() {
        return registeredShapes;
    }

    public ShapeType getShapeType(long type) {
        for(ShapeType t: registeredShapes) {
            if(t.getType() == type) {
                return t;
            }
        }
        throw new RuntimeException("Type not found: " + type);
    }

    private void getStructSizes() {
        int[] structs = new int[registeredShapes.size() + 1];
        for(int i = 0; i < registeredShapes.size(); i++) {
            structs[i] = (int) registeredShapes.get(i).getType();
        }
        structs[structs.length -1] = -1;

        CLContext.CLKernel kernelStruct =
                getKernelObject(CLContext.KERNEL_GET_STRUCT_SIZE);
        CLManager.allocateMemory(this, CL_MEM_READ_ONLY,
                structs,
                "shapesInQuestion");
        CLManager.allocateMemory(this, CL_MEM_WRITE_ONLY,
                (long) Integer.BYTES * structs.length,
                "result");
        kernelStruct.setParameter1i(0, structs.length);
        kernelStruct.setParameterPointer(
                1, "shapesInQuestion");
        kernelStruct.setParameterPointer(
                2, "result");

        kernelStruct.run(new long[]{1}, null);

        //TODO use MemoryStack instead of BufferUtils
        ByteBuffer buffer = BufferUtils.createByteBuffer(Integer.BYTES * structs.length);
        getMemoryObject("result").getValue(buffer);

        int[] result = new int[structs.length];
        buffer.asIntBuffer().get(result);

        for (int i = 0; i < registeredShapes.size(); i++) {
            getShapeType(structs[i]).setStructSize(result[i]);
            assert result[i] != -1: "Struct with size -1. " +
                    "The kernel does not recognize struct with type " + structs[i];
        }
        this.baseShapeStructSize = result[result.length -1];
        assert result[result.length -1] != -1: "shape_t has unknown size";

        freeMemoryObject("result");
        freeMemoryObject("shapesInQuestion");
    }

    public class CLMemoryObject {
        /** Can point to zero */
        private long pointer;
        //In bytes
        //If size is -1, it indicates that the memory object should not be read to the main memory
        //This is the case when
        private final long size;

        public CLMemoryObject(long pointer, long size) {
            this.pointer = pointer;
            this.size = size;
        }

        public void getValue(ByteBuffer destination) {
            assert this.size == destination.remaining() : size +
                    " " + destination.remaining();
            CLManager.readMemoryInternal(commandQueue, this.pointer, destination);
        }

        public void acquireFromGL() {
            assert size == -1;
            CLManager.acquireFromGLInternal(commandQueue, pointer);
        }

        public void releaseFromGL() {
            assert size == -1;
            CLManager.releaseFromGLInternal(commandQueue, pointer);
        }

        public void releaseFromGL(PointerBuffer eventWaitList, PointerBuffer event) {
            assert size == -1;
            CLManager.releaseFromGLInternal(commandQueue, pointer, eventWaitList, event);
        }

        private void delete() {
            CLManager.freeMemoryInternal(pointer);
            pointer = 0;
        }

        public long getPointer() {
            return pointer;
        }

        public long getSize() {
            return size;
        }
    }

    public class CLProgram {
        private final String programId;
        private long program;
        /**
         * A kernel can only be created from a program if <code>linked</code> is set to <code>true</code>
         */
        private final boolean linked;

        /**
         * @param programId Name of the program.
         *                  Should be name of the file it was generated from,
         *                  so that it may used for include directives in OpenCL C
         */
        public CLProgram(String programId, long program, boolean linked) {
            this.programId = programId;
            this.program = program;
            this.linked = linked;
        }

        public String getProgramId() {
            return programId;
        }

        public long getProgram() {
            return program;
        }

        private void destroy() {
            CLManager.destroyCLProgramInternal(program);
            program = 0;
        }

        public boolean isLinked() {
            return linked;
        }
    }

    public class CLKernel {
        private final String kernelId, programId;
        private long kernel;

        public CLKernel(String kernelId, String programId, long kernel) {
            this.kernelId = kernelId;
            this.programId = programId;
            this.kernel = kernel;
        }

        public void setParameterPointer(int index, String memoryObject) {
            CLManager.setParameterPointerInternal(CLContext.this, kernel, index, memoryObject);
        }

        public void setParameter1i(int index, int value) {
            CLManager.setParameter1iInternal(kernel, index, value);
        }

        public void setParameter4f(int index, float d0, float d1, float d2, float d3) {
            CLManager.setParameter4fInternal(kernel, index, d0, d1, d2, d3);
        }

        public void setParameter1f(int index, float d0) {
            CLManager.setParameter1fInternal(kernel, index, d0);
        }

        public void run(long[] globalWorkSize, long[] localWorkSize) {
            CLManager.runKernelInternal(
                    kernel, commandQueue, globalWorkSize, localWorkSize
            );
        }

        public long enqueue(long[] globalWorkSize, long[] localWorkSize, PointerBuffer eventWaitList) {
            return CLManager.enqueueKernelInternal(
                    kernel, commandQueue, globalWorkSize, localWorkSize, eventWaitList
            );
        }

        private void destroy() {
            CLManager.destroyCLKernelInternal(kernel);
            this.kernel = 0;
        }

        public long getKernel() {
            return kernel;
        }

        public String getProgramId() {
            return programId;
        }

        public String getKernelId() {
            return kernelId;
        }
    }
}
