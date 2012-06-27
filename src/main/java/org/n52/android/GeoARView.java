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
package org.n52.android;

import org.n52.android.data.MeasurementManager;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.view.InfoView;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

/**
 * Interface to allow {@link GeoARActivity} to prepare the options menu for a
 * view.
 * 
 * @author Holger Hopmann
 * 
 */
public interface GeoARView {

	public boolean onOptionsItemSelected(MenuItem item);

	public Integer getMenuGroupId();

	public void setInfoHandler(InfoView infoHandler);
	
	public void setMeasureManager(MeasurementManager measureManager);
	
	public void setLocationHandler(LocationHandler locationHandler);

	public boolean isVisible();

	/**
	 * Used from activities save cycle. Easier to use than
	 * {@link View#onSaveInstanceState}
	 * 
	 * @param outState
	 */
	public void onSaveInstanceState(Bundle outState);

	/**
	 * Used from activities save cycle. Easier to use than
	 * {@link View#onRestoreInstanceState}
	 * 
	 * @param outState
	 */
	public void onRestoreInstanceState(Bundle savedInstanceState);

}
