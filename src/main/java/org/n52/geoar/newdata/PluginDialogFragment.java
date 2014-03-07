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

import org.n52.geoar.R;
import org.n52.geoar.ar.view.IntroController;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class PluginDialogFragment extends DialogFragment {

	public static PluginDialogFragment newInstance(PluginHolder plugin) {
		PluginDialogFragment df = new PluginDialogFragment();
		Bundle args = new Bundle();

		args.putParcelable("plugin", plugin);
		df.setArguments(args);
		return df;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// get parameters
		Bundle args = getArguments();
		final PluginHolder plugin = args.getParcelable("plugin");

		// inflate layout
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_plugin_dialog, null);

		final ImageView imageView = (ImageView) view
				.findViewById(R.id.imageView);

		((TextView) view.findViewById(R.id.textViewName)).setText(plugin
				.getName());
		((TextView) view.findViewById(R.id.textViewPublisher)).setText(plugin
				.getPublisher() != null ? plugin.getPublisher()
				: getString(R.string.unknown_publisher));
		((TextView) view.findViewById(R.id.textViewVersion)).setText(plugin
				.getVersion() != null ? "" + plugin.getVersion()
				: getString(R.string.no_value));

		((TextView) view.findViewById(R.id.textViewDescription)).setText(plugin
				.getDescription() != null ? plugin.getDescription()
				: getString(R.string.no_value));

		TextView textViewDataSources = (TextView) view
				.findViewById(R.id.textViewDataSources);
		if (plugin instanceof InstalledPluginHolder) {

			String dsText = "";
			for (DataSourceHolder dataSource : ((InstalledPluginHolder) plugin)
					.getDataSources()) {
				if (!dsText.isEmpty())
					dsText += "\n";
				dsText += dataSource.getName();
			}
			textViewDataSources.setText(dsText);
		} else {
			textViewDataSources.setText(R.string.no_value);
		}

		// dialogButton.setAnimation(getActivity().findViewById(android.R.drawable.stat_sys_download));
		Dialog dsDialog = new AlertDialog.Builder(getActivity())
				.setTitle(plugin.getName())
				.setPositiveButton(R.string.add,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (plugin instanceof InstalledPluginHolder) {
									((InstalledPluginHolder) plugin)
											.setChecked(true);
									IntroController.finishTaskIfActive(R.string.intro_task_2);
									IntroController.notify(R.string.intro_desc_3_1);
								} else if (plugin instanceof PluginDownloadHolder) {
									PluginDownloader
											.downloadPlugin((PluginDownloadHolder) plugin);
									IntroController.finishTaskIfActive(R.string.intro_task_1);
									IntroController.notify(R.string.intro_desc_2_1);
								}
							}
						}).setNegativeButton(R.string.cancel, null)
				.setView(view).create();

		// Asynchronously load and display plugin icon
		Thread imageThread = new Thread(new Runnable() {
			@Override
			public void run() {
				final Bitmap pluginIcon = plugin.getPluginIcon();
				Activity activity = getActivity();
				if (activity != null) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (pluginIcon != null)
								imageView.setImageBitmap(pluginIcon);
						}
					});
				}
			}
		});
		imageThread.start();

		return dsDialog;
	}
}
