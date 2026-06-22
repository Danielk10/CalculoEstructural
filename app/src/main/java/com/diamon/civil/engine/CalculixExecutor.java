package com.diamon.civil.engine;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CalculixExecutor {
    private static final String TAG = "CalculixExecutor";
    private final File workDir;
    private final File nativeLibDir;

    public CalculixExecutor(Context context) {
        this.workDir = context.getFilesDir();
        this.nativeLibDir = new File(context.getApplicationInfo().nativeLibraryDir);
    }

    public String executeCalculix(String jobName) {
        return executeBinary("ccx", jobName);
    }

    public String executeBinary(String binaryName, String... args) {
        File binary = new File(new File(workDir, "usr/bin"), binaryName);
        if (!binary.exists()) {
            binary = new File(nativeLibDir, "lib" + binaryName + ".so");
            if (!binary.exists() && binaryName.equals("gmsh")) {
                binary = new File(nativeLibDir, "libgmsh_bin.so");
            }
        }
        
        if (!binary.exists()) {
             return "Error: Binary (" + binaryName + ") not found.";
        }

        List<String> command = new ArrayList<>();
        command.add(binary.getAbsolutePath());
        for (String arg : args) {
            if (arg != null && !arg.isEmpty()) {
                command.add(arg);
            }
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workDir);
            pb.redirectErrorStream(true);

            Map<String, String> env = pb.environment();
            
            // Configure multi-threading
            String numCores = String.valueOf(Runtime.getRuntime().availableProcessors());
            env.put("OMP_NUM_THREADS", numCores);
            env.put("CCX_NPROC_EQUATION_SOLVER", numCores);
            
            File usrLib = new File(workDir, "usr/lib");
            env.put("LD_LIBRARY_PATH", usrLib.getAbsolutePath() + ":" + nativeLibDir.getAbsolutePath());
            
            String path = env.get("PATH");
            String binPath = new File(workDir, "usr/bin").getAbsolutePath();
            env.put("PATH", binPath + ":" + nativeLibDir.getAbsolutePath() + (path != null ? ":" + path : ""));

            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            String result = output.toString().trim();
            
            StringBuilder fullLog = new StringBuilder();
            fullLog.append("> ").append(binaryName);
            for (String arg : args) fullLog.append(" ").append(arg);
            fullLog.append("\n");
            
            if (!result.isEmpty()) {
                fullLog.append(result).append("\n");
            }
            fullLog.append("Exit Code: ").append(exitCode);
            
            return fullLog.toString().trim();

        } catch (Exception e) {
            Log.e(TAG, "Execution Failed: " + e.getMessage());
            return "Execution Error: " + e.getMessage();
        }
    }
}
