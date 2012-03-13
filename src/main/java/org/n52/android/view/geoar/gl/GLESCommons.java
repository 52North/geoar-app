package org.n52.android.view.geoar.gl;

import android.opengl.GLES20;
import android.util.Log;

public abstract class GLESCommons {
	
	private static final String TAG = GLESCommons.class.getSimpleName().toString();
	
	public static int createProgram(String vertexSource, String fragmentSource){
		int vertexShaderHandle = loadVertexShader(vertexSource);
		if (vertexShaderHandle == 0)
			return 0;
		
		int fragmentShaderHandle = loadFragmentShader(fragmentSource);
		if (fragmentShaderHandle == 0)
			return 0;
		
		int program = createProgram(vertexShaderHandle, fragmentShaderHandle);
		return program;
	}
	
	private static int createProgram(int vertexShaderHandle, int fragmentShaderHandle){
		int programHandle = GLES20.glCreateProgram();
		if (programHandle != 0){
			// bind vertex shader to the program
			GLES20.glAttachShader(programHandle, vertexShaderHandle);
			// bind fragment shader to the program
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);
			// link shaders into the program
			GLES20.glLinkProgram(programHandle);
			// get the link status
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
			// check link status
			if(linkStatus[0] == 0){
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
				Log.e(TAG, "createProgram failed: " + GLES20.glGetProgramInfoLog(programHandle));
				throw new RuntimeException("Error creating program");
			}
		}
		return programHandle;
	}
	
	private static int loadVertexShader(String source){
		int shaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		if(shaderHandle != 0){
			// Pass in the shader source
			GLES20.glShaderSource(shaderHandle,  source);
			// Compile the shader
			GLES20.glCompileShader(shaderHandle);
			// get the compilation status
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
			// check the compilation status
			if (compileStatus[0] == 0){
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
				Log.e(TAG, "Vertexshader compilation failed: " + GLES20.glGetShaderInfoLog(shaderHandle));
				throw new RuntimeException("Error creating vertex shader.");
			}
		}
		
		return shaderHandle;
	}
	
	private static int loadFragmentShader(String source){
		int shaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		if(shaderHandle != 0){
			// Pass in the shader source
			GLES20.glShaderSource(shaderHandle,  source);
			// Compile the shader
			GLES20.glCompileShader(shaderHandle);
			// get the compilation status
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
			// check the compilation status
			if (compileStatus[0] == 0){
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
				Log.e(TAG, "Fragmentshader compilation failed: " + GLES20.glGetShaderInfoLog(shaderHandle));
				throw new RuntimeException("Error creating fragment shader.");
			}
		}
		
		return shaderHandle;
	}

}
