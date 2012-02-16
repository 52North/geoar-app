/**
 * Copyright 2011 52°North Initiative for Geospatial Open Source Software GmbH
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
 * 
 */
package org.n52.android.view.geoar.gl;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import org.n52.android.alg.Interpolation;
import org.n52.android.alg.NoiseView.NoiseGridValueProvider;
import org.n52.android.alg.NoiseView.NoiseViewChangedListener;
import org.n52.android.alg.Picking;
import org.n52.android.alg.proj.MercatorPoint;
import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.MeasurementManager;
import org.n52.android.data.MeasurementManager.GetMeasurementBoundsCallback;
import org.n52.android.data.MeasurementManager.MeasurementsCallback;
import org.n52.android.data.MeasurementManager.RequestHolder;
import org.n52.android.geoar.R;
import org.n52.android.view.InfoView;
import org.n52.android.view.camera.CameraView;
import org.n52.android.view.camera.NoiseCamera;
import org.n52.android.view.camera.NoiseCamera.CameraUpdateListener;
import org.n52.android.view.geoar.Settings;
import org.osmdroid.util.GeoPoint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.location.Location;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;


/**
 * {@link Renderer} implementation to serve a augmented reality view on noise
 * interpolations
 * 
 * @author Holger Hopmann
 * 
 */
