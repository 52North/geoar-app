package org.n52.android.data.noise;

import org.n52.android.data.DataSourceAbstractFactory;
import org.n52.android.data.DataSourceHandler.DataSource;
import org.n52.android.data.Measurement;
import org.n52.android.data.MeasurementFilter;
import org.n52.android.data.MeasurementManager;

@DataSource(value = "Noise")
public class NoiseDataFactory extends DataSourceAbstractFactory {

	@Override
	public Measurement CreateMeasurement() {
		return (Measurement) new NoiseMeasurement();
	}

	@Override
	public MeasurementFilter CreateMeasurementFilter() {
		return (MeasurementFilter) new NoiseMeasurementFilter();
	}

	@Override
	public MeasurementManager CreateMeasurementManager() {
		return (MeasurementManager) new NoiseMeasurementManager(new NoiseDroidServerSource());
	}

}
