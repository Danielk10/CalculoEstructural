package com.diamon.civil.ui;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class FrameGLSurfaceView extends GLSurfaceView {

    private final FrameRenderer renderer;

    public FrameGLSurfaceView(Context context) {
        this(context, null);
    }

    public FrameGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(3);
        renderer = new FrameRenderer(context);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}
