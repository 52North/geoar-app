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

public class BilligerColorShader extends FeatureShader {

	//@formatter:off
	private static final String vertexShader = "" +
			"uniform mat4 "+ UNIFORM_MATRIX_MVP +";      	\n" + 
			"attribute vec4 "+ ATTRIBUTE_POSITION +";     	\n" + 
			"attribute vec4 "+ ATTRIBUTE_COLOR +";        	\n" + 
			
			"varying vec4 vColor;          					\n" + 
			
			"void main()                    				\n" + 
			"{                              				\n"	+ 
			
			/** pass the color directly to the fragment shader */
			"   vColor = "+ ATTRIBUTE_COLOR +";          	\n" +
			
			/** compute and set the vertex position */
			"   gl_Position = "+ UNIFORM_MATRIX_MVP +"   	\n" + 
			"               * "+ ATTRIBUTE_POSITION +";   	\n" +
			
			"}                              \n"; 

	private static final String fragmentShader = 
			"precision mediump float;       		\n" + 
			"varying vec4 vColor;           		\n" + 
					
			"void main()                    		\n" + 
			"{                              		\n" + 
			
			/** set the pixel color */
			"	gl_FragColor = vColor;        		\n" + 
			"}                              		\n";
	//@formatter:on
	
	private static FeatureShader instance;

	/** not used */
	private BilligerColorShader() {
		super(vertexShader, fragmentShader);
	}

	public static FeatureShader getInstance() {
		if (instance == null) {
			instance = new FeatureShader(vertexShader, fragmentShader);
		}
		return instance;
	}

}
