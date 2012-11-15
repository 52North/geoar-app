package org.n52.android.newdata;

public class DataSourceDownloadHolder extends PluginHolder {

	private String description;
	private String downloadLink;
	private String imageLink;
	private String identifier;
	private String name;
	private Long version;

	public String getDescription() {
		return description;
	}

	public String getDownloadLink() {
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

	public void setDownloadLink(String downloadLink) {
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

}