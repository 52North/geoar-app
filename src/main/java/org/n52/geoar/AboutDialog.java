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
package org.n52.geoar;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class AboutDialog extends Dialog {

	private Context context;

	public AboutDialog(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.about_dialog);

		this.setTitle(R.string.about_geoar);

		ImageView im = (ImageView) findViewById(R.id.imageView52n);
		im.setOnClickListener(createLinkOnClickLister("http://52north.org/"));

		im = (ImageView) findViewById(R.id.imageViewGeoviqua);
		im.setOnClickListener(createLinkOnClickLister("http://www.geoviqua.org/"));

		super.onCreate(savedInstanceState);
	}

	private View.OnClickListener createLinkOnClickLister(final String link) {
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
