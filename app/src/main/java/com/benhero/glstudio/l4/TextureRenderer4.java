package com.benhero.glstudio.l4;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

import com.benhero.glstudio.R;
import com.benhero.glstudio.util.LoggerConfig;
import com.benhero.glstudio.util.ShaderHelper;
import com.benhero.glstudio.util.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 纹理绘制
 *
 * @author Benhero
 */
public class TextureRenderer4 implements Renderer {
    private static final String VERTEX_SHADER = "" +
            "uniform mat4 u_Matrix;\n" +
            "attribute vec4 a_Position;\n" +
            // 纹理坐标：2个分量，S和T坐标
            "attribute vec2 a_texCoord;\n" +
            "varying vec2 v_texCoord;\n" +
            // a_Color：从外部传递进来的每个顶点的颜色值
            "attribute vec4 a_Color;\n" +
            // v_Color：将每个顶点的颜色值传递给片段着色器
            "varying vec4 v_Color;\n" +
            "void main()\n" +
            "{\n" +
            "    v_Color = a_Color;\n" +
            "    v_texCoord = a_texCoord;\n" +
            "    gl_Position = u_Matrix * a_Position;\n" +
            "}";
    private static final String FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "varying vec2 v_texCoord;\n" +
            "varying vec4 v_Color;\n" +
            // sampler2D：二维纹理数据的数组
            "uniform sampler2D u_TextureUnit;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_FragColor = v_Color * texture2D(u_TextureUnit, v_texCoord);\n" +
            "}";
    private static final String A_POSITION = "a_Position";
    private static final String U_MATRIX = "u_Matrix";
    private static final String U_TEXTURE_UNIT = "u_TextureUnit";
    private static final String A_TEX_COORD = "a_texCoord";
    private static final String A_COLOR = "a_Color";

    private final Context mContext;
    private int mProgram;
    private final FloatBuffer mVertexData;
    private int aPositionLocation;
    private int uMatrixLocation;
    private int aColorLocation;

    /**
     * 纹理坐标索引
     */
    private int aTexCoordLocation;
    private int uTextureUnitLocation;
    /**
     * 颜色占用的向量个数
     */
    private static final int COLOR_COMPONENT_COUNT = 4;

    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int BYTES_PER_FLOAT = 4;

    private static final int STRIDE =
            (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private static final float[] POINT_DATA = {
            -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.9f,
            -0.5f, 0.5f, 1.0f, 1.0f, 1.0f, 0.9f,
            0.5f, 0.5f, 1.0f, 1.0f, 1.0f, 0.9f,
            0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.9f,
    };

    private final float[] projectionMatrix = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
    };

    /**
     * 纹理坐标
     */
    private static final float[] TEX_VERTEX = {
            0, 1,
            0, 0,
            1, 0,
            1, 1,
    };
    /**
     * 纹理坐标中每个点占的向量个数
     */
    private static final int TEX_VERTEX_COMPONENT_COUNT = 2;
    private final FloatBuffer mTexVertexBuffer;
    /**
     * 纹理数据
     */
    private TextureHelper.TextureBean mTextureBean;

    public TextureRenderer4(Context context) {
        mContext = context;

        mVertexData = ByteBuffer
                .allocateDirect(POINT_DATA.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(POINT_DATA);

        mTexVertexBuffer = ByteBuffer.allocateDirect(TEX_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEX_VERTEX);

    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        GLES20.glClearColor(1f, 1f, 1f, 0f);
        int vertexShader = ShaderHelper.compileVertexShader(VERTEX_SHADER);
        int fragmentShader = ShaderHelper.compileFragmentShader(FRAGMENT_SHADER);

        mProgram = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(mProgram);
        }

        GLES20.glUseProgram(mProgram);


        // 开启纹理透明混合，这样才能绘制透明图片
        GLES20.glEnable(GL10.GL_BLEND);
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

        // 纹理数据
        mTextureBean = TextureHelper.loadTexture(mContext, R.mipmap.ic_launcher);

        aPositionLocation = GLES20.glGetAttribLocation(mProgram, A_POSITION);
        uMatrixLocation = GLES20.glGetUniformLocation(mProgram, U_MATRIX);

        aColorLocation = GLES20.glGetAttribLocation(mProgram, A_COLOR);

        // 纹理索引
        aTexCoordLocation = GLES20.glGetAttribLocation(mProgram, A_TEX_COORD);
        uTextureUnitLocation = GLES20.glGetUniformLocation(mProgram, U_TEXTURE_UNIT);

        mVertexData.position(0);
        GLES20.glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, STRIDE, mVertexData);
        GLES20.glEnableVertexAttribArray(aPositionLocation);

        // COLOR_COMPONENT_COUNT：从数组中每次读取3个向量
        // STRIDE：每次读取间隔是 (2个位置 + 4个颜色值) * Float占的Byte位
        mVertexData.position(POSITION_COMPONENT_COUNT);
        GLES20.glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, STRIDE, mVertexData);
        GLES20.glEnableVertexAttribArray(aColorLocation);

        // 加载纹理坐标
        mTexVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(aTexCoordLocation, TEX_VERTEX_COMPONENT_COUNT, GLES20.GL_FLOAT, false, 0, mTexVertexBuffer);
        GLES20.glEnableVertexAttribArray(aTexCoordLocation);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        final float aspectRatio = width > height ?
                (float) width / (float) height :
                (float) height / (float) width;
        if (width > height) {
            Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);
        } else {
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
        }
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        drawTexture();
    }

    private void drawTexture() {
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);

        // 纹理单元：在OpenGL中，纹理不是直接绘制到片段着色器上，而是通过纹理单元去保存纹理

        // 设置当前活动的纹理单元为纹理单元0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // 将纹理ID绑定到当前活动的纹理单元上
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureBean.getTextureId());

        // 将纹理单元传递片段着色器的u_TextureUnit
        GLES20.glUniform1i(uTextureUnitLocation, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
    }
}
