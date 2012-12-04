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

import android.net.Uri;
import android.os.Parcel;

public class PluginDownloadHolder extends PluginHolder {

	private String description;
	private Uri downloadLink;
	private String imageLink;
	private String identifier;
	private String name;
	private Long version;

	public String getDescription() {
		return description;
	}

	public Uri getDownloadLink() {
		return downloadLink;
	}

	public String getImageLink() {
		return imageLink;
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
	public Long getVersion() {
		return version;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDownloadLink(Uri downloadLink) {
		this.downloadLink = downloadLink;
	}

	public void setImageLink(String imageLink) {
		this.imageLink = imageLink;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getClass().getName());
		super.writeToParcel(dest, flags);
	}

}