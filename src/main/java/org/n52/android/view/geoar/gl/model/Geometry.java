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
package org.n52.android.view.geoar.gl.model;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;

/**
 * 
 * @author Arne de Wall
 * 	Represents the geometric representation of a {@link RenderNode}. It creates, compiles, 
 * 	and handles the specific Buffers for vertices, normals, colors and indices representation.
 *
 */
public class Geometry {

    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_DATA_SIZE = 3;
    private static final int COLOR_DATA_SIZE = 4;

    /**
     * 
     * @author Arne de Wall
     * 		Holds the details of different VBOs (vertices, normals, colors...) 
     * 			for the rendering process.
     *
     */
    public class BufferDetailsHolder {
	public Buffer buffer;
	public BufferType bufferType;
	public int bufferHandle;
	public int byteSize;
	public int target;

	public int usage;

	public BufferDetailsHolder() {
	}

	public BufferDetailsHolder(Buffer buffer, BufferType bufferType) {
	    this.bufferType = bufferType;
	    this.buffer = buffer;
	}
    }

    public enum BufferType {
	FLOAT_BUFFER, INT_BUFFER, SHORT_BUFFER
    }

    // FloatBuffer for vertex data (X,Y,Z);
    protected FloatBuffer verticesBuffer;
    // FloatBuffer containing normals (X,Y,Z)
    protected FloatBuffer normalsBuffer;
    // FloatBuffer for Color data (R,G,B,A)
    protected FloatBuffer colorsBuffer;
    // IntBuffer for Indices
    protected IntBuffer indicesBuffer;

    protected int verticesCount;
    protected int indicesCount;

    /**
     * @return the indicesCount
     */
    public int getIndicesCount() {
        return indicesCount;
    }

    protected BufferDetailsHolder vertexDetails;
    protected BufferDetailsHolder colorDetails;
    protected BufferDetailsHolder normalDetails;
    protected BufferDetailsHolder indicesDetails;

    public Geometry() {
	vertexDetails = new BufferDetailsHolder();
	colorDetails = new BufferDetailsHolder();
	normalDetails = new BufferDetailsHolder();
	indicesDetails = new BufferDetailsHolder();
    }

