package com.diamon.civil.ui;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class FrameRenderer implements GLSurfaceView.Renderer {

    private final Context context;
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    
    private int program;
    private int uProjectionLocation;
    private int uViewLocation;
    private int uColorLocation;

    private FloatBuffer gridBuffer;
    private int gridVertexCount;

    public FrameRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        
        // Simple program
        String vertexShaderCode = 
            "#version 300 es\n" +
            "layout(location = 0) in vec3 aPosition;\n" +
            "uniform mat4 uProjection;\n" +
            "uniform mat4 uView;\n" +
            "void main() {\n" +
            "    gl_Position = uProjection * uView * vec4(aPosition, 1.0);\n" +
            "}";
            
        String fragmentShaderCode = 
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "out vec4 fragColor;\n" +
            "uniform vec4 uColor;\n" +
            "void main() {\n" +
            "    fragColor = uColor;\n" +
            "}";

        program = createProgram(vertexShaderCode, fragmentShaderCode);
        uProjectionLocation = GLES30.glGetUniformLocation(program, "uProjection");
        uViewLocation = GLES30.glGetUniformLocation(program, "uView");
        uColorLocation = GLES30.glGetUniformLocation(program, "uColor");

        setupGrid();
    }

    private void setupGrid() {
        int size = 20;
        int step = 1;
        float[] gridVertices = new float[(size * 2 + 1) * 2 * 2 * 3];
        int idx = 0;

        for (int i = -size; i <= size; i += step) {
            // Horizontal lines
            gridVertices[idx++] = -size; gridVertices[idx++] = 0; gridVertices[idx++] = i;
            gridVertices[idx++] = size; gridVertices[idx++] = 0; gridVertices[idx++] = i;
            // Vertical lines
            gridVertices[idx++] = i; gridVertices[idx++] = 0; gridVertices[idx++] = -size;
            gridVertices[idx++] = i; gridVertices[idx++] = 0; gridVertices[idx++] = size;
        }
        gridVertexCount = idx / 3;

        gridBuffer = ByteBuffer.allocateDirect(gridVertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gridBuffer.put(gridVertices);
        gridBuffer.position(0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45, ratio, 0.1f, 100.0f);
        Matrix.setLookAtM(viewMatrix, 0, 10, 10, 10, 0, 0, 0, 0, 1, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        GLES30.glUseProgram(program);
        GLES30.glUniformMatrix4fv(uProjectionLocation, 1, false, projectionMatrix, 0);
        GLES30.glUniformMatrix4fv(uViewLocation, 1, false, viewMatrix, 0);

        // Draw Grid
        GLES30.glUniform4f(uColorLocation, 0.3f, 0.3f, 0.3f, 1.0f);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, gridBuffer);
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, gridVertexCount);
        GLES30.glDisableVertexAttribArray(0);
    }

    private int createProgram(String vertexCode, String fragmentCode) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode);
        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);
        return program;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }
}
