package com.diamon.civil.engine;

public class NativeFeaCore {
    static {
        System.loadLibrary("calculoestructural");
    }

    // Lifecycle
    public native long createModel();
    public native void deleteModel(long ptr);
    public native void clearModel(long ptr);

    // Serialization
    public native String modelToInp(long ptr);
    public native String modelToJson(long ptr);
    public native void modelFromJson(long ptr, String json);

    // Project Storage
    public native boolean saveProject(String path, long ptr);
    public native boolean loadProject(String path, long ptr);

    // Execution
    public native String runCalculix(String workDir, String libDir, String jobName, long ptr);
}
