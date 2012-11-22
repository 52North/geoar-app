/**
 * Copyright 2012 52°North Initiative for Geospatial Open Source Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.android.view.geoar.gl.model.shader;

import android.opengl.GLES20;

public class SimpleColorRenderer extends Renderer{
	
	private static SimpleColorRenderer instance;
	
	static final String vertexShader =
			"uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
			
			  + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
			  + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.			  
			  
			  + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.
			  
			  + "void main()                    \n"		// The entry point for our vertex shader.
			  + "{                              \n"
			  + "   v_Color = a_Color;          \n"		// Pass the color through to the fragment shader. 
			  											// It will be interpolated across the triangle.
			  + "   gl_Position = u_MVPMatrix   \n" 	// gl_Position is a special variable used to store the final position.
			  + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in 			                                            			 
			  + "}                              \n";    // normalized screen coordinates.     
//		      "uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
//
//			+ "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
//		    + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.
//		    + "attribute vec3 a_Normal;        \n"		// Per-vertex color information we will pass in.
//		    
//		    + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.
//		  
//		    + "void main()                    \n" 		// The entry point for our vertex shader.
//		    + "{                              \n"		
//		    + " v_Color = a_Color;			  \n"
//		    
//			// gl_Position is a special variable used to store the final position.
//			// Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
//			+ " gl_Position = u_MVPMatrix * a_Position; \n"
//			+ "}";
		
	static final String fragmentShader = 
			"precision mediump float;       \n"	// Set the default precision to medium. We don't need as high of a 
			// precision in the fragment shader.
		+	"varying vec4 v_Color;          \n"	// This is the color from the vertex shader interpolated across the 
						// triangle per fragment.
			
			//The entry point for our fragment shader.
		+	"void main()                    \n"		
		+	"{                              \n"	
			// Pass through the color
		+	"gl_FragColor = v_Color;        \n"                         		
		+	"}                              \n";
	
	public static SimpleColorRenderer getInstance(){
		if(instance == null){
			instance = new SimpleColorRenderer();
		} else if(!GLES20.glIsProgram(instance.programHandle)){
		    instance = new SimpleColorRenderer();
		}
		return instance;
	}
	
	public SimpleColorRenderer(){
		super(SimpleColorRenderer.vertexShader, SimpleColorRenderer.fragmentShader);
		initShaders();
	}
}
