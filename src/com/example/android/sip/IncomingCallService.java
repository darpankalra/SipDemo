package com.example.android.sip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipProfile;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("ParserError")
public class IncomingCallService extends IntentService {
	
	FileOutputStream fos;
	ObjectOutputStream oos;
	File file;
	static boolean callHeld=true;
	static boolean flag;
	
	public IncomingCallService()
	{
		super("IncomingCallService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		SipAudioCall incomingCall = null;
		final Context c=getApplicationContext();
        try {

//        	final WalkieTalkieActivity wtActivity = (WalkieTalkieActivity) context;
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
            	
            	@Override
                public void onCallEstablished(final SipAudioCall call) {
            		
//                    Toast.makeText(c, "onCallEstablished", Toast.LENGTH_LONG).show();
            		Log.d("problem in established", "i m here");
            		if(call!=null && call.isInCall() && !callHeld)
            		{
            			callHeld=true;
//            			wtActivity.updateStatus(call);
            		}
            		else if(call!=null && call.isInCall() && callHeld)
            			{
            				callHeld=false;
//            				wtActivity.updateStatus("Call held");
            				return;
            			}
            	}
            	
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    try {
//                        Toast.makeText(c, "onRinging", Toast.LENGTH_LONG).show();
                        Log.d("problem in ringing", "i m here");
//                    	wtActivity.showDialog(12);
                    	call.answerCall(30);
                    } catch (Exception e) {
                    	Log.d("error in ringing", "i m here");
                        e.printStackTrace();
                    }
                }
                
                @Override
                public void onRingingBack(SipAudioCall call)
                {
//                	Toast.makeText(c, "onRingingBack", Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onCalling(SipAudioCall call)
                {
//                	Toast.makeText(c, "onCalling", Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onError(SipAudioCall call, final int errorCode, final String errorMessage)
                {
//                	Toast.makeText(c, "onError "+" "+errorCode+" "+errorMessage, Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onCallBusy(SipAudioCall call)
                {
//                	Toast.makeText(c, "onCallBusy", Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onCallHeld(SipAudioCall call)
                {
                	callHeld=false;
//                	wtActivity.updateStatus("Call held");
                	super.onCallHeld(call);
                }
                
                @Override
                public void onCallEnded(SipAudioCall call) {

//                    wtActivity.updateStatus("Call ended by other person");
                    try
                    {
//                    	if(wtActivity.startTime > 0)
//                    	{
//                    		Toast toast=Toast.makeText(wtActivity, "Entering data in file", Toast.LENGTH_LONG);
//                    		toast.show();
                    	
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
                    		
//                    		if(wtActivity.mNotificationManager!=null)
//                      			wtActivity.mNotificationManager.cancel(WalkieTalkieActivity.HELLO_ID);
                    		
//                    		Toast toast2=Toast.makeText(wtActivity, "Data entered successfully", Toast.LENGTH_LONG);
//                            toast2.show();
//                    	}
                    }
                    catch(Exception e)
                    {
                    	Toast toast=Toast.makeText(c, "Unable to enter data to file "+e, Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            };

//            Looper.myLooper().prepare();
            Toast.makeText(c, "onReceive 1", Toast.LENGTH_LONG).show();
            Log.d("onReceive 1", "i m here");
            (incomingCall = WalkieTalkieActivity.manager.takeAudioCall(intent, null)).setListener(listener, true);
//            listener.onRinging(incomingCall, incomingCall.getPeerProfile());
//            incomingCall.setListener(listener);
//            wtActivity.showDialog(12);
//            incomingCall.answerCall(30);
            incomingCall.startAudio();
            incomingCall.setSpeakerMode(true);
            AudioManager am;
            am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            if(WalkieTalkieActivity.speakerPhone)
            {
            	am.setSpeakerphoneOn(true);
            }
            if(WalkieTalkieActivity.walkieMode && !incomingCall.isMuted()) {
                incomingCall.toggleMute();
            }
            WalkieTalkieActivity.call = incomingCall;
            callHeld=false;
//            wtActivity.call.setListener(listener);
            WalkieTalkieActivity.startTime=SystemClock.elapsedRealtime();
        	SimpleDateFormat sdf = new SimpleDateFormat("H:mmaa   EEEE, MMMM d, yyyy");
        	WalkieTalkieActivity.callDate = sdf.format(new Date());
        	WalkieTalkieActivity.sipAddress="sip:"+incomingCall.getPeerProfile().getUserName() + "@" + incomingCall.getPeerProfile().getSipDomain();
        	WalkieTalkieActivity.outgoingCall=false;

//            wtActivity.updateStatus(incomingCall);
//            Log.d("checking call status", ""+wtActivity.call.isInCall());
//            wtActivity.mSensorManager.registerListener(wtActivity, wtActivity.mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            
//            int icon = R.drawable.icon;
//            long when = System.currentTimeMillis();
//           CharSequence contentTitle = "Sip Demo";
//            CharSequence contentText = "Return to ongoing call";
            
//            Intent notificationIntent = new Intent(wtActivity,WalkieTalkieActivity.class);
//            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            PendingIntent contentIntent = PendingIntent.getActivity(wtActivity, 0, notificationIntent, 0);
            
//            Notification notification = new Notification(icon, "", when);
//            notification.setLatestEventInfo(wtActivity, contentTitle, contentText, contentIntent);
//            notification.flags |= Notification.FLAG_ONGOING_EVENT;
//            notification.flags |= Notification.FLAG_NO_CLEAR;
            
//            wtActivity.mNotificationManager.notify(WalkieTalkieActivity.HELLO_ID, notification);
            
            Toast.makeText(c, "onReceive 2", Toast.LENGTH_LONG).show();
            Log.d("onReceive 2", "i m here");
            } catch (Exception e) {
        	
        	Toast toast=Toast.makeText(c, "Exception "+e, Toast.LENGTH_LONG);
            toast.show();
            
            if (incomingCall != null) {
                incomingCall.close();
            }
        }
	}

}
