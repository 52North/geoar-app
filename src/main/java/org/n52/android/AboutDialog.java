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
package org.n52.android;

import org.n52.android.R;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class AboutDialog extends Dialog {

	private static Context context;

	public AboutDialog(Context context) {
		super(context);
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.about_dialog);

		this.setTitle("About GeoAR");

		TextView tv = (TextView) findViewById(R.id.about_legal_text);
		tv.setText(R.string.about_legal_text);
		tv.setTextColor(Color.GRAY);

		tv = (TextView) findViewById(R.id.about_headline);
		tv.setText(Html.fromHtml("<h3>"
				+ context.getString(R.string.about_titel) + "</h3>"));
		tv.setTextColor(Color.DKGRAY);

		final String s = "More information at <br> "
				+ "<center><b>http://52north.org/android</b></center> <br> <br>"
				+ "GeoAR is part of the 52&deg;North Geostatistics Commnity: <br>"
				+ "<b>http://52north.org/communities/geostatistics/</b> <br> <br>"
				+ "Contact: <i>Daniel N&uuml;st (geoar@52north.org)</i> <br>"
				+ "Development Credits: <i> Arne de Wall, Holger Hopmann </i>";

		tv = (TextView) findViewById(R.id.info_more_text);
		tv.setText(Html.fromHtml(s));
		Linkify.addLinks(tv, Linkify.ALL);

		ImageView im = (ImageView) findViewById(R.id.about_image);
		im.setOnClickListener(createLinkOnClickLister("http://52north.org/"));

		im = (ImageView) findViewById(R.id.about_geoviqua_image);
		im.setOnClickListener(createLinkOnClickLister("http://www.geoviqua.org/"));

		super.onCreate(savedInstanceState);
	}

	private static View.OnClickListener createLinkOnClickLister(
			final String link) {
		return new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.addCategory(Intent.CATEGORY_BROWSABLE);
				intent.setData(Uri.parse(link));
				context.startActivity(intent);
			}
		};
	}
}
