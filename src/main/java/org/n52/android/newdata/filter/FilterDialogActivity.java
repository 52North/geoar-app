package org.n52.android.newdata.filter;

import org.n52.android.newdata.DataSourceInstanceHolder;

public class FilterDialogActivity extends AbstractSettingsDialogActivity {

	@Override
	protected Object getSettingsObject() {
		DataSourceInstanceHolder dataSourceInstance = getIntent()
				.getParcelableExtra("dataSourceInstance");
		return dataSourceInstance.getCurrentFilter();
	}

}
