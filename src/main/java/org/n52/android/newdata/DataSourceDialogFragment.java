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

import org.n52.android.data.ImageLoader;
import org.n52.android.geoar.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DataSourceDialogFragment extends DialogFragment {

	static DataSourceDialogFragment newInstance(
			DataSourceHolder dataSourceHolder) {
		DataSourceDialogFragment df = new DataSourceDialogFragment();
		Bundle args = new Bundle();

		args.putParcelable("dataSource", dataSourceHolder);
		df.setArguments(args);
		return df;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// get paramters
		Bundle args = getArguments();
		final DataSourceHolder dataSource = args.getParcelable("dataSource");

		// inflate layout
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View v = inflater.inflate(R.layout.cb_dialog_fragment, null);

		TextView textView = (TextView) v.findViewById(R.id.cb_dialog_textview);
		textView.setText(dataSource.getDescription() != "" ? dataSource
				.getDescription() : "No Description");

		ImageView imageView = (ImageView) v.findViewById(R.id.cb_dialog_image);
		ImageLoader.getInstance().displayImage("", imageView); // TODO
		// imageView.setImageBitmap();

		// dialogButton.setAnimation(getActivity().findViewById(android.R.drawable.stat_sys_download));
		Dialog dsDialog = new AlertDialog.Builder(getActivity())
				.setTitle(dataSource.getName())
				.setPositiveButton("Add",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								DataSourceLoader.getInstance().addDataSource(
										dataSource);
							}
						}).setNegativeButton("Cancel", null).setView(v)
				.create();

		return dsDialog;
	}

}
