package org.n52.android.data.noise;

import java.util.Calendar;

import org.n52.android.data.MeasurementFilter;

public class NoiseMeasurementFilter extends MeasurementFilter {

	@Override
	public NoiseMeasurementFilter clone() {
		NoiseMeasurementFilter cloneFilter = new NoiseMeasurementFilter();
		if (hourFrom != null) {
			cloneFilter.hourFrom = new Integer(hourFrom);
		}
		if (hourTo != null) {
			cloneFilter.hourTo = new Integer(hourTo);
		}
		if (timeFrom != null) {
			cloneFilter.timeFrom = (Calendar) timeFrom.clone();
		}
		if (timeTo != null) {
			cloneFilter.timeTo = (Calendar) timeTo.clone();
		}
		return cloneFilter;
	}

}
