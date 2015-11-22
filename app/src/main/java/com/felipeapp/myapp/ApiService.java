package com.felipeapp.myapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import com.squareup.okhttp.OkHttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// IntentService which performs the HTTP calls to talk to the Particle Cloud.

public class ApiService extends IntentService {

    // Key to retrieve the API response from the Bundle for the ResultReceiver
	public static final String EXTRA_API_RESPONSE_JSON = "EXTRA_API_RESPONSE_JSON";
	public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
	public static final String EXTRA_ERROR_MSG = "EXTRA_ERROR_MSG";

    // Key to handle request intents
    private static final String NS = ApiService.class.getCanonicalName() + ".";
    private static final String ACTION_POST = NS + "ACTION_POST";
    private static final String EXTRA_PATH_SEGMENTS = NS + "EXTRA_PATH_SEGMENTS";
    private static final String EXTRA_REQUEST_DATA = NS + "EXTRA_REQUEST_DATA";
    private static final String EXTRA_RESULT_RECEIVER = NS + "EXTRA_RESULT_RECEIVER";
    private static final String EXTRA_ACCESS_TOKEN = "EXTRA_ACCESS_TOKEN";

    OkHttpClient okHttpClient;
    ByteArrayOutputStream reusableResponseStream = new ByteArrayOutputStream(8192);

    public ApiService() {
        super(ApiService.class.getSimpleName());
        okHttpClient = new OkHttpClient();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setIntentRedelivery(false);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Bundle extras = intent.getExtras();
        String action = intent.getAction();
        Response response = null;
        int resultCode = -1;
        Bundle resultData = new Bundle();

        if (ACTION_POST.equals(action)) {
            Log.i("Request type", "post");
            String[] pathSegments = extras.getStringArray(EXTRA_PATH_SEGMENTS);
            String accessToken = extras.getString(EXTRA_ACCESS_TOKEN);
            URL url = buildPostUrl(pathSegments, accessToken);
            Bundle requestBody = extras.getBundle(EXTRA_REQUEST_DATA);
            String postData = URLEncodedUtils.format(bundleParamsToNameValuePairs(requestBody), HTTP.UTF_8);
            response = performRequestWithInputData(url, "POST", postData);
        }

        if (response != null) {
            resultCode = response.responseCode;
            resultData.putString(EXTRA_API_RESPONSE_JSON, response.apiResponse);
            resultData.putInt(EXTRA_RESULT_CODE, resultCode);
        } else {
            resultData.putString(EXTRA_ERROR_MSG, "Error communicating with server");
        }

        Log.i("Response", response.apiResponse);

        ResultReceiver receiver = extras.getParcelable(EXTRA_RESULT_RECEIVER);

        if (receiver != null) {
            receiver.send(resultCode, resultData);
        }

    }

    // Perform a POST request
    public static void post(Context ctx, String[] resourcePathSegments,
                            Bundle postData, ResultReceiver resultReceiver, String coreAccessToken) {

        Intent intent = new Intent(ctx, ApiService.class);
        intent.setAction(ACTION_POST);

        if (resourcePathSegments != null) {
            intent.putExtra(EXTRA_PATH_SEGMENTS, resourcePathSegments);
        }

        if (postData != null) {
            intent.putExtra(EXTRA_REQUEST_DATA, postData);
        }

        if (resultReceiver != null) {
            intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
        }

        if (resultReceiver != null) {
            intent.putExtra(EXTRA_ACCESS_TOKEN, coreAccessToken);
        }

        ctx.startService(intent);

    }

	Response performRequestWithInputData(URL url, String httpMethod, String stringData) {

        HttpURLConnection connection = okHttpClient.open(url);
		OutputStream out = null;
		InputStream in = null;
		int responseCode = -1;
		String responseData = "";

        Log.i("url",url.toString());
        Log.i("httpMethod", httpMethod);
        Log.i("stringData", stringData);

		try {
			try {
				connection.setRequestMethod(httpMethod);
				connection.setDoOutput(true);

				out = connection.getOutputStream();
				out.write(stringData.getBytes(HTTP.UTF_8));
				out.close();

				responseCode = connection.getResponseCode();

				in = connection.getInputStream();
				responseData = readAsUtf8String(in);
			} finally {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			}
		} catch (IOException e) {
			Log.i("Error trying to make: ", connection.getRequestMethod() + " request");
		}

		return new Response(responseCode, responseData);


	}

	URL buildPostUrl(String[] pathSegments, String token) {
		Uri.Builder uriBuilder = ApiUrlHelper.buildUri(token, pathSegments);
		return ApiUrlHelper.convertToURL(uriBuilder);
	}

	List<NameValuePair> bundleParamsToNameValuePairs(Bundle params) {
		List<NameValuePair> paramList = new ArrayList<NameValuePair>();
		for (String key : params.keySet()) {
			Object value = params.get(key);
			if (value != null) {
				paramList.add(new BasicNameValuePair(key, value.toString()));
			}
		}
		return paramList;
	}

	String readAsUtf8String(InputStream in) throws IOException {
		reusableResponseStream.reset();
		byte[] buffer = new byte[1024];
		for (int count; (count = in.read(buffer)) != -1;) {
			reusableResponseStream.write(buffer, 0, count);
		}
		return reusableResponseStream.toString(HTTP.UTF_8);
	}

	private static class Response {

		public final int responseCode;
		public final String apiResponse;

		public Response(int responseCode, String apiResponse) {
			this.responseCode = responseCode;
			this.apiResponse = apiResponse;
		}

		@Override
		public String toString() {
			return "RequestResult [resultCode=" + responseCode + ", apiResponse=" + apiResponse
					+ "]";
		}

	}

}
