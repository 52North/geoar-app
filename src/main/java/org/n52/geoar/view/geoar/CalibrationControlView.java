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
package org.n52.geoar.view.geoar;


/**
 * A simple LinearLayout offering a user interface to change the camera's
 * elevation
 * 
 * @author Holger Hopmann
 * 
 */
//public class CalibrationControlView extends LinearLayout implements
//		OnClickListener, CameraUpdateListener, GeoARView {
//
//	private TextView heightInfoTextView;
//	private DecimalFormat distFormat;
//
//	public CalibrationControlView(Context context, AttributeSet attrs) {
//		super(context, attrs);
//		LayoutInflater.from(context).inflate(R.layout.calibration_views, this,
//				true);
//
//		// Find child views
//		Button moveUpButton = (Button) findViewById(R.id.buttonCalibMoveUp);
//		Button moveDownButton = (Button) findViewById(R.id.buttonCalibMoveDown);
//		heightInfoTextView = (TextView) findViewById(R.id.textViewCalibInfo);
//		Button okButton = (Button) findViewById(R.id.buttonCalibOk);
//
//		// Set click listeners
//		moveDownButton.setOnClickListener(this);
//		moveUpButton.setOnClickListener(this);
//		okButton.setOnClickListener(this);
//
//		distFormat = new DecimalFormat("0.00 m");
//		onCameraUpdate();
//	}
//
//	@Override
//	protected void onVisibilityChanged(View changedView, int visibility) {
//		if (isShown()) {
//			RealityCamera.addCameraUpdateListener(this);
//		} else {
//			RealityCamera.removeCameraUpdateListener(this);
//		}
//		super.onVisibilityChanged(changedView, visibility);
//	}
//
//	public void onClick(View v) {
//		switch (v.getId()) {
//		case R.id.buttonCalibMoveDown:
//			// Decreases camera height
//			RealityCamera.changeHeight(-0.1f);
//			break;
//		case R.id.buttonCalibMoveUp:
//			// Increases camera height
//			RealityCamera.changeHeight(0.1f);
//			break;
//		case R.id.buttonCalibOk:
//			// Hide on OK
//			setVisibility(View.GONE);
//			break;
//		}
//	}
//
//	public boolean isVisible() {
//		return isShown();
//	}
//
//	public void onCameraUpdate() {
//		post(new Runnable() {
//			public void run() {
//				heightInfoTextView.setText(distFormat
//						.format(RealityCamera.height));
//			}
//		});
//	}
//
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//		case R.id.item_calibrate:
//			setVisibility(VISIBLE);
//			// Allow others views to receive this event too
//			break;
//		}
//		return false;
//	}
//
//	public Integer getMenuGroupId() {
//		return null;
//	}
//	
//	@Override
//	public void setInfoHandler(InfoView infoHandler) {
//	}
//
//	public void onSaveInstanceState(Bundle outState) {
//		
//	}
//
//	public void onRestoreInstanceState(Bundle savedInstanceState) {
//		
//	}
//
//	@Override
//	public void setMeasureManager(MeasurementManager measureManager) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void setLocationHandler(LocationHandler locationHandler) {
//		// TODO Auto-generated method stub
//		
//	}
//
//}
