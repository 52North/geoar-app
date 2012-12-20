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
package org.n52.android.newdata;

import org.n52.android.R;
import org.n52.android.newdata.settings.SettingsView;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockActivity;

public class DataSourceInstanceSettingsDialogActivity extends SherlockActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_datasourceinstance_dialog);

		final SettingsView generalSettingsView = (SettingsView) findViewById(R.id.settingsViewGeneral);
		final SettingsView filterSettingsView = (SettingsView) findViewById(R.id.settingsViewFilter);

		DataSourceInstanceHolder dataSourceInstance = getIntent()
				.getParcelableExtra("dataSourceInstance");
		generalSettingsView.setSettingsObject(dataSourceInstance
				.getDataSource());
		filterSettingsView.setSettingsObject(dataSourceInstance
				.getCurrentFilter());

		if (generalSettingsView.isEmpty()) {
			findViewById(R.id.textViewGroupGeneral).setVisibility(View.GONE);
		}
		if (filterSettingsView.isEmpty()) {
			findViewById(R.id.textViewGroupFilter).setVisibility(View.GONE);
		}

		Button cancelButton = (Button) findViewById(R.id.negativeButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		Button okButton = (Button) findViewById(R.id.positiveButton);
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (generalSettingsView.validate()
						&& filterSettingsView.validate()) {
					generalSettingsView.updateObject();
					filterSettingsView.updateObject();
					setResult(RESULT_OK);
					finish();
				}
			}
		});

	}
}