    /**
     * 
     */
    public void createBuffers() {
	if (verticesBuffer != null) {
	    verticesBuffer.compact().position(0);
	    createBuffer(verticesBuffer, BufferType.FLOAT_BUFFER, vertexDetails, GLES20.GL_ARRAY_BUFFER);
	}
	if (normalsBuffer != null) {
	    normalsBuffer.compact().position(0);
	    createBuffer(normalsBuffer, BufferType.FLOAT_BUFFER, normalDetails, GLES20.GL_ARRAY_BUFFER);
	}
	if (colorsBuffer != null) {
	    colorsBuffer.compact().position(0);
	    createBuffer(colorsBuffer, BufferType.FLOAT_BUFFER, colorDetails, GLES20.GL_ARRAY_BUFFER);
	}
	if (indicesBuffer != null) {
	    indicesBuffer.compact().position(0);
	    createBuffer(indicesBuffer, BufferType.INT_BUFFER, indicesDetails, GLES20.GL_ARRAY_BUFFER);
	}

	GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
	GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * 
     * @param buffer
     * 			Buffer for interacting with NIO channels. A buffer is essentially a block of memory
     * 			into which data will be written to in order to interacting with the graphical device.
     * @param type
     * @param details
     * @param target
     */
    public void createBuffer(Buffer buffer, BufferType type, BufferDetailsHolder details, int target) {
	int buff[] = new int[1];
	GLES20.glGenBuffers(1, buff, 0);
	int handle = buff[0];
	int byteSize = BYTES_PER_FLOAT;

	GLES20.glBindBuffer(target, handle);
	GLES20.glBufferData(target, buffer.limit() * byteSize, buffer, GLES20.GL_STATIC_DRAW);
	GLES20.glBindBuffer(target, 0);

	details.buffer = buffer;
	details.bufferHandle = handle;
	details.bufferType = type;
	details.target = target;
	details.byteSize = byteSize;
    }

    public void setRenderObjectives(float[] vertices, float[] colors, float[] normals, int[] indices) {
	setVertices(vertices, true);
	if (normals != null)
	    setNormals(normals);
	if (colors != null)
	    setColors(colors);
	if (indices != null)
	    setIndices(indices);
	createBuffers();
    }
    
    public void setRenderObjectives(float[] vertices, float[] colors, float[] normals, short[] indices) {
	setVertices(vertices, true);
	if (normals != null)
	    setNormals(normals);
	if (colors != null)
	    setColors(colors);
	if (indices != null)
	    setIndices(indices);
	createBuffers();
    }

    public void setVertices(float[] vertices, boolean override) {
	if (this.verticesBuffer == null || override == true) {
	    if (this.verticesBuffer != null)
		this.verticesBuffer.clear();
	    this.verticesBuffer = allocateFloatMemory(vertices, BYTES_PER_FLOAT);
	    this.verticesCount = vertices.length / 3;
	} else {
	    this.verticesBuffer.put(vertices);
	}
    }

    public FloatBuffer getVerticesBuffer() {
	return this.verticesBuffer;
    }

    public void setNormals(float[] normals) {
	if (this.normalsBuffer == null) {
	    this.normalsBuffer = allocateFloatMemory(normals, BYTES_PER_FLOAT);
	} else {
	    normalsBuffer.clear(); // TODO do we need to clear ?
	    normalsBuffer.position(0);
	    normalsBuffer.put(normals);
	}
    }

    public FloatBuffer getNormalsBuffer() {
	return normalsBuffer;
    }

    public void setColors(float[] colors) {
	if (colorsBuffer == null) {
	    this.colorsBuffer = allocateFloatMemory(colors, BYTES_PER_FLOAT);
	} else {
	    this.colorsBuffer.put(colors);
	}
    }

    public FloatBuffer getColorsBuffer() {
	return colorsBuffer;
    }

    public void setIndices(int[] indices) {
	if (indicesBuffer == null) {
	    indicesBuffer = allocateIntMemory(indices, BYTES_PER_FLOAT);
	    indicesCount = indices.length;
	} else {
	    indicesBuffer.put(indices);
	}
    }
    
    public void setIndices(short[] indices) {
//	if (indicesBuffer == null) {
//	    indicesBuffer = allocateShortMemory(indices, 2);
//	    indicesCount = indices.length;
//	} else {
//	    indicesBuffer.put(indices);
//	}
    }

    public IntBuffer getIndicesBuffer() {
	return indicesBuffer;
    }

    private static FloatBuffer allocateFloatMemory(float[] data, int byteSize) {
	FloatBuffer floatBuffer = ByteBuffer.allocateDirect(data.length * byteSize).order(ByteOrder.nativeOrder())
		.asFloatBuffer();
	floatBuffer.put(data).position(0);
	return floatBuffer;
    }

    private static IntBuffer allocateIntMemory(int[] data, int byteSize) {
	IntBuffer intBuffer = ByteBuffer.allocateDirect(data.length * byteSize).order(ByteOrder.nativeOrder())
		.asIntBuffer();
	intBuffer.put(data).position(0);
	return intBuffer;
    }
    
    private static ShortBuffer allocateShortMemory(short[] data, int byteSize){
	ShortBuffer shortBuffer = ByteBuffer.allocateDirect(data.length * byteSize).order(ByteOrder.nativeOrder())
		.asShortBuffer();
	shortBuffer.put(data).position(0);
	return shortBuffer;
    }

    /**
     * @return the verticesCount
     */
    public int getVerticesCount() {
	return verticesCount;
    }

    /**
     * @return the vertexDetails
     */
    public BufferDetailsHolder getVertexDetails() {
	return vertexDetails;
    }

    /**
     * @param vertexDetails
     *            the vertexDetails to set
     */
    public void setVertexDetails(BufferDetailsHolder vertexDetails) {
	this.vertexDetails = vertexDetails;
    }

    /**
     * @return the colorDetails
     */
    public BufferDetailsHolder getColorDetails() {
	return colorDetails;
    }

    /**
     * @param colorDetails
     *            the colorDetails to set
     */
    public void setColorDetails(BufferDetailsHolder colorDetails) {
	this.colorDetails = colorDetails;
    }

    /**
     * @return the normalDetails
     */
    public BufferDetailsHolder getNormalDetails() {
	return normalDetails;
    }

    /**
     * @param normalDetails
     *            the normalDetails to set
     */
    public void setNormalDetails(BufferDetailsHolder normalDetails) {
	this.normalDetails = normalDetails;
    }

    /**
     * @return the indicesDetails
     */
    public BufferDetailsHolder getIndicesDetails() {
	return indicesDetails;
    }

    /**
     * @param indicesDetails
     *            the indicesDetails to set
     */
    public void setIndicesDetails(BufferDetailsHolder indicesDetails) {
	this.indicesDetails = indicesDetails;
    }
}
