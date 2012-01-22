package org.n52.android.data;

public abstract class DataSourceAbstractFactory {

	public abstract Measurement CreateMeasurement();
	public abstract MeasurementFilter CreateMeasurementFilter();
	public abstract MeasurementManager CreateMeasurementManager();
}
