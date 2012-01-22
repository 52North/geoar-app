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

import java.util.Calendar;

/**
 * 
 * Class to hold information about the filter to use for data request
 * 
 * @author Holger Hopmann
 * 
 */
public abstract class MeasurementFilter {
	// package scope
	public Calendar timeFrom, timeTo;
	public Integer hourFrom;
	public Integer hourTo;

	public Integer getHourFrom() {
		return hourFrom;
	}

	public Integer getHourTo() {
		return hourTo;
	}

	public Calendar getTimeFrom() {
		return timeFrom;
	}

	public Calendar getTimeTo() {
		return timeTo;
	}

	public void setHourFrom(Integer integer) {
		this.hourFrom = integer;
	}

	public void setHourTo(Integer hourTo) {
		this.hourTo = hourTo;
	}

	public void setTimeFrom(Calendar timeFrom) {
		this.timeFrom = timeFrom;
	}

	public void setTimeTo(Calendar timeTo) {
		this.timeTo = timeTo;
	}

	public void apply(MeasurementFilter tempFilter) {
		hourFrom = tempFilter.hourFrom;
		hourTo = tempFilter.hourTo;
		timeFrom = tempFilter.timeFrom;
		timeTo = tempFilter.timeTo;
	}

	public abstract MeasurementFilter clone();

	/**
	 * Checks if a measurement conforms to that filter
	 * 
	 * @param measurement
	 * @return
	 */
	public boolean filter(Measurement measurement) {
		if (measurement.getTime() == null
				&& (timeFrom != null || timeTo != null || hourFrom != null || hourTo != null)) {
			return false;
		}
		if (timeFrom != null && timeFrom.after(measurement.getTime())) {
			return false;
		}
		if (timeTo != null && timeTo.before(measurement.getTime())) {
			return false;
		}
		if (hourFrom != null
				&& measurement.getTime().get(Calendar.HOUR_OF_DAY) < hourFrom) {
			return false;
		}
		if (hourTo != null
				&& measurement.getTime().get(Calendar.HOUR_OF_DAY) > hourTo) {
			return false;
		}
		return true;
	}

	/**
	 * Correct invalid constraints
	 */
	public void validate() {
		if (hourFrom != null && hourTo != null && hourFrom > hourTo) {
			int temp = hourFrom;
			hourFrom = hourTo;
			hourTo = temp;
		}
	}
}
