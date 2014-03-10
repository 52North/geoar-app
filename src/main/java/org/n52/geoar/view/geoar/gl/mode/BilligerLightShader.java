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

public class BilligerLightShader extends FeatureShader {

	//@formatter:off
	private static final String vertexShader = "" +
			"uniform mat4 "+UNIFORM_MATRIX_MVP+";		\n" +    		       
			"uniform mat4 "+UNIFORM_MATRIX_MV+";		\n" +    		
		  			
			"attribute vec4 "+ATTRIBUTE_POSITION+";		\n" +  				
			"attribute vec4 "+ATTRIBUTE_COLOR+";		\n" +			
			"attribute vec4 "+ATTRIBUTE_NORMAL+";		\n" +      		
		  
			"varying vec3 vPosition;					\n" +      		
			"varying vec4 vColor;						\n" +         		
			"varying vec3 vNormal;						\n" + 	
		   
			"void main()                    			\n" +                             	
			"{                              			\n" +                           
					
				"vPosition = vec3("+UNIFORM_MATRIX_MV+" * "+ATTRIBUTE_POSITION+");          	\n" +  
				"vColor = "+ATTRIBUTE_COLOR+";													\n" +
				"vNormal = normalize(vec3("+UNIFORM_MATRIX_MV+" * "+ATTRIBUTE_NORMAL+"));		\n" +
				"gl_Position = "+UNIFORM_MATRIX_MVP+" * "+ATTRIBUTE_POSITION+";                	\n" +
				
			"}                               													\n";
	
	private static final String fragmentShader = "" +
			"precision mediump float;     					\n" + 
			"uniform vec3 "+UNIFORM_VEC3_LIGHTPOS+";     	\n" + 

			"varying vec3 vPosition;	  					\n" +	
			"varying vec4 vColor;        					\n" + 
			"varying vec3 vNormal;       					\n" +

			"void main()                    				\n" +
			"{                              				\n" +
//				"float distance = length("+UNIFORM_VEC3_LIGHTPOS+");   			\n" +               

				"vec3 lightVector = normalize("+UNIFORM_VEC3_LIGHTPOS+");       \n" +       	

				"float diffuse = max(dot(vNormal, lightVector), 0.0);			\n" +	  
//				"diffuse = diffuse * (1.0 / (1.0 + (0.01 * distance)));			\n" +
				"diffuse = diffuse + 0.5;  										\n" +

				"gl_FragColor = diffuse * vColor;								\n" +               		
			"}            														\n";
	//@formatter:on
	
	private static FeatureShader instance;
	
	public static FeatureShader getInstance(){
		if(instance == null){
			instance = new FeatureShader(vertexShader, fragmentShader);
		}
		return instance;
	}
	
	/** unused */
	private BilligerLightShader(String vertexShader, String fragmentShader) {
		super(vertexShader, fragmentShader);
	}

}
