/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipProfile;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Listens for incoming SIP calls, intercepts and hands them off to WalkieTalkieActivity.
 */
public class IncomingCallReceiver extends BroadcastReceiver {
    /**
     * Processes the incoming call, answers it, and hands it over to the
     * WalkieTalkieActivity.
     * @param context The context under which the receiver is running.
     * @param intent The intent being received.
     */
	FileOutputStream fos;
	ObjectOutputStream oos;
	File file;
	static boolean callHeld=true;
	static boolean flag;
	SipAudioCall incomingCall = null;
	public static final String ACTION_UPDATE = "com.example.android.sip.UPDATE";
	
    @Override
    public void onReceive(Context context, final Intent intent) {
        
/*        try {

        	final WalkieTalkieActivity wtActivity = (WalkieTalkieActivity) context;
            final SipAudioCall.Listener listener = new SipAudioCall.Listener() {
            	
            	@Override
                public void onCallEstablished(final SipAudioCall call) {
            		
            		wtActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(wtActivity, "onCallEstablished", Toast.LENGTH_LONG).show();
                        }
                    });
            		if(call!=null && call.isInCall() && !callHeld)
            		{
            			callHeld=true;
            			wtActivity.updateStatus(call);
            		}
            		else if(call!=null && call.isInCall() && callHeld)
            			{
            				callHeld=false;
            				wtActivity.updateStatus("Call held");
            				return;
            			}
            	}
            	
                @Override
                public void onRinging(final SipAudioCall call, SipProfile caller) {
                    try {
                    	wtActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(wtActivity, "onRinging", Toast.LENGTH_LONG).show();
                                wtActivity.showDialog(12);
                                try{
                            	call.answerCall(30);}
                                catch(Exception e)
                                {}
                            }
                        });
                    } catch (Exception e) {
                    	Log.d("problem in ringing", ""+e);
                        e.printStackTrace();
                    }
                }
                
                @Override
                public void onRingingBack(SipAudioCall call)
                {
                	wtActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(wtActivity, "onRingingBack", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                
                @Override
                public void onChanged(SipAudioCall call)
                {
                	wtActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(wtActivity, "onChanged", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                
                @Override
                public void onCalling(SipAudioCall call)
                {
                	wtActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(wtActivity, "onCalling", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                
                @Override
                public void onError(SipAudioCall call, final int errorCode, final String errorMessage)
                {
                	wtActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(wtActivity, "onError "+" "+errorCode+" "+errorMessage, Toast.LENGTH_LONG).show();
                            if(errorCode==-7)
                        	{
                        		//show a dialog
                        		wtActivity.showDialog(11);
                        	}
                        }
                    });
                }
                
                @Override
                public void onCallBusy(SipAudioCall call)
                {
                	wtActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(wtActivity, "onCallBusy", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                
                @Override
                public void onCallHeld(SipAudioCall call)
                {
                	callHeld=false;
                	wtActivity.updateStatus("Call held");
                	super.onCallHeld(call);
                }
                
                @Override
                public void onCallEnded(SipAudioCall call) {

                    wtActivity.updateStatus("Call ended by other person");
                    try
                    {
//                    	if(wtActivity.startTime > 0)
//                    	{
//                    		Toast toast=Toast.makeText(wtActivity, "Entering data in file", Toast.LENGTH_LONG);
//                    		toast.show();
                    	
                    		wtActivity.stopTime=SystemClock.elapsedRealtime();
                    		long milliseconds=wtActivity.stopTime-wtActivity.startTime;
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
                			wtActivity.callDuration=sb.toString();
                			wtActivity.startTime=0;
                    		CallInfo cinfo=new CallInfo(wtActivity.sipAddress.substring(4),wtActivity.callDate,wtActivity.callDuration,wtActivity.outgoingCall);
                    		
                    		AudioManager am;
                            am = (AudioManager) wtActivity.getSystemService(Context.AUDIO_SERVICE);
                            am.setMode(AudioManager.MODE_NORMAL);
                            am.setSpeakerphoneOn(false);
                    		
                    		file=new File("/data/data/com.example.android.sip/files/"+wtActivity.FILENAME);
                            if(!file.exists())
                            {
                            	fos = wtActivity.openFileOutput(wtActivity.FILENAME, Context.MODE_APPEND);
                            	oos=new ObjectOutputStream(fos);
                            }
                            else
                            {
                            	fos = wtActivity.openFileOutput(wtActivity.FILENAME, Context.MODE_APPEND);
                            	oos=new AppendableObjectOutputStream(fos);
                            }
                    		oos.writeObject(cinfo);
                    		oos.flush();
                    		fos.close();
                    		oos.close();
                    		
                    		if(wtActivity.mNotificationManager!=null)
                      			wtActivity.mNotificationManager.cancel(wtActivity.HELLO_ID);
                    		
//                    		Toast toast2=Toast.makeText(wtActivity, "Data entered successfully", Toast.LENGTH_LONG);
//                            toast2.show();
//                    	}
                    }
                    catch(Exception e)
                    {
                    	Toast toast=Toast.makeText(wtActivity, "Unable to enter data to file "+e, Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            };

//            Looper.myLooper().prepare();
            Toast.makeText(wtActivity, "onReceive 1", Toast.LENGTH_LONG).show();
            (incomingCall = wtActivity.manager.takeAudioCall(intent, listener)).setListener(listener, true);					
//            listener.onRinging(incomingCall, incomingCall.getPeerProfile());
//            incomingCall.setListener(listener);
//            wtActivity.showDialog(12);
//            incomingCall.answerCall(30);
            incomingCall.startAudio();
            incomingCall.setSpeakerMode(true);
            AudioManager am;
            am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            if(wtActivity.speakerPhone)
            {
            	am.setSpeakerphoneOn(true);
            }
            if(WalkieTalkieActivity.walkieMode && !incomingCall.isMuted()) {
                incomingCall.toggleMute();
            }
            wtActivity.call = incomingCall;
            callHeld=false;
//            wtActivity.call.setListener(listener);
            wtActivity.startTime=SystemClock.elapsedRealtime();
        	SimpleDateFormat sdf = new SimpleDateFormat("H:mmaa   EEEE, MMMM d, yyyy");
        	wtActivity.callDate = sdf.format(new Date());
        	wtActivity.sipAddress="sip:"+incomingCall.getPeerProfile().getUserName() + "@" + incomingCall.getPeerProfile().getSipDomain();
        	wtActivity.outgoingCall=false;

            wtActivity.updateStatus(incomingCall);
//            Log.d("checking call status", ""+wtActivity.call.isInCall());
            wtActivity.mSensorManager.registerListener(wtActivity, wtActivity.mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            
            int icon = R.drawable.icon;
            long when = System.currentTimeMillis();
            CharSequence contentTitle = "Sip Demo";
            CharSequence contentText = "Return to ongoing call";
            
            Intent notificationIntent = new Intent(wtActivity,WalkieTalkieActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(wtActivity, 0, notificationIntent, 0);
            
            Notification notification = new Notification(icon, "", when);
            notification.setLatestEventInfo(wtActivity, contentTitle, contentText, contentIntent);
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.flags |= Notification.FLAG_NO_CLEAR;
            
            wtActivity.mNotificationManager.notify(wtActivity.HELLO_ID, notification);
            
            Toast.makeText(wtActivity, "onReceive 2", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
        	
        	Toast toast=Toast.makeText(context, "Exception "+e, Toast.LENGTH_LONG);
            toast.show();
            
            if (incomingCall != null) {
                incomingCall.close();
            }
        }*/
//    	Intent intent2=new Intent(context,IncomingCallService.class);
/*    	Toast.makeText(context, "onReceive", Toast.LENGTH_LONG).show();
    	intent.setClass(context, IncomingCallActivity.class);
    	context.startActivity(intent);*/
//    	Log.d("receiving intent 1", "i m here");
    	WalkieTalkieActivity wtActivity = (WalkieTalkieActivity) context;
    	String text = intent.getStringExtra(IncomingCallActivity.PARAM_OUT_MSG);
//    	Log.d("receiving intent 2", "i m here");
    	wtActivity.updateStatus(text);
//    	Log.d("receiving intent 3", "i m here");
    }

}
