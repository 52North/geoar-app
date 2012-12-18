package org.n52.android.newdata;

import org.n52.android.newdata.DataSourceInstanceHolder;
import org.n52.android.newdata.filter.AbstractSettingsDialogActivity;

public class DataSourceInstanceDialogActivity extends AbstractSettingsDialogActivity {

	@Override
	protected Object getSettingsObject() {
		DataSourceInstanceHolder dataSourceInstance = getIntent()
				.getParcelableExtra("dataSourceInstance");
		return dataSourceInstance.getDataSource();
	}

}
