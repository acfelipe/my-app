package com.felipeapp.myapp;

import android.net.Uri;
import android.util.Log;
import java.net.MalformedURLException;
import java.net.URL;

public class ApiUrlHelper {

	private static Uri baseUri;

	public static Uri.Builder buildUri(String authToken, String... pathSegments) {
		Uri.Builder builder = getBaseUriBuilder().appendPath("v1");
		for (String segment : pathSegments) {
			builder.appendPath(segment);
		}
		builder.appendQueryParameter("access_token", authToken);
		return builder;
	}

	public synchronized static Uri.Builder getBaseUriBuilder() {
		if (baseUri == null) {
			baseUri = new Uri.Builder()
					.scheme("https")
					.encodedAuthority("api.spark.io" + ":" + 443)
					.build();
		}
		return baseUri.buildUpon();
	}

	public static URL convertToURL(Uri.Builder uriBuilder) {
		Uri builtUri = uriBuilder.build();
		try {
			return new URL(builtUri.toString());
		} catch (MalformedURLException e) {
			Log.i("Error", "Unable to build URL from Uri");
			return null;
		}
	}
}