public class OpenGLRenderer implements Renderer, NoiseGridValueProvider,
		CameraUpdateListener {

	/**
	 * Public interface to create an object which can supplies the renderer with
	 * the current extrinsic camera rotation. Ensures that matrices are just
	 * computed as needed, since the renderer asks for data and does not listen
	 * for them.
	 */
	public interface RotationMatrixProvider {
		/**
		 * Returns the current extrinsic rotation matrix
		 * 
		 * @return row major 4x4 matrix
		 */
		float[] getRotationMatrix();
	}

	// Texture information for calibration overlay. Loads texture automatically
	// from defined resource
	private OpenGLTexture calibrationTexture = new OpenGLTexture(true) {
		@Override
		protected Bitmap getBitmap() {
			return BitmapFactory.decodeResource(context.getResources(),
					R.drawable.calibration_rect);
		}

		@Override
		protected void getTextureParams(GL11 gl11) {
			gl11.glTexParameterf(GL10.GL_TEXTURE_2D,
					GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl11.glTexParameterf(GL10.GL_TEXTURE_2D,
					GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl11.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
					GL10.GL_REPEAT);
			gl11.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
					GL10.GL_REPEAT);

		}
	};
	// Texture information for interpolation data. Does not load texture
	// automatically
	private OpenGLTexture interpolationTexture = new OpenGLTexture() {
		@Override
		protected Bitmap getBitmap() {
			return Interpolation.interpolationToBitmap(
					currentInterpolationRect, currentMeasurement.interpolationBuffer, null);
		}
	};

	// Callback for the interpolation request in setCenter
	private GetMeasurementBoundsCallback callback = new GetMeasurementBoundsCallback() {
		public void onAbort(MercatorRect bounds, int reason) {
			if (infoHandler != null) {
				infoHandler.clearProgress(OpenGLRenderer.this);
				if (reason == MeasurementManager.ABORT_NO_CONNECTION) {
					infoHandler.setStatus(R.string.connection_error, 5000,
							OpenGLRenderer.this);
				} else if (reason == MeasurementManager.ABORT_UNKOWN) {
					infoHandler.setStatus(R.string.unkown_error, 5000,
							OpenGLRenderer.this);
				}
			}
		}

		public void onProgressUpdate(int progress, int maxProgress, int step) {
			if (infoHandler != null) {
				String stepTitle = "";
				switch (step) {
				case Interpolation.STEP_CLUSTERING:
					stepTitle = context.getString(R.string.clustering);
					break;
				case Interpolation.STEP_INTERPOLATION:
					stepTitle = context.getString(R.string.interpolation);
					break;
				case MeasurementManager.STEP_REQUEST:
					stepTitle = context.getString(R.string.measurement_request);
					break;
				}

				infoHandler.setProgressTitle(stepTitle, OpenGLRenderer.this);
				infoHandler.setProgress(progress, maxProgress,
						OpenGLRenderer.this);
			}
		}

		public void onReceiveDataUpdate(MercatorRect bounds,
				MeasurementsCallback measurementsCallback) {
			// Save result reference in variable. Those should always be the
			// same
			
			currentMeasurement = measurementsCallback;
			currentInterpolationRect = bounds;
			// Update geometry to reflect the results dimension
			glInterpolation.setWidth(bounds.width());
			glInterpolation.setHeight(bounds.height());
			// Ask the corresponding texture to reload its data on next draw
			interpolationTexture.reload();
		}
	};

	// current center
	private GeoPoint currentCenterGPoint;
	private MercatorPoint currentCenterMercator;
	// current resolution to calculate distances in meter
	private float currentGroundResolution;

	private Context context;

	// currently used data
//	private byte[] currentInterpolation;
	private MeasurementsCallback currentMeasurement;
	private MercatorRect currentInterpolationRect;
	// performing measurement request
	private RequestHolder currentRequest;

	// Geometries for drawing
	private Rectangle glCalibration = new TexturedRectangleRepeat(
			calibrationTexture, 10, 10);
	private Rectangle glInterpolation = new TexturedRectangle(
			interpolationTexture);

	private InfoView infoHandler;

	// current display
	private boolean showInterpolation = true;
	private boolean showCalibration = false;

	private RotationMatrixProvider rotationProvider;
	private MeasurementManager measureManager;

	// Matrices currently used for transformation. Needed for unprojection for
	// noise diagram
	private float[] modelViewMatrix = new float[16];
	private float[] projectionMatrix = new float[16];
	private int[] viewportMatrix = new int[4];

	// Listeners for noise view feature
	private List<NoiseViewChangedListener> noiseViewChangedListeners = new ArrayList<NoiseViewChangedListener>();

	// Flag indicating that projection needs update
	private boolean resetProjection;

	/**
	 * Constructor
	 * 
	 * @param context
	 *            Context to load resources
	 * @param rotationProvider
	 *            Object which serves with extrinsic camera rotation matrix
	 */
	public OpenGLRenderer(Context context,
			RotationMatrixProvider rotationProvider) {
		this.rotationProvider = rotationProvider;
		this.context = context;
	}

	public void addOnNoiseViewChangedListener(NoiseViewChangedListener listener) {
		if (!noiseViewChangedListeners.contains(listener)) {
			noiseViewChangedListeners.add(listener);
		}
	}

	public byte getNoiseValue(int x, int y) {
		if (currentInterpolationRect != null && x >= 0
				&& x < currentInterpolationRect.width() && y >= 0
				&& y < currentInterpolationRect.height()) {
			return currentMeasurement.interpolationBuffer[y * currentInterpolationRect.width()
					+ x];
		} else {
			return NO_DATA;
		}
	}

	public void onCameraUpdate() {
		resetProjection = true;
	}

	public void onDrawFrame(GL10 gl) {
		GL11 gl11 = (GL11) gl; // OpenGl ES 1.1
		gl11.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		if (resetProjection) {
			resetProjectionFromCamera(gl11);
		}

		gl11.glPushMatrix();

		// Camera rotation, matrix is requested only on every frame (15 Hz), not
		// every sensor update (60 Hz).
		gl11.glMultMatrixf(rotationProvider.getRotationMatrix(), 0);
		// Camera translation
		gl11.glTranslatef(0, -NoiseCamera.height, 0);

		if (currentInterpolationRect != null) {
			// has interpolation to draw
			gl11.glPushMatrix();

			// Sets virtual center point
			float dx = currentCenterMercator.x - currentInterpolationRect.left;
			float dy = currentCenterMercator.y - currentInterpolationRect.top;
			gl11.glTranslatef(
					-dx * currentGroundResolution * NoiseCamera.scale, 0, -dy
							* currentGroundResolution * NoiseCamera.scale);
			gl11.glScalef(currentGroundResolution, 1, currentGroundResolution);
			gl11.glScalef(NoiseCamera.scale, 1, NoiseCamera.scale);

			// save current matrix for unprojection
			gl11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix, 0);

			// draw interpolation
			if (showInterpolation) {
				gl11.glColor4f(1, 1, 1, 0.7f);
				glInterpolation.draw(gl11);
			}
			gl11.glPopMatrix();

			// Find noise view, i. e. intersections with interpolation plane
			if (noiseViewChangedListeners.size() != 0) {

				OnNoiseViewChanged( // Unten, Top / Bottom getauscht
						Picking.getGridPoint(modelViewMatrix, projectionMatrix,
								viewportMatrix, 0, viewportMatrix[3], 0,
								viewportMatrix[2]), // Bottom Left
						Picking.getGridPoint(modelViewMatrix, projectionMatrix,
								viewportMatrix, 0, viewportMatrix[3],
								viewportMatrix[2], 0), // Bottom Right
						// Oben Top / Bottom korrekt
						Picking.getGridPoint(modelViewMatrix, projectionMatrix,
								viewportMatrix, viewportMatrix[3], 0, 0,
								viewportMatrix[2]), // Top Left
						Picking.getGridPoint(modelViewMatrix, projectionMatrix,
								viewportMatrix, viewportMatrix[3], 0,
								viewportMatrix[2], 0), // Top Right
						new PointF(dx, dy));
			}
		}

		// Show calibration plane
		if (showCalibration) {
			gl11.glColor4f(1, 1, 1, 1);
			gl11.glPushMatrix();
			gl11.glScalef(NoiseCamera.scale, 1, NoiseCamera.scale);
			gl11.glTranslatef(-glCalibration.width / 2, 0,
					-glCalibration.height / 2);
			glCalibration.draw(gl11);
			gl11.glPopMatrix();
		}

		gl11.glPopMatrix();

	}

	/**
	 * Method triggers all listeners with new noise view vertices
	 * 
	 * @param bottomLeft
	 * @param bottomRight
	 * @param topLeft
	 * @param topRight
	 * @param viewerPos
	 */
	private void OnNoiseViewChanged(PointF bottomLeft, PointF bottomRight,
			PointF topLeft, PointF topRight, PointF viewerPos) {
		for (NoiseViewChangedListener listener : noiseViewChangedListeners) {
			listener.onNoiseViewChanged(bottomLeft, bottomRight, topLeft,
					topRight, viewerPos);
		}
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GL11 gl11 = (GL11) gl;
		NoiseCamera.glViewportHeight = height;
		NoiseCamera.glViewportWidth = width;
		if (NoiseCamera.cameraViewportHeight == 0
				|| NoiseCamera.cameraViewportWidth == 0) {
			// Set camera viewport if none exists
			NoiseCamera.setViewportSize(width, height);
		}
		resetProjectionFromCamera(gl11);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Transparent background
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// No shading
		gl.glShadeModel(GL10.GL_FLAT);

		gl.glClearDepthf(1.0f);
		// Depth test not needed
		// gl.glEnable(GL10.GL_DEPTH_TEST);
		// gl.glDepthFunc(GL10.GL_LESS);

		// activate textureing
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		// "Note that when the EGL context is lost, all OpenGL resources
		// associated with that context will be automatically deleted. You do
		// not need to call the corresponding "glDelete" methods such as
		// glDeleteTextures to manually delete these lost resources"
		interpolationTexture.reset();
		calibrationTexture.reset();
	}

	/**
	 * Ask renderer to reload its interpolation
	 */
	public void reload() {
		if (currentCenterGPoint != null) {
			setCenter(currentCenterGPoint);
		}
	}

	public void removeOnNoiseViewChangedListener(
			NoiseViewChangedListener listener) {
		noiseViewChangedListeners.remove(listener);
	}

	/**
	 * (Re)sets intrinsic camera parameters obtained from the central
	 * {@link NoiseCamera} class. Data is updated from within the
	 * {@link CameraView} class.
	 * 
	 * @param gl11
	 */
	private void resetProjectionFromCamera(GL11 gl11) {
		gl11.glViewport(0, 0, NoiseCamera.glViewportWidth,
				NoiseCamera.glViewportHeight);

		gl11.glMatrixMode(GL10.GL_PROJECTION);
		gl11.glLoadIdentity();

		GLU.gluPerspective(gl11, NoiseCamera.fovY, NoiseCamera.aspect,
				NoiseCamera.zNear, NoiseCamera.zFar);

		gl11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projectionMatrix, 0);
		gl11.glGetIntegerv(GL11.GL_VIEWPORT, viewportMatrix, 0);

		gl11.glMatrixMode(GL10.GL_MODELVIEW);
		gl11.glLoadIdentity();

		resetProjection = false;
	}

	/**
	 * Sets the position of the viewer in the virtual realm. Data gets
	 * reloaded/updated if needed
	 * 
	 * @param gPoint
	 */
	public void setCenter(GeoPoint gPoint) {
		if (measureManager == null) {
			return;
		}

		// Calculate thresholds for request of data
		double meterPerPixel = MercatorProj.getGroundResolution(
				gPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);
		int pixelRadius = (int) (Settings.SIZE_AR_INTERPOLATION / meterPerPixel);
		int pixelReloadDist = (int) (Settings.RELOAD_DIST_AR / meterPerPixel);

		// Calculate new center point in world coordinates
		int centerPixelX = (int) MercatorProj.transformLonToPixelX(
				gPoint.getLongitudeE6() / 1E6f, Settings.ZOOM_AR);
		int centerPixelY = (int) MercatorProj.transformLatToPixelY(
				gPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);
		currentCenterMercator = new MercatorPoint(centerPixelX, centerPixelY,
				Settings.ZOOM_AR);
		currentCenterGPoint = gPoint;

		currentGroundResolution = (float) MercatorProj.getGroundResolution(
				currentCenterGPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);

		// determination if data request is needed or if just a simple shift is
		// enough
		boolean requestInterpolation = false;
		if (currentInterpolationRect == null) {
			// Get new data if there were none before
			requestInterpolation = true;
		} else {
			MercatorPoint interpolationCenter = currentInterpolationRect
					.getCenter();
			if (currentCenterMercator.zoom != currentInterpolationRect.zoom
					|| currentCenterMercator.distanceTo(interpolationCenter) > pixelReloadDist) {
				// request data if new center offsets more than
				// Settings.RELOAD_DIST_AR meters
				requestInterpolation = true;
			}
		}

		if (requestInterpolation) {
			// if new data is needed
			if (currentRequest != null) {
				// cancel currently running data request
				currentRequest.cancel();
			}
			
			// trigger data request
			currentRequest = measureManager.getInterpolation(new MercatorRect(
					currentCenterMercator.x - pixelRadius,
					currentCenterMercator.y - pixelRadius,
					currentCenterMercator.x + pixelRadius,
					currentCenterMercator.y + pixelRadius, Settings.ZOOM_AR),
					callback, false, currentMeasurement);
		}
	}

	/**
	 * @see OpenGLRenderer#setCenter(GeoPoint)
	 * 
	 * @param location
	 */
	public void setCenter(Location location) {
		setCenter(new GeoPoint((int) (location.getLatitude() * 1E6),
				(int) (location.getLongitude() * 1E6)));
	}

	public void setInfoHandler(InfoView infoHandler) {
		this.infoHandler = infoHandler;
	}

	/**
	 * sets the {@link MeasurementManager} to use for data request
	 * 
	 * @param measureManager
	 */
	public void setMeasureManager(MeasurementManager measureManager) {
		this.measureManager = measureManager;
		if (currentCenterGPoint != null) {
			setCenter(currentCenterGPoint);
		}
	}

	public void showCalibration(boolean show) {
		showCalibration = show;
	}

	public void showInterpolation(boolean show) {
		showInterpolation = show;
	}

	public boolean showsCalibration() {
		return showCalibration;
	}

	public boolean showsInterpolation() {
		return showInterpolation;
	}

}
