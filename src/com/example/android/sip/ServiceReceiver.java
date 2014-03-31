package com.example.android.sip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class ServiceReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context c, Intent intent) {
		// TODO Auto-generated method stub
		Bundle bundle=intent.getExtras();
		if(bundle==null)
			return;
		String state = bundle.getString(TelephonyManager.EXTRA_STATE);
		if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING))
		{
			try
			{
				if(WalkieTalkieActivity.call!=null)
				{
					if(!WalkieTalkieActivity.call.isOnHold() && !WalkieTalkieActivity.callHeld && IncomingCallActivity.callHeld)
					{
//						Toast.makeText(c, "Before hold", Toast.LENGTH_LONG).show();
						WalkieTalkieActivity.incomingCallHeld=true;
						WalkieTalkieActivity.call.holdCall(30);
//						Toast.makeText(c, "After hold", Toast.LENGTH_LONG).show();
					}
				}
//				Toast.makeText(c, "call aayi "+WalkieTalkieActivity.call, Toast.LENGTH_LONG).show();
			}
			catch(Exception e)
			{
				Log.d("error in service receiver",""+e);
			}
		}
		else if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE))
		{
			try
			{
				if(WalkieTalkieActivity.call!=null)
				{
					if(WalkieTalkieActivity.incomingCallHeld)
					{
//						Toast.makeText(c, "Before continue", Toast.LENGTH_LONG).show();
						WalkieTalkieActivity.incomingCallHeld=false;
						WalkieTalkieActivity.call.continueCall(30);
//						Toast.makeText(c, "After continue", Toast.LENGTH_LONG).show();
					}
				}
//				Toast.makeText(c, "call khatam "+WalkieTalkieActivity.call, Toast.LENGTH_LONG).show();
			}
			catch(Exception e)
			{
				Log.d("error in service receiver",""+e);
			}
		}
	}

}
