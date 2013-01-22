package org.n52.android.view.geoar.gl.mode;

public class ShabbyTextureShader extends FeatureShader{

	//@formatter:off
	private static final String vertexShader = 
			"uniform mat4 " + UNIFORM_MATRIX_MVP + ";				\n" + 		
			
			"attribute vec4 " + ATTRIBUTE_POSITION + ";				\n" +	
			"attribute vec4 " + ATTRIBUTE_COLOR + ";        		\n" +
			"attribute vec2 " + ATTRIBUTE_TEXTURE + ";				\n" + 	
			
			/** color varying for fragment shader */	
			"varying vec2 vTexCoordinate;							\n" + 
			 
			"void main()											\n" +
			"{                              						\n" + 
			
			/** transform vertex and normals into model coordinates */
			"	vTexCoordinate = " + ATTRIBUTE_TEXTURE + ";													\n" +

			/** set position */
			"   gl_Position = " + UNIFORM_MATRIX_MVP + " * " + ATTRIBUTE_POSITION + ";   					\n" +

			"}           																					\n";
	
	private static final String fragmentShader = 
			"precision mediump float;															\n" +
			"uniform sampler2D " + UNIFORM_SAMPLER_TEXTURE + ";									\n" +
			
			/** color varying for fragment shader */
			"varying vec2 vTexCoordinate;														\n" + 
			
			"void main()																		\n" +
			"{																					\n" +

			"	gl_FragColor =  (texture2D(" + UNIFORM_SAMPLER_TEXTURE + ", vTexCoordinate)); 	\n" +
			
			"}																					\n";		
	//@formatter:on
	
	private static FeatureShader INSTANCE;
	
	private ShabbyTextureShader(String vertexShader, String fragmentShader) {
		super(vertexShader, fragmentShader);
		// TODO Auto-generated constructor stub
	}
	
	public static FeatureShader getInstance(){
		if(INSTANCE == null){
			INSTANCE = new FeatureShader(vertexShader, fragmentShader);
		}
		
		return INSTANCE;
	}
	

}
