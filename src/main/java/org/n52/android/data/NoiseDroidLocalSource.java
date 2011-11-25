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
package org.n52.android.data;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.ConnectException;
import java.util.Iterator;
import java.util.List;

import org.n52.android.geoar.R;

import android.os.Environment;

/**
 * Data source to access locally saved measurements of the NoiseDroid
 * application. Intended just for testing purposes
 * 
 * @author Holger Hopmann
 * 
 */
public class NoiseDroidLocalSource implements DataSource {

	private static String noiseDroidMeasurementsPath = Environment
			.getExternalStorageDirectory()
			+ "/Android/data/de.noisedroid/measures.xml";

	public String getTitle() {
		return "NoiseDroid Applikation";
	}

	public boolean isAvailable() {
		// NoiseDroid data is available if specific file exists on sd card
		return new File(noiseDroidMeasurementsPath).canRead();
	}

	public byte getPreferredRequestZoom() {
		return 10;
	}

	public List<Measurement> getMeasurements(Tile tile, MeasurementFilter filter)
			throws ConnectException, RequestException {
		try {
			Reader reader = new FileReader(noiseDroidMeasurementsPath);

			// Uses capabilities of NoiseDroidServerSource
			List<Measurement> measurements = NoiseDroidServerSource
					.getMeasuresFromResponse(reader);

			Iterator<Measurement> iterator = measurements.iterator();
			while (iterator.hasNext()) {
				if (!filter.filter(iterator.next())) {
					iterator.remove();
				}
			}
			return measurements;
		} catch (Exception e) {
			throw new RequestException(e.getMessage());
		}
	}

	public long getDataReloadMinInterval() {
		// Never
		return -1;
	}

	public Integer getIconDrawableId() {
		return R.drawable.noisedroid;
	}

}
