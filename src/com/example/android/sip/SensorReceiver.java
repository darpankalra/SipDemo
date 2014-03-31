package com.example.android.sip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorReceiver extends BroadcastReceiver {

	public static final String ACTION_SENSOR = "com.example.android.sip.SENSOR";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		WalkieTalkieActivity wtActivity = (WalkieTalkieActivity)context;
		String text = intent.getStringExtra(IncomingCallActivity.PARAM_OUT_MSG2);
		if(text.equalsIgnoreCase("register"))
		{
			if(wtActivity.mSensorManager!=null)
			{
				Log.d("registerSensor", "i m here");
				wtActivity.mSensorManager.registerListener(wtActivity, wtActivity.mSensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
		else if(text.equalsIgnoreCase("unregister"))
		{
			if(wtActivity.mSensorManager!=null)
			{
				Log.d("unregisterSensor", "i m here");
				wtActivity.mSensorManager.unregisterListener(wtActivity);
			}
		}
	}

}
