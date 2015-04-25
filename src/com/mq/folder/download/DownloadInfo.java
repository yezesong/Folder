package com.mq.folder.download;

import android.os.Parcel;
import android.os.Parcelable;

public class DownloadInfo implements Parcelable {

	private int id;

	private String filename;

	private String localname;

	private String url;

	private int filesize;

	private int completesize;

	private int status;// 0:none,1:downloading,2:pause,3:stop

	public Integer getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getLocalname() {
		return localname;
	}

	public void setLocalname(String localname) {
		this.localname = localname;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getFilesize() {
		return filesize;
	}

	public void setFilesize(int filesize) {
		this.filesize = filesize;
	}

	public int getCompletesize() {
		return completesize;
	}

	public void setCompletesize(int completesize) {
		this.completesize = completesize;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public DownloadInfo() {
	}

	public DownloadInfo(int id, String filename, String localname, String url, int filesize, int completesize,
			int status) {
		super();
		this.id = id;
		this.filename = filename;
		this.localname = localname;
		this.url = url;
		this.filesize = filesize;
		this.completesize = completesize;
		this.status = status;
	}

	@Override
	public String toString() {
		return "DownloadInfo [id=" + id + ", filename=" + filename + ", localname=" + localname + ", url=" + url
				+ ", filesize=" + filesize + ", completesize=" + completesize + ", status=" + status + "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(filename);
		dest.writeString(localname);
		dest.writeString(url);
		dest.writeInt(filesize);
		dest.writeInt(completesize);
		dest.writeInt(status);
	}

	public static final Parcelable.Creator<DownloadInfo> CREATOR = new Creator<DownloadInfo>() {

		@Override
		public DownloadInfo[] newArray(int size) {
			return new DownloadInfo[size];
		}

		@Override
		public DownloadInfo createFromParcel(Parcel source) {
			return new DownloadInfo(source.readInt(), source.readString(), source.readString(), source.readString(),
					source.readInt(), source.readInt(), source.readInt());
		}
	};
}
