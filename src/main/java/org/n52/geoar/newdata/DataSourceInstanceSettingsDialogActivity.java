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
package org.n52.geoar.newdata;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.n52.geoar.R;
import org.n52.geoar.settings.SettingsView;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * Activity to set data source instance settings and its filter settings. Also
 * responsible for updating the data source instance.
 * 
 */
public class DataSourceInstanceSettingsDialogActivity extends SherlockActivity {

	public abstract static class SettingsResultListener implements Parcelable {
		private static List<WeakReference<SettingsResultListener>> instances = new ArrayList<WeakReference<SettingsResultListener>>();
		private int id = nextId++;
		private static int nextId = 0;

		public static final Parcelable.Creator<SettingsResultListener> CREATOR = new Parcelable.Creator<SettingsResultListener>() {
			@Override
			public SettingsResultListener createFromParcel(Parcel in) {
				int id = in.readInt();
				// Find SettingsResultListener with provided id
				Iterator<WeakReference<SettingsResultListener>> it = SettingsResultListener.instances
						.iterator();
				while (it.hasNext()) {
					WeakReference<SettingsResultListener> next = it.next();
					if (next.get() == null) {
						it.remove();
					} else if (next.get().id == id) {
						return next.get();
					}
				}
				return null;
			}

			@Override
			public SettingsResultListener[] newArray(int size) {
				return new SettingsResultListener[size];
			}
		};

		public SettingsResultListener() {
			instances.add(new WeakReference<SettingsResultListener>(this));
		}

		abstract void onSettingsResult(int resultCode);

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(id);
		}
	}

	private SettingsResultListener resultListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_datasourceinstance_dialog);

		final SettingsView generalSettingsView = (SettingsView) findViewById(R.id.settingsViewGeneral);
		final SettingsView filterSettingsView = (SettingsView) findViewById(R.id.settingsViewFilter);

		final DataSourceInstanceHolder dataSourceInstance = getIntent()
				.getParcelableExtra("dataSourceInstance");
		resultListener = getIntent().getParcelableExtra("resultListener");
		generalSettingsView.setStringsContext(dataSourceInstance.getParent()
				.getPluginHolder().getPluginContext());
		filterSettingsView.setStringsContext(dataSourceInstance.getParent()
				.getPluginHolder().getPluginContext());

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
				setDialogResult(RESULT_CANCELED);
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
					setDialogResult(RESULT_OK);
					finish();
				}
			}
		});
	}

	/**
	 * Should be used instead of {@link Activity#setResult(int)} to use a passed
	 * {@link SettingsResultListener}
	 * 
	 * @param resultCode
	 */
	private void setDialogResult(int resultCode) {
		setResult(resultCode);
		if (resultListener != null) {
			resultListener.onSettingsResult(resultCode);
		}
	}

}
