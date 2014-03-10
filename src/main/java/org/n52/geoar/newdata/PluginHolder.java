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

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public abstract class PluginHolder implements Parcelable {

	public abstract String getIdentifier();

	public abstract String getName();

	public abstract Long getVersion();

	public abstract String getDescription();

	/**
	 * Returns the icon associated with this plugin; that is the icon packaged
	 * with an installed plugin, or the icon provided by the GeoAR webservices
	 * 
	 * @return
	 */
	public abstract Bitmap getPluginIcon();

	public abstract String getPublisher();

	@Override
	public boolean equals(Object o) {
		if (o instanceof PluginHolder) {
			PluginHolder other = (PluginHolder) o;
			if (getIdentifier() == null)
				return false;

			if ((getVersion() == null && other.getVersion() == null)
					|| (getVersion() != null && getVersion().equals(
							other.getVersion()))) {
				return getIdentifier().equals(other.getIdentifier());
			} else {
				return false;
			}
		}

		return super.equals(o);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime;
		result = prime * result
				+ ((getIdentifier() == null) ? 0 : getIdentifier().hashCode());
		result = prime * result
				+ ((getVersion() == null) ? 0 : getVersion().hashCode());

		return result;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getIdentifier());
	}

	public static final Parcelable.Creator<PluginHolder> CREATOR = new Parcelable.Creator<PluginHolder>() {
		public PluginHolder createFromParcel(Parcel in) {
			String className = in.readString();
			String identifier = in.readString();
			if (className.equals(InstalledPluginHolder.class.getName())) {
				return PluginLoader.getPluginByIdentifier(identifier);
			} else if (className.equals(PluginDownloadHolder.class.getName())) {
				return PluginDownloader.getPluginByIdentifier(identifier);
			} else {
				return null;
			}
		}

		public PluginHolder[] newArray(int size) {
			return new InstalledPluginHolder[size];
		}
	};

}
