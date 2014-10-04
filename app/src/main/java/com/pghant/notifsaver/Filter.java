package com.pghant.notifsaver;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Filter implements Parcelable {

	private String text;
	private String title;
	private String packageName;
	private String appName;
	private String name;
	private boolean exactText;
	private boolean exactTitle;
	private boolean exactPackageName;
	private boolean exactAppName;

	public static final Parcelable.Creator<Filter> CREATOR = new Parcelable.Creator<Filter>() {

		public Filter createFromParcel(Parcel in) {
			return new Filter(in);
		}

		public Filter[] newArray(int size) {
			return new Filter[size];
		}
	};

	private Filter(Parcel in) {
		text = in.readString();
		title = in.readString();
		packageName = in.readString();
		appName = in.readString();
		name = in.readString();
		exactText = in.readByte() != 0;
		exactTitle = in.readByte() != 0;
		exactPackageName = in.readByte() != 0;
		exactAppName = in.readByte() != 0;
	}

	public Filter(String name, String text, String title, String packageName, String appName,
			boolean exactText, boolean exactTitle, boolean exactPackageName, boolean exactAppName) {
		if (TextUtils.isEmpty(name))
			throw new IllegalArgumentException("Filter must specify a name");
		this.name = name;
		this.text = text == null ? "" : text;
		this.title = title == null ? "" : title;
		this.packageName = packageName == null ? "" : packageName;
		this.appName = appName == null ? "" : appName;
		this.exactAppName = exactAppName;
		this.exactPackageName = exactPackageName;
		this.exactText = exactText;
		this.exactTitle = exactTitle;
	}

	public String getText() {
		return text;
	}

	public String getTitle() {
		return title;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getAppName() {
		return appName;
	}

	public String getName() {
		return name;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(text);
		dest.writeString(title);
		dest.writeString(packageName);
		dest.writeString(appName);
		dest.writeString(name);
		dest.writeByte((byte) (exactText ? 1 : 0));
		dest.writeByte((byte) (exactTitle ? 1 : 0));
		dest.writeByte((byte) (exactPackageName ? 1 : 0));
		dest.writeByte((byte) (exactAppName ? 1 : 0));
	}

}
