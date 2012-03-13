package org.n52.android.data;

import org.n52.android.data.noise.NoiseDataFactory;
import org.n52.android.view.geoar.Settings.DataSourceSetting;

// FIXME ich denke hier ist der einstigspunkt ;)
public class DataSourceAdapter extends DataSourceAbstractFactory {
	
	private static DataSourceAbstractFactory factory;
	private static DataSourceAdapter singleton;
	private static DataSourceSetting dataSelected = DataSourceSetting.NOISE;
	
	private DataSourceAdapter(DataSourceSetting dataSelected){
		setDataSource(dataSelected);
	}
	
	public static DataSourceAdapter getInstance(){
		if(singleton == null){
			singleton = new DataSourceAdapter(dataSelected);
		}
		return singleton;
	}
	
	public static void setDataSource(DataSourceSetting dataSource){
		switch(dataSelected){
		case NOISE:
			factory = (DataSourceAbstractFactory) new NoiseDataFactory();
			break;
		case AIRPOLUTION:
			factory = null;
			break;
		}
	}
	
	public static DataSourceSetting getSelectedDataSource(){
		return dataSelected;
	}
	
	@Override
	public Measurement CreateMeasurement() {
		return factory.CreateMeasurement();
	}

	@Override
	public MeasurementFilter CreateMeasurementFilter() {
		return factory.CreateMeasurementFilter();
	}

	@Override
	public MeasurementManager CreateMeasurementManager() {
		return factory.CreateMeasurementManager();
	}
 
}
