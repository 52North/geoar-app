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
package org.n52.android.dialog;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.data.DataSource;
import org.n52.android.data.MeasurementManager;
import org.n52.android.geoar.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

/**
 * Dialog to change the data source providing measurements
 * 
 * @author Holger Hopmann
 */
public class DataSourceDialog extends AlertDialog {

	private DataSource selectedSource;
	private List<ToggleButton> toggleButtons = new ArrayList<ToggleButton>();

	public DataSourceDialog(Context context, DataSource[] dataSources,
			final MeasurementManager measurementManager) {
		super(context);

		selectedSource = measurementManager.getDataSource();

		LinearLayout layout = new LinearLayout(context);
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));

		for (final DataSource source : dataSources) {
			final ToggleButton button = new ToggleButton(context);
//			button.setTextOn(source.getTitle());
//			button.setTextOff(source.getTitle());
//			button.setEnabled(source.isAvailable());
//			button.setChecked(source.getClass().equals(
//					measurementManager.getDataSource().getClass()));
//			if (source.getIconDrawableId() != null) {
//				button.setCompoundDrawablesWithIntrinsicBounds(0,
//						source.getIconDrawableId(), 0, 0);
//			}
			button.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					if (isChecked) {
						unselectOtherButtons(button);
						selectedSource = source;
					}
				}
			});
			toggleButtons.add(button);
			layout.addView(button, LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
		}

		// Set Dialog Options
		setView(layout);
		setCancelable(true);
		setTitle(context.getString(R.string.select_source));
		setButton(BUTTON_POSITIVE, context.getString(R.string.ok),
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Change source if selected Source is different to
						// current source
						if (selectedSource != measurementManager
								.getDataSource()) {
							measurementManager.setDataSource(selectedSource);
						}
					}
				});
		setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel),
				(Message) null);

	}

	/**
	 * Unselect all buttons in this dialog except for refButton
	 * 
	 * @param refButton
	 */
	private void unselectOtherButtons(ToggleButton refButton) {
		for (ToggleButton button : toggleButtons) {
			if (button != refButton) {
				button.setChecked(false);
			}
		}
	}
}
