package com.felipeapp.myapp;

import android.os.Parcel;
import android.os.Parcelable;

public class ParticleResponse implements Parcelable {

	public static final int RESPONSE_TYPE_DIGITAL = 1;
	public static final int RESPONSE_TYPE_ANALOG = 2;
	public static final int REQUEST_TYPE_READ = 3;
	public static final int REQUEST_TYPE_WRITE = 4;

	public final int requestType;
	public final String coreId;
	public final String pin;
	public final int responseValue;
	public final int responseType;

	public ParticleResponse(int requestType, String coreId, String pin, int responseType,
							int responseValue, boolean hasErrors) {
		this.requestType = requestType;
		this.coreId = coreId;
		this.pin = pin;
		this.responseType = responseType;
		this.responseValue = responseValue;
	}

	public ParticleResponse(Parcel in) {
		this.requestType = in.readInt();
		this.coreId = in.readString();
		this.pin = in.readString();
		this.responseType = in.readInt();
		this.responseValue = in.readInt();
	}

	@Override
	public String toString() {
		return "TinkerResponse [requestType=" + requestType + ", coreId=" + coreId + ", pin=" + pin
				+ ", responseValue=" + responseValue + ", responseType=" + responseType + "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(requestType);
		dest.writeString(coreId);
		dest.writeString(pin);
		dest.writeInt(responseType);
		dest.writeInt(responseValue);
	}

	public static final Creator<ParticleResponse> CREATOR = new Creator<ParticleResponse>() {

		public ParticleResponse createFromParcel(Parcel in) {
			return new ParticleResponse(in);
		}

		public ParticleResponse[] newArray(int size) {
			return new ParticleResponse[size];
		}

	};

}
