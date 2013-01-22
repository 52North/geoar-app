package org.n52.android.view.geoar.gl.mode;

public class LightFeatureShader extends FeatureShader {

	//@formatter:off
	private static final String vertexShader = "" +
			"uniform mat4 "+UNIFORM_MATRIX_MVP+";		\n" +// A constant representing the combined model/view/projection matrix.      		       
			"uniform mat4 "+UNIFORM_MATRIX_MV+";		\n" +// A constant representing the combined model/view matrix.       		
		  			
			"attribute vec4 "+ATTRIBUTE_POSITION+";		\n" +// Per-vertex position information we will pass in.   				
			"attribute vec4 "+ATTRIBUTE_COLOR+";		\n" +// Per-vertex color information we will pass in. 				
			"attribute vec4 "+ATTRIBUTE_NORMAL+";		\n" +// Per-vertex normal information we will pass in.       		
		  
			"varying vec3 vPosition;		\n" +// This will be passed into the fragment shader.       		
			"varying vec4 vColor;			\n" +// This will be passed into the fragment shader.          		
			"varying vec3 vNormal;			\n" +// This will be passed into the fragment shader.  	
		  
			// The entry point for our vertex shader.  
			"void main()                    \n" +                             	
			"{                              \n" +                           
				// Transform the vertex into eye space. 	
				"vPosition = vec3("+UNIFORM_MATRIX_MV+" * "+ATTRIBUTE_POSITION+");          	\n" +  
		
				// Pass through the color.
				"vColor = "+ATTRIBUTE_COLOR+";										\n" +
	
				// Transform the normal's orientation into eye space.
				"vNormal = normalize(vec3("+UNIFORM_MATRIX_MV+" * "+ATTRIBUTE_NORMAL+"));		\n" +
          
				// gl_Position is a special variable used to store the final position.
				// Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
				"gl_Position = "+UNIFORM_MATRIX_MVP+" * "+ATTRIBUTE_POSITION+";                \n" +      		  
			"}                               							\n";
	
	private static final String fragmentShader = "" +
			"precision mediump float;     \n" + // Set the default precision to medium. We don't need as high of a 
			// precision in the fragment shader.
			"uniform vec3 "+UNIFORM_VEC3_LIGHTPOS+";     \n" + // The position of the light in eye space.

			"varying vec3 vPosition;	  \n" +	// Interpolated position for this fragment.
			"varying vec4 vColor;        \n" + // This is the color from the vertex shader interpolated across the 
			// triangle per fragment.
			"varying vec3 vNormal;       \n" + // Interpolated normal for this fragment.

			//The entry point for our fragment shader.
			"void main()                    		\n" +
			"{                              \n" +
				// Will be used for attenuation.
//				"float distance = length("+UNIFORM_VEC3_LIGHTPOS+");   \n" +               

				// Get a lighting direction vector from the light to the vertex.
				"vec3 lightVector = normalize("+UNIFORM_VEC3_LIGHTPOS+");       \n" +       	

				// Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
				// pointing in the same direction then it will get max illumination.
				"float diffuse = max(dot(vNormal, lightVector), 0.0);										\n" +	  

				// Add attenuation. 
//				"diffuse = diffuse * (1.0 / (1.0 + (0.01 * distance)));	\n" +
//				"vec3 diffuse2 = vec3(0.3,0.3,0.3) + diffuse * vec3(1.0, 1.0, 1.0);  									\n" +
				// Add ambient lighting
				"diffuse = diffuse + 0.3;  									\n" +

				// Multiply the color by the diffuse illumination level to get final output color.
				"gl_FragColor = diffuse * vColor;		\n" +               		
			"}            													\n";
	
	private static FeatureShader instance;
	
	public static FeatureShader getInstance(){
		if(instance == null){
			instance = new FeatureShader(vertexShader, fragmentShader);
		}
		return instance;
	}
	
	/** unused */
	private LightFeatureShader(String vertexShader, String fragmentShader) {
		super(vertexShader, fragmentShader);
	}

}
