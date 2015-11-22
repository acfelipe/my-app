package com.felipeapp.myapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

// Interface for making requests and handling responses from the Particle API

public class ApiFacade {

    // Broadcast receiver actions
    public static final String BROADCAST_TINKER_RESPONSE_RECEIVED = "BROADCAST_TINKER_RESPONSE_RECEIVED";
    public static final String EXTRA_TINKER_RESPONSE = "EXTRA_TINKER_RESPONSE";

    private static ApiFacade instance = null;
    final Context ctx;
    final Handler handler;
    final LocalBroadcastManager broadcastMgr;

	public static ApiFacade getInstance(Context context) {
		if (instance == null) {
			instance = new ApiFacade(context.getApplicationContext());
		}
		return instance;
	}

	private ApiFacade(Context context) {
		this.ctx = context.getApplicationContext();
		this.handler = new Handler();
        this.broadcastMgr = LocalBroadcastManager.getInstance(this.ctx);
	}

	public void digitalWrite(String coreId, String coreAccessToken, String pinId, DigitalValue newValue) {

		WriteValueReceiver receiver = new WriteValueReceiver(handler,
                ParticleResponse.REQUEST_TYPE_WRITE, coreId, pinId,
                ParticleResponse.RESPONSE_TYPE_DIGITAL, newValue.asInt());
		Bundle postData = new Bundle();
		postData.putString("params", pinId + "," + newValue.name());
        String[] resourcePathSegments = { "devices", coreId, "digitalwrite" };
		ApiService.post(ctx, resourcePathSegments, postData, receiver, coreAccessToken);

	}

	public static class WriteValueReceiver extends ResultReceiver {

        final int requestType;
        final String coreId;
        final String pinId;
        final int valueType;
        final int newValue;

        public WriteValueReceiver(Handler handler, int requestType, String coreId,
                                  String pinId, int valueType, int newValue) {
            super(handler);
            this.requestType = requestType;
            this.coreId = coreId;
            this.pinId = pinId;
            this.valueType = valueType;
            this.newValue = newValue;
        }

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
		    if (resultCode == HttpStatus.SC_OK) {
                try {
                    String jsonData = resultData.getString(ApiService.EXTRA_API_RESPONSE_JSON);
                    JSONObject json = new JSONObject(jsonData);
                    if (json.has("return_value")) {
                        int returnVal = json.getInt("return_value");
                        Log.i("Return value", Integer.toString(returnVal));
                        if (returnVal==1) {
                            broadcastResponse();
                        }
                    } else if (json.has("error")) {
                        int error = json.getInt("error");
                        Log.i("Api Error", Integer.toString(error));
                    }
                } catch (JSONException e) {
                    Log.i("Error", "Unable to get result from response JSON");
                }
            } else {
                Log.i("Response Error", Integer.toString(resultCode));
            }
        }

        private void broadcastResponse() {
            ParticleResponse response = new ParticleResponse(requestType, coreId, pinId, valueType,
                    newValue, false);
            Intent intent = new Intent(BROADCAST_TINKER_RESPONSE_RECEIVED)
                    .putExtra(EXTRA_TINKER_RESPONSE, response);
            instance.broadcastMgr.sendBroadcast(intent);
        }

	}

}
