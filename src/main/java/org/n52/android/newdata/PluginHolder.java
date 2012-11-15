package org.n52.android.newdata;

public abstract class PluginHolder {

	public abstract String getIdentifier();

	public abstract String getName();

	public abstract Long getVersion();

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
}
