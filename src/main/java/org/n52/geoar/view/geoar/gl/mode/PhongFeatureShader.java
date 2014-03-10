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
package org.n52.geoar.view.geoar.gl.mode;

public class PhongFeatureShader extends FeatureShader {
	
	private final String vertexShader = 
			"uniform mat4 " + UNIFORM_MATRIX_MVP + ";		\n" + 	
			"uniform mat4 " + UNIFORM_MATRIX_MV + ";		\n" +	
			"uniform vec3 " + UNIFORM_VEC3_LIGHTPOS + ";	\n" + 	
			
			"attribute vec3 " + ATTRIBUTE_POSITION + ";		\n" +	
			"attribute vec4 " + ATTRIBUTE_COLOR + ";		\n" +	
			"attribute vec3 " + ATTRIBUTE_NORMAL + ";		\n" + 	
				
			/** color varying for fragment shader */
			"varying vec4 vColor;			\n" +	
			
			"void main()					\n" +
			"{                              \n" + 
			
			/** transform vertex and normals into model coordinates */
			"   vec3 vertexMV = vec3(" + UNIFORM_MATRIX_MV + " * vec4(" + ATTRIBUTE_POSITION + ", 0.0));	\n" +
			"	vec3 normalMV = vec3(" + UNIFORM_MATRIX_MV + " * vec4(" + ATTRIBUTE_NORMAL + ", 0.0));		\n" +
				
			/** lightning */
			"	vec3 lightVec = normalize(" + UNIFORM_VEC3_LIGHTPOS + " - vertexMV)			\n" +
			"	float dist = length(" + UNIFORM_VEC3_LIGHTPOS + " - vertexMV)				\n" +
			"	float diffuse = max(dot(normalMV, lightVec), 0.1)*(1.0/(1.0(0.25*dist*dist)))	\n" +
			
			/** set color and position */
			"	vColor = " + ATTRIBUTE_COLOR + " * diffuse;			\n" +
			"   gl_Position = " + UNIFORM_MATRIX_MVP + "   			\n" +
			"               * " + ATTRIBUTE_POSITION + ";   		\n" + 
			"}                              						\n";       

	private final String fragmentShader = 
			"precision mediump float;       \n"	// Set the default precision to medium. We don't need as high of a 
						// precision in the fragment shader.
		+	"varying vec4 vColor;          \n"	// This is the color from the vertex shader interpolated across the 
						// triangle per fragment.
			
			//The entry point for our fragment shader.
		+	"void main()                    \n"		
		+	"{                              \n"	
			// Pass through the color
		+		"gl_FragColor = vColor;    	\n"                         		
		+	"}                              \n";
	
	public PhongFeatureShader(String vertexShader, String fragmentShader) {
		super(vertexShader, fragmentShader);
	}

}
