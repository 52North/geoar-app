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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcel;

public class PluginDownloadHolder extends PluginHolder {

	private String description;
	private String identifier;
	private String name;
	private Long version;
	private Bitmap pluginIcon;
	private boolean iconLoaded = false;
	private String publisher;

	private static final Logger LOG = LoggerFactory
			.getLogger(PluginDownloadHolder.class);

	public String getDescription() {
		return description;
	}

	public Uri getDownloadLink() {
		return Uri.parse(PluginDownloader.SERVER_URL + "/" + identifier
				+ "/apk");
	}

	public Uri getImageLink() {
		return Uri.parse(PluginDownloader.SERVER_URL + "/" + identifier
				+ "/image");
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPublisher() {
		return publisher;
	}

	@Override
	public Long getVersion() {
		return version;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getClass().getName());
		super.writeToParcel(dest, flags);
	}

	@Override
	public Bitmap getPluginIcon() {
		if (!iconLoaded) {
			try {
				iconLoaded = true;
				URLConnection connection = new URL(getImageLink().toString())
						.openConnection();
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(10000);
				connection.connect();
				BufferedInputStream bis = new BufferedInputStream(
						connection.getInputStream());
				pluginIcon = BitmapFactory.decodeStream(bis);
				bis.close();
			} catch (IOException e) {
				LOG.error("Could not load image " + getImageLink());
			}
		}

		return pluginIcon;
	}

	public boolean isDownloaded() {
		return PluginLoader.getPluginByIdentifier(getIdentifier()) != null;
	}

}