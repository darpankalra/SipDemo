package com.example.android.sip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ParserError")
public class IncomingCallActivity extends Activity {
	
	FileOutputStream fos;
	ObjectOutputStream oos;
	File file;
	static boolean callHeld=true;
	static boolean flag;
	boolean accept;
	SipAudioCall incomingCall=null;
	SipAudioCall.Listener listener;
	private Handler myHandler = new Handler();
	AudioManager am;
	NotificationManager mNotificationManager;
	boolean missed=false;
	String peerAddress;
//	SensorManager mSensorManager;
//	Sensor mSensor;
	public static String PARAM_OUT_MSG = "omsg";
	public static String PARAM_OUT_MSG2 = "omsg2";
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.incomingcall);
		am=(AudioManager)getSystemService(AUDIO_SERVICE);
		am.setMode(AudioManager.MODE_RINGTONE);
		
		String ns=NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) getSystemService(ns);
		
        missed=false;
        
		Intent intent=getIntent();
		final Context c=getApplicationContext();
        try {

//        	final WalkieTalkieActivity wtActivity = (WalkieTalkieActivity) context;
            listener = new SipAudioCall.Listener() {
            	
            	@Override
                public void onCallEstablished(final SipAudioCall call) {
            		
            		runOnUiThread(new Runnable() {
                        public void run() {
                        	Toast.makeText(c, "onCallEstablished", Toast.LENGTH_LONG).show();
                        }
                    });
            		Log.d("on call established", "i m here");
            		if(call!=null && call.isInCall() && !callHeld)
            		{
            			callHeld=true;
//            			wtActivity.updateStatus(call);
            			sendUpdateBroadcast(""+call.getPeerProfile().getUserName() + "@" + call.getPeerProfile().getSipDomain());
            		}
            		else if(call!=null && call.isInCall() && callHeld)
            			{
            				callHeld=false;
//            				wtActivity.updateStatus("Call held");
            				sendUpdateBroadcast("Call held");
            				return;
            			}
            	}
            	
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    try {
                    	if(accept)
                    	{
                    		runOnUiThread(new Runnable() {
                                public void run() {
                                	Toast.makeText(c, "onRinging", Toast.LENGTH_LONG).show();
                                }
                            });
                    		Log.d("on call ringing", "i m here");
//                    		wtActivity.showDialog(12);
                    		call.answerCall(30);
                    		call.startAudio();
                            call.setSpeakerMode(true);
                            AudioManager am;
                            am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                            if(WalkieTalkieActivity.speakerPhone)
                            {
                            	am.setSpeakerphoneOn(true);
                            }
                            if(WalkieTalkieActivity.walkieMode && !call.isMuted()) {
                                call.toggleMute();
                            }
                            WalkieTalkieActivity.call = call;
                            callHeld=false;
//                            wtActivity.call.setListener(listener);
                            WalkieTalkieActivity.startTime=SystemClock.elapsedRealtime();
                        	SimpleDateFormat sdf = new SimpleDateFormat("H:mmaa   EEEE, MMMM d, yyyy");
                        	WalkieTalkieActivity.callDate = sdf.format(new Date());
                        	WalkieTalkieActivity.sipAddress="sip:"+call.getPeerProfile().getUserName() + "@" + call.getPeerProfile().getSipDomain();
                        	WalkieTalkieActivity.outgoingCall=false;
//                        	Log.d("sending intent 1", "i m here");
                        	sendUpdateBroadcast(""+call.getPeerProfile().getUserName() + "@" + call.getPeerProfile().getSipDomain());
                        	
                        	int icon = R.drawable.icon;
                          long when = System.currentTimeMillis();
                         CharSequence contentTitle = "Sip Demo";
                          CharSequence contentText = "Return to ongoing call";
                          
                          Intent notificationIntent = new Intent(getApplicationContext(),WalkieTalkieActivity.class);
                          notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                          PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
                          
                          Notification notification = new Notification(icon, "", when);
                          notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
                          notification.flags |= Notification.FLAG_ONGOING_EVENT;
                          notification.flags |= Notification.FLAG_NO_CLEAR;
                          
                          mNotificationManager.notify(WalkieTalkieActivity.HELLO_ID, notification);
                          
                          sendSensorBroadcast("register");
                    	}
                    	else
                    	{
                    		sendUpdateBroadcast("Call Rejected");
                    		missed=true;
                    		call.endCall();
                    		logMissedCall();
                    	}
                    } catch (Exception e) {
                    	Log.d("error in ringing", "i m here");
                        e.printStackTrace();
                    }
                }
                
                @Override
                public void onRingingBack(SipAudioCall call)
                {
                	Log.d("on ringing back", "i m here");
                	Toast.makeText(c, "onRingingBack", Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onCalling(SipAudioCall call)
                {
                	Log.d("on calling", "i m here");
                	Toast.makeText(c, "onCalling", Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onError(SipAudioCall call, final int errorCode, final String errorMessage)
                {
                	Log.d("on error", "i m here");
                	runOnUiThread(new Runnable() {
                        public void run() {
                        	Toast.makeText(c, "onError "+" "+errorCode+" "+errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
                
                @Override
                public void onCallBusy(SipAudioCall call)
                {
                	Log.d("on call busy", "i m here");
                	runOnUiThread(new Runnable() {
                        public void run() {
                        	Toast.makeText(c, "onCallBusy", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                
                @Override
                public void onCallHeld(SipAudioCall call)
                {
                	Log.d("on call held", "i m here");
                	runOnUiThread(new Runnable() {
                        public void run() {
                        	Toast.makeText(c, "onCallHeld", Toast.LENGTH_LONG).show();
                        }
                    });
                	callHeld=false;
//                	wtActivity.updateStatus("Call held");
                	sendUpdateBroadcast("Call held");
                	super.onCallHeld(call);
                }
                
                @Override
                public void onCallEnded(SipAudioCall call) {

//                    wtActivity.updateStatus("Call ended by other person");
                	if(!missed)
                	{
                		sendUpdateBroadcast("Call ended by other person");
                		Log.d("on call ended", "i m here");
                		runOnUiThread(new Runnable() {
                			public void run() {
                				Toast.makeText(c, "onCallEnded", Toast.LENGTH_LONG).show();
                			}
                		});
                		try
                		{
//                    		if(wtActivity.startTime > 0)
//                    		{
//                    			Toast toast=Toast.makeText(wtActivity, "Entering data in file", Toast.LENGTH_LONG);
//                    			toast.show();
                    	
                    			WalkieTalkieActivity.stopTime=SystemClock.elapsedRealtime();
                    			long milliseconds=WalkieTalkieActivity.stopTime-WalkieTalkieActivity.startTime;
                    			int seconds = (int) (milliseconds / 1000) % 60 ;
                    			int minutes = (int) ((milliseconds / (1000*60)) % 60);
                    			int hours   = (int) ((milliseconds / (1000*60*60)));
                    			StringBuilder sb = new StringBuilder(64);
                    			if(hours>0)
                    			{
                    				sb.append(hours);
                    				sb.append(" hrs ");
                    			}
                    			sb.append(minutes);
                    			sb.append(" mins ");
                    			sb.append(seconds);
                    			sb.append(" secs");
                    			WalkieTalkieActivity.callDuration=sb.toString();
                    			WalkieTalkieActivity.startTime=0;
                    			CallInfo cinfo=new CallInfo(WalkieTalkieActivity.sipAddress.substring(4),WalkieTalkieActivity.callDate,WalkieTalkieActivity.callDuration,WalkieTalkieActivity.outgoingCall,false);
                    		
                    			AudioManager am;
                    			am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    			am.setMode(AudioManager.MODE_NORMAL);
                    			am.setSpeakerphoneOn(false);
                    		
                    			file=new File("/data/data/com.example.android.sip/files/"+WalkieTalkieActivity.FILENAME);
                    			if(!file.exists())
                    			{
                    				fos = openFileOutput(WalkieTalkieActivity.FILENAME, Context.MODE_APPEND);
                    				oos=new ObjectOutputStream(fos);
                    			}
                    			else
                    			{
                    				fos = openFileOutput(WalkieTalkieActivity.FILENAME, Context.MODE_APPEND);
                    				oos=new AppendableObjectOutputStream(fos);
                    			}
                    			oos.writeObject(cinfo);
                    			oos.flush();
                    			fos.close();
                    			oos.close();
                    		
                    			if(mNotificationManager!=null)
                    				mNotificationManager.cancel(WalkieTalkieActivity.HELLO_ID);
                    		
                    			sendSensorBroadcast("unregister");
                    		
//                    			Toast toast2=Toast.makeText(wtActivity, "Data entered successfully", Toast.LENGTH_LONG);
//                            	toast2.show();
//                    		}
                		}
                		catch(Exception e)
                		{
                			Toast toast=Toast.makeText(c, "Unable to enter data to file "+e, Toast.LENGTH_LONG);
                			toast.show();
                		}
                	}
                	missed=false;
                }
            };

//            Looper.myLooper().prepare();
            Toast.makeText(c, "onReceive 1", Toast.LENGTH_LONG).show();
            Log.d("onReceive 1", "i m here");
            incomingCall = WalkieTalkieActivity.manager.takeAudioCall(intent, listener);
            peerAddress=incomingCall.getPeerProfile().getUserName()+"@"+incomingCall.getPeerProfile().getSipDomain();
            TextView tv= (TextView)findViewById(R.id.incomingcall_view);
            tv.append("\n"+incomingCall.getPeerProfile().getUserName()+"@"+incomingCall.getPeerProfile().getSipDomain());

//            wtActivity.mSensorManager.registerListener(wtActivity, wtActivity.mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            
            Toast.makeText(c, "onReceive 2", Toast.LENGTH_LONG).show();
            Log.d("onReceive 2", "i m here");
            
            myHandler.postDelayed(myRunnable, 30000);
            } catch (Exception e) {
        	
        	Toast toast=Toast.makeText(c, "Exception "+e, Toast.LENGTH_LONG);
            toast.show();
            
            if (incomingCall != null) {
                incomingCall.close();
            }
        }
	}
	
	@Override
	public void onBackPressed()
	{
		
	}
	
	public void acceptCall(View v)
	{
//		if(myHandler!=null)
//		{
			myHandler.removeCallbacks(myRunnable);
//		}
		accept=true;
		am.setMode(AudioManager.MODE_IN_COMMUNICATION);
		incomingCall.setListener(listener, true);
//		incomingCall.setListener(listener, true);
//		WalkieTalkieActivity.call = incomingCall;
		Intent i = new Intent(this,WalkieTalkieActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
		this.finish();
//		Toast.makeText(this, "Hi", Toast.LENGTH_LONG).show();
	}
	
	public void rejectCall(View v)
	{
		myHandler.removeCallbacks(myRunnable);
		accept=false;
		am.setMode(AudioManager.MODE_NORMAL);
		incomingCall.setListener(listener, true);
		this.finish();
	}
	
	public void sendUpdateBroadcast(String text)
	{
		Intent broadcastIntent = new Intent();
    	broadcastIntent.setAction(IncomingCallReceiver.ACTION_UPDATE);
    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    	broadcastIntent.putExtra(PARAM_OUT_MSG, text);
    	sendBroadcast(broadcastIntent);
	}
	
	public void sendSensorBroadcast(String text)
	{
		Intent broadcastIntent = new Intent();
    	broadcastIntent.setAction(SensorReceiver.ACTION_SENSOR);
    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    	broadcastIntent.putExtra(PARAM_OUT_MSG2, text);
    	sendBroadcast(broadcastIntent);
	}
	
	public void logMissedCall()
	{
		try
		{
    		int seconds = 0;
    		int minutes = 0;
    		StringBuilder sb = new StringBuilder(64);
    		sb.append(minutes);
    		sb.append(" mins ");
    		sb.append(seconds);
    		sb.append(" secs");
    		WalkieTalkieActivity.callDuration=sb.toString();
    		SimpleDateFormat sdf = new SimpleDateFormat("H:mmaa   EEEE, MMMM d, yyyy");
    		Log.d("creating callinfo", "i m here");
    		CallInfo cinfo=new CallInfo(peerAddress,sdf.format(new Date()),WalkieTalkieActivity.callDuration,WalkieTalkieActivity.outgoingCall,true);
    		Log.d("creating callinfo done", "i m here");
    	
    		AudioManager am;
    		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    		am.setMode(AudioManager.MODE_NORMAL);
    		am.setSpeakerphoneOn(false);
    	
    		file=new File("/data/data/com.example.android.sip/files/"+WalkieTalkieActivity.FILENAME);
    		if(!file.exists())
    		{
    			fos = openFileOutput(WalkieTalkieActivity.FILENAME, Context.MODE_APPEND);
    			oos=new ObjectOutputStream(fos);
    		}
    		else
    		{
    			fos = openFileOutput(WalkieTalkieActivity.FILENAME, Context.MODE_APPEND);
    			oos=new AppendableObjectOutputStream(fos);
    		}
    		oos.writeObject(cinfo);
    		oos.flush();
    		fos.close();
    		oos.close();
		}
		catch(Exception e)
		{
			Toast toast=Toast.makeText(getApplicationContext(), "Unable to enter data to file "+e, Toast.LENGTH_LONG);
			toast.show();
		}
	}
	
	private Runnable myRunnable = new Runnable() {
	    public void run() {
//	    	incomingCall.setListener(listener, true);
	    	try{
	    		sendUpdateBroadcast("Call Missed");
	    		am.setMode(AudioManager.MODE_NORMAL);
	    		missed=true;
	    		incomingCall.endCall();
	    		logMissedCall();
//	    		IncomingCallActivity.this.finish();
	    	}
	    	catch(Exception e)
	    	{}
	        finish();
	    }
	};
}
