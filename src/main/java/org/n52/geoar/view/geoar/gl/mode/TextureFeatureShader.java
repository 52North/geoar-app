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

public class TextureFeatureShader extends FeatureShader {

	//@formatter:off
	private final static String vertexShader = 
			"uniform mat4 " + UNIFORM_MATRIX_MVP + ";				\n" + 	
			"uniform mat4 " + UNIFORM_MATRIX_MV + ";				\n" +	
//			"uniform vec3 " + UNIFORM_VEC3_LIGHTPOS + ";			\n" + 	
			
			"attribute vec4 " + ATTRIBUTE_POSITION + ";				\n" +	
			"attribute vec4 " + ATTRIBUTE_COLOR + ";				\n" +	
			"attribute vec4 " + ATTRIBUTE_NORMAL + ";				\n" + 	
			"attribute vec2 " + ATTRIBUTE_TEXTURE + ";				\n" + 	
			
			/** color varying for fragment shader */
			"varying vec3 vPosition;								\n" +
			"varying vec4 vColor;									\n" +	
			"varying vec3 vNormal;									\n" +
			"varying vec2 vTexCoordinate;							\n" + 
			
			"void main()											\n" +
			"{                              						\n" + 
			
			// TODO position is something we already calculated
			/** transform vertex and normals into model coordinates */
			"   vPosition = vec3(" + UNIFORM_MATRIX_MV + " * " + ATTRIBUTE_POSITION + ");				\n" +
			"	vColor = " + ATTRIBUTE_COLOR + ";														\n" +
			"	vNormal = normalize(vec3(" + UNIFORM_MATRIX_MV + " * " + ATTRIBUTE_NORMAL + "));					\n" +
			"	vTexCoordinate = " + ATTRIBUTE_TEXTURE + ";												\n" +
			
			/** set position */
			"   gl_Position = " + UNIFORM_MATRIX_MVP + " * " + ATTRIBUTE_POSITION + ";   				\n" + 
			"}           																				\n";
	
	private final static String fragmentShader = 
			"precision mediump float;						\n" +
			"uniform vec3 " + UNIFORM_VEC3_LIGHTPOS + ";	\n" +
			"uniform sampler2D " + UNIFORM_SAMPLER_TEXTURE + ";	\n" +
			
			/** color varying for fragment shader */
			"varying vec4 vColor;									\n" +	
			"varying vec3 vPosition;								\n" +
			"varying vec3 vNormal;									\n" +
			"varying vec2 vTexCoordinate;							\n" + 
			
			"void main()									\n" +
			"{												\n" +
			
			/** calculate the distance and the normlized light vector */
			"	float dist = length("+ UNIFORM_VEC3_LIGHTPOS + " - vPosition)/100.0;			\n" + 
			"   vec3 lightVec = normalize(" + UNIFORM_VEC3_LIGHTPOS + " - vPosition); 	\n" +
			"   float diffuse = max(dot(vNormal, lightVec), 0.0); 						\n" +
			"	diffuse = diffuse * (1.0 / ( 0.25 * dist * dist)); 					\n" +
			"   diffuse = diffuse + 0.6; 												\n" +
			
//			"	vec3 lightVec = normalize(" + UNIFORM_VEC3_LIGHTPOS + " - vPosition); \n" +
//			"	float lightVec \n" +

			// Removed alpha from lighting
			"   vec4 color = vColor * texture2D(" + UNIFORM_SAMPLER_TEXTURE + ", vTexCoordinate);\n" +
			"   color.rgb = color.rgb * diffuse; \n" +
			"	gl_FragColor = color; \n" +
			"}												\n";				
	//@formatter:on
	
	private static FeatureShader instance;
	
	public static FeatureShader getInstance() {
		if (instance == null) {
			instance = new FeatureShader(vertexShader, fragmentShader);
		}

		return instance;
	}

	private TextureFeatureShader(String vertexShader, String fragmentShader) {
		/** unused */
		super(vertexShader, fragmentShader);
	}

}
