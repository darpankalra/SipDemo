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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.JetPlayer;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
/*import org.openintents.sensorsimulator.hardware.Sensor;
import org.openintents.sensorsimulator.hardware.SensorEvent;
import org.openintents.sensorsimulator.hardware.SensorEventListener;
import org.openintents.sensorsimulator.hardware.SensorManagerSimulator;*/

/**
 * Handles all calling, receiving calls, and UI interaction in the WalkieTalkie app.
 */
@SuppressLint("ParserError")
public class WalkieTalkieActivity extends Activity implements View.OnTouchListener, SensorEventListener {

    public static String sipAddress = null;
    static String callDate = "";
    static String callDuration = "";
    static long startTime=0;
    static long stopTime=0;
    static boolean outgoingCall;
    
    static String FILENAME = "Hist_file";
    File file;

    public static SipManager manager = null;
    public SipProfile me = null;
    public static SipAudioCall call = null;
    public IncomingCallReceiver callReceiver;
    public SensorReceiver sensorReceiver;

    private static final int CALL_ADDRESS = 1;
    private static final int SET_AUTH_INFO = 2;
    private static final int UPDATE_SETTINGS_DIALOG = 3;
    private static final int HANG_UP = 4;
    private static final int SHOW_HISTORY = 5;
    private static final int TOGGLE_LOUDSPEAKER = 6;
    private static final int CALL_CONNECTED_DIALOG=7;
    private static final int TOGGLE_WALKIEMODE = 8;
    private static final int HOLD_CALL = 9;
    private static final int RESUME_CALL = 10;
    private static final int INCORRECT_ADDRESS = 11;
    static final int INCOMING_CALL = 12;
    
    static final int HELLO_ID = 1;
    PowerManager.WakeLock mProximityWakeLock;
    PowerManager pm;
    KeyguardManager keyguardManager;
    
//    static TextView labelView;
    ToggleButton pushToTalkButton;
    
    FileOutputStream fos;
    ObjectOutputStream oos;
    
    static boolean backFromPreferences=false;
    static boolean callSomeone=false;
    static boolean endCall=false;
    static boolean speakerPhone=true;
    static boolean walkieMode=true;
    static boolean callHeld=false;
    static boolean incomingCallHeld;
    
    public final static String CALLSTATUS="com.example.android.sip.CALLCONNECTED";
    
    AudioManager am;
    NotificationManager mNotificationManager;
    SensorManager mSensorManager;
//    SensorManagerSimulator mSensorManager;
    Sensor mSensor;
    SipAudioCall.Listener listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.walkietalkie);
        
        walkieMode=true;
        speakerPhone=true;
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setSpeakerphoneOn(true);

        pushToTalkButton = (ToggleButton) findViewById(R.id.pushToTalk);
        pushToTalkButton.setOnTouchListener(this);
        pushToTalkButton.setBackgroundResource(R.drawable.btn_record);
        
        Toast.makeText(this, "onCreate "+walkieMode, Toast.LENGTH_LONG).show();

        // Set up the intent filter.  This will be used to fire an
        // IncomingCallReceiver when someone calls the SIP address used by this
        // application.
        IntentFilter filter = new IntentFilter(IncomingCallReceiver.ACTION_UPDATE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);
        
        IntentFilter filter2 = new IntentFilter(SensorReceiver.ACTION_SENSOR);
        filter2.addCategory(Intent.CATEGORY_DEFAULT);
        sensorReceiver = new SensorReceiver();
        this.registerReceiver(sensorReceiver, filter2);

        // "Push to talk" can be a serious pain when the screen keeps turning off.
        // Let's prevent that.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
/*        if(endCall)
        {
        	endCall=false;
        	endCurrentCall();
        }*/

        String ns=NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) getSystemService(ns);
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
//        mSensorManager = SensorManagerSimulator.getSystemService(this, SENSOR_SERVICE);
//        mSensorManager.connectSimulator();
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        try {
            Method method = pm.getClass().getDeclaredMethod("getSupportedWakeLockFlags");
            int supportedFlags = (Integer) method.invoke(pm);
            Field f = PowerManager.class.getDeclaredField("PROXIMITY_SCREEN_OFF_WAKE_LOCK");
            int proximityScreenOffWakeLock = (Integer) f.get(null);
            if( (supportedFlags & proximityScreenOffWakeLock) != 0x0 ) {
                    mProximityWakeLock = pm.newWakeLock(proximityScreenOffWakeLock, "Tag");
                    mProximityWakeLock.setReferenceCounted(false);
            }
            
        } 
        catch (Exception e) {
            Log.d("Impossible to get power manager supported wake lock flags", "i m here");
        }
        keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        initializeManager();
//        Toast.makeText(this, "No Nava intent", Toast.LENGTH_LONG).show();
        
/*        if(callSomeone)
        {
        	callSomeone=false;
        	Intent intent=getIntent();
//        	Toast.makeText(this, ""+intent.getStringExtra(History.ADDRTOCALL), Toast.LENGTH_LONG).show();
        	sipAddress=intent.getStringExtra(History.ADDRTOCALL);
//        	Toast.makeText(this, ""+sipAddress, Toast.LENGTH_LONG).show();
//        	Log.d("yaar", "call is intiating");
        	initiateCall();
        }*/
/*        try
        {
        	int i=0;
        	for(;i<=2;i++)
        	{
        	file=new File("/data/data/com.example.android.sip/files/"+FILENAME);
            if(!file.exists())
            {
            	fos = openFileOutput(FILENAME, Context.MODE_APPEND);
            	oos=new ObjectOutputStream(fos);
            }
            else
            {
            	fos = openFileOutput(FILENAME, Context.MODE_APPEND);
            	oos=new AppendableObjectOutputStream(fos);
            }
        	CallInfo cinfo=new CallInfo("sip:darpan@iptel.org", "2:45am  Saturday, June 30, 2012", "3 mins 57 secs");
        	oos.writeObject(cinfo);
        	SimpleDateFormat sdf = new SimpleDateFormat("H:mmaa   EEEE, MMMM d, yyyy");
        	String currentDateandTime = sdf.format(new Date());
        	oos.writeObject(new CallInfo("sip:darpan@iptel.org", currentDateandTime, "3 mins 57 secs"));
        	oos.flush();
        	fos.close();
        	oos.close();
        	}
        }
        catch(Exception e)
        {
        	Toast toast=Toast.makeText(getApplicationContext(), "Unable to enter data to file "+e, Toast.LENGTH_LONG);
            toast.show();
        }*/
        
        
        
    }
    
 //   @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy)
    {
    	
    }
    
//    @Override
    public final void onSensorChanged(SensorEvent event)
    {
    	if(event.sensor.getType()==Sensor.TYPE_PROXIMITY)
    	{
    		float distance=event.values[0];
//    		Toast.makeText(this, "Sensor detected", Toast.LENGTH_LONG).show();
    		if(!walkieMode && !speakerPhone)
    		{
    			if(distance >= 0.0 && distance <= (mSensor.getMaximumRange()/2))
    			{
//    				Toast.makeText(this, "Sensor is working", Toast.LENGTH_SHORT).show();
/*    				WindowManager.LayoutParams params = getWindow().getAttributes();
    				params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
    				params.screenBrightness = 0.01f;
    				getWindow().setAttributes(params);*/
//    				PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
//    				pm.goToSleep(SystemClock.uptimeMillis());
//    				if(wakeLock!=null)
//    					wakeLock.release();
//    	            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
//    	            wakeLock.acquire();
//    	            WindowManager.LayoutParams params = getWindow().getAttributes();
//    	            params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
//    				params.screenBrightness = 0.0f;
//    				getWindow().setAttributes(params);
/*    	            KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE); 
    	            KeyguardLock keyguardLock =  keyguardManager.newKeyguardLock("TAG");
    	            keyguardLock.reenableKeyguard();*/
/*    				int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
    		    	mProximityWakeLock = pm.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, "yo");
    		    	if(!mProximityWakeLock.isHeld()){
    		    	    mProximityWakeLock.acquire();
    		    	}*/
    				if(mProximityWakeLock!=null && !mProximityWakeLock.isHeld())
    					mProximityWakeLock.acquire();
    				 
    			}
    			else
    			{
//    				Toast.makeText(this, "Screen is on", Toast.LENGTH_SHORT).show();
//    				if(wakeLock!=null)
//    					wakeLock.release();
//    				PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
//    	            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "TAG");
/*    				wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
    	            wakeLock.acquire();
    	            WindowManager.LayoutParams params = getWindow().getAttributes();
    				params.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD; //| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
    				params.screenBrightness = 1f;
    				getWindow().setAttributes(params);
    	            KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE); 
    	            KeyguardLock keyguardLock =  keyguardManager.newKeyguardLock("TAG");
    	            keyguardLock.disableKeyguard();*/
    				if(mProximityWakeLock!=null && mProximityWakeLock.isHeld())
    				{
//    					mProximityWakeLock.release();
    					try{
    					int i=1;
    					Method method = pm.getClass().getDeclaredMethod("release");
                        method.invoke(pm,i);}
    					catch(Exception e)
    					{
    						Toast.makeText(getApplicationContext(), " "+e, Toast.LENGTH_LONG).show();
    					}
    				}
//    				if(mProximityWakeLock!=null && mProximityWakeLock.isHeld())
//    				{
//    					mProximityWakeLock.release();
//    				}
/*    				try{
    				PowerManager.class.getDeclaredField("PROXIMITY_SCREEN_OFF_WAKE_LOCK");}
    				catch(Exception e)
    				{}*/
/*    				((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG").acquire();*/
    				if(keyguardManager!=null)
    				{
    					KeyguardLock keyguardLock =  keyguardManager.newKeyguardLock("tag");
    					keyguardLock.disableKeyguard();
    				}
/*    				KeyguardManager.KeyguardLock newKeyguardLock = (KeyguardManager)getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
    				newKeyguardLock = km.newKeyguardLock(HANDSFREE);
    				newKeyguardLock.disableKeyguard();*/
    			}
    		}
    	}
    }

    @Override
    public void onStart() {
        super.onStart();
        // When we get back from the preference setting Activity, assume
        // settings have changed, and re-login with new auth info.
        if(backFromPreferences)
        {
        	backFromPreferences=false;
        	initializeManager();
        }
        mSensorManager.registerListener(WalkieTalkieActivity.this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	if(mNotificationManager!=null && call==null)
    		mNotificationManager.cancel(HELLO_ID);
//    	if(mSensorManager!=null && call==null)
//    		mSensorManager.unregisterListener(this);
    	
    }
    
    @Override
    protected void onNewIntent(Intent intent)
    {
    	super.onNewIntent(intent);
    	Toast.makeText(this, "Nava intent", Toast.LENGTH_LONG).show();
    	
    	if(endCall)
        {
        	endCall=false;
//        	Toast.makeText(this, "i m here "+call, Toast.LENGTH_LONG).show();
        	endCurrentCall();
        }
    	
//    	Toast.makeText(this, "wallah", Toast.LENGTH_LONG).show();
        
        if(callSomeone)
        {
        	callSomeone=false;
//        	Toast.makeText(this, "wallah 2", Toast.LENGTH_LONG).show();
//        	Intent intent2=getIntent();
//        	Toast.makeText(this, ""+intent.getStringExtra(History.ADDRTOCALL), Toast.LENGTH_LONG).show();
        	sipAddress=intent.getStringExtra(History.ADDRTOCALL);
//        	Toast.makeText(this, ""+sipAddress, Toast.LENGTH_LONG).show();
//        	Log.d("yaar", "call is intiating");
//        	Toast.makeText(this, "wallah 3", Toast.LENGTH_LONG).show();
        	initiateCall();
        }
    	
    }
    
    @Override
    public void onStop()
    {
    	super.onStop();
    	if(mProximityWakeLock!=null && mProximityWakeLock.isHeld())
        	mProximityWakeLock.release();
    	if(mSensorManager!=null)
        	mSensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        Toast.makeText(this, "onDestroy", Toast.LENGTH_LONG).show();
        
        if (call!=null && call.isInCall())
    	{
    		endCurrentCall();
    	}
        if (call != null) {
            call.close();
        }

        closeLocalProfile();

        if (callReceiver != null) {
            this.unregisterReceiver(callReceiver);
        }
        if (sensorReceiver != null) {
            this.unregisterReceiver(sensorReceiver);
        }
        
        Context c=getApplicationContext();
        am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_NORMAL);
        am.setSpeakerphoneOn(false);
        
        if(mProximityWakeLock!=null && mProximityWakeLock.isHeld())
        	mProximityWakeLock.release();
        if(mSensorManager!=null)
        	mSensorManager.unregisterListener(this);
        if(mNotificationManager!=null)
        	mNotificationManager.cancel(HELLO_ID);
        if(keyguardManager!=null)
        {
        	KeyguardLock keyguardLock =  keyguardManager.newKeyguardLock("tag");
        	keyguardLock.reenableKeyguard();
        }
    }
    
    @Override
    public void onBackPressed()
    {
    	if (call!=null && call.isInCall())
    	{
    		showDialog(CALL_CONNECTED_DIALOG);
    	}
//    	super.onBackPressed();
    	else
    	{
    		super.onBackPressed();
    	}
    }

    public void initializeManager() {
        if(manager == null) {
          manager = SipManager.newInstance(this);
        }

        initializeLocalProfile();
    }

    /**
     * Logs you into your SIP provider, registering this device as the location to
     * send SIP calls to for your SIP address.
     */
    public void initializeLocalProfile() {
        if (manager == null) {
            return;
        }

        if (me != null) {
            closeLocalProfile();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String username = prefs.getString("namePref", "");
        String domain = prefs.getString("domainPref", "");
        String password = prefs.getString("passPref", "");

        if (username.length() == 0 || domain.length() == 0 || password.length() == 0) {
            showDialog(UPDATE_SETTINGS_DIALOG);
            return;
        }

        try {
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            me = builder.build();

/*            Intent i = new Intent();
            i.setAction("android.SipDemo.INCOMING_CALL");
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
            manager.open(me, pi, null);*/
            
            Intent i = new Intent();
            i.setAction("android.SipDemo.INCOMING_CALL");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, Intent.FILL_IN_DATA);
            manager.open(me, pi, null);


            // This listener must be added AFTER manager.open is called,
            // Otherwise the methods aren't guaranteed to fire.

            manager.setRegistrationListener(me.getUriString(), new SipRegistrationListener() {
                    public void onRegistering(String localProfileUri) {
                        updateStatus("Registering with SIP Server...");
                    }

                    public void onRegistrationDone(String localProfileUri, long expiryTime) {
                        updateStatus("Ready");
                    }

                    public void onRegistrationFailed(String localProfileUri, int errorCode,
                            String errorMessage) {
                        updateStatus("Registration failed.  \nPlease check settings.");
                    }
                });
        } catch (ParseException pe) {
            updateStatus("Connection Error.");
        } catch (SipException se) {
            updateStatus("Connection error.");
        }
    }

    /**
     * Closes out your local profile, freeing associated objects into memory
     * and unregistering your device from the server.
     */
    public void closeLocalProfile() {
        if (manager == null) {
            return;
        }
        try {
            if (me != null) {
                manager.close(me.getUriString());
            }
        } catch (Exception ee) {
            Log.d("WalkieTalkieActivity/onDestroy", "Failed to close local profile.", ee);
        }
    }

    /**
     * Make an outgoing call.
     */
    public void initiateCall() {

//        updateStatus(sipAddress);

        try {
            listener = new SipAudioCall.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.
                @Override
                public void onCallEstablished(final SipAudioCall call) {
                	

                	
                	if(call!=null && call.isInCall())
                	{
                		if(!callHeld)
                		{
                			callHeld=true;
                			updateStatus("Call held");
                			return;
                		}
                		else
                		{
                			callHeld=false;
                			updateStatus(call);
                			return;
                		}
                	}
                	
                	startTime=SystemClock.elapsedRealtime();
                	SimpleDateFormat sdf = new SimpleDateFormat("H:mmaa   EEEE, MMMM d, yyyy");
                	callDate = sdf.format(new Date());
                	outgoingCall=true;
                	callHeld=false;
                    call.startAudio();
                    call.setSpeakerMode(true);
                    final Context c=getApplicationContext();
                    if(speakerPhone)
                    {
                    	am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
                    	am.setSpeakerphoneOn(true);
                    }
                    am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    if(walkieMode)
                    {
                    	call.toggleMute();
                    }
                    updateStatus(call);
                    
                    mSensorManager.registerListener(WalkieTalkieActivity.this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    
                    int icon = R.drawable.icon;
                    long when = System.currentTimeMillis();
                    CharSequence contentTitle = "Sip Demo";
                    CharSequence contentText = "Return to ongoing call";
                    
                    Intent notificationIntent = new Intent(c,WalkieTalkieActivity.class);
                    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    PendingIntent contentIntent = PendingIntent.getActivity(c, 0, notificationIntent, 0);
                    
                    Notification notification = new Notification(icon, "", when);
                    notification.setLatestEventInfo(c, contentTitle, contentText, contentIntent);
                    notification.flags |= Notification.FLAG_ONGOING_EVENT;
                    notification.flags |= Notification.FLAG_NO_CLEAR;
                    
                    mNotificationManager.notify(HELLO_ID, notification);

                }
                
                @Override
                public void onCalling(SipAudioCall call)
                {
                	runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(WalkieTalkieActivity.this, "onCalling", Toast.LENGTH_LONG).show();
                        }
                    });
                	updateStatus("Calling..");
                }
                
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller)
                {
                	runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(WalkieTalkieActivity.this, "onRinging", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                
                @Override
                public void onCallHeld(SipAudioCall call)
                {
                	callHeld=true;
                	updateStatus("Call held");
                	super.onCallHeld(call);
                }

                @Override
                public void onCallEnded(SipAudioCall call) {

//                	Toast toast3=Toast.makeText(getApplicationContext(), "Before update", Toast.LENGTH_LONG);
//            		toast3.show();
                    updateStatus("Call ended by other person");
//                    Toast toast4=Toast.makeText(getApplicationContext(), "After update", Toast.LENGTH_LONG);
//            		toast4.show();
                    try
                    {
//                    	if(startTime > 0)
//                    	{
//                    		Toast toast=Toast.makeText(getApplicationContext(), "Entering data in file", Toast.LENGTH_LONG);
//                    		toast.show();
                    	
                    		stopTime=SystemClock.elapsedRealtime();
                    		long milliseconds=stopTime-startTime;
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
                			callDuration=sb.toString();
                			startTime=0;
                    		CallInfo cinfo=new CallInfo(sipAddress.substring(4),callDate,callDuration,outgoingCall,false);
                    		
                            Context c=getApplicationContext();
                            am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
                            am.setMode(AudioManager.MODE_NORMAL);
                            am.setSpeakerphoneOn(false);
                            
                            Log.d("yo-yo", "i m here");
                    		
                    		file=new File("/data/data/com.example.android.sip/files/"+FILENAME);
                            if(!file.exists())
                            {
                            	fos = openFileOutput(FILENAME, Context.MODE_APPEND);
                            	oos=new ObjectOutputStream(fos);
                            }
                            else
                            {
                            	fos = openFileOutput(FILENAME, Context.MODE_APPEND);
                            	oos=new AppendableObjectOutputStream(fos);
                            }
                            
                            Log.d("yo-yo", "i m here 2");
                            
                    		oos.writeObject(cinfo);
                    		oos.flush();
                        	fos.close();
                        	oos.close();
                        	
                        	mNotificationManager.cancel(HELLO_ID);
                        	
                        	mSensorManager.unregisterListener(WalkieTalkieActivity.this);
                    		
//                    		Toast toast2=Toast.makeText(getApplicationContext(), "Data entered successfully", Toast.LENGTH_LONG);
//                            toast2.show();
//                    	}
                    }
                    catch(Exception e)
                    {
//                    	Toast toast=Toast.makeText(getApplicationContext(), "Unable to enter data to file "+e, Toast.LENGTH_LONG);
//                        toast.show();
                    	Log.d("yo-yo","Unable to enter "+e);
                    }
                }
                
                @Override
                public void onError(SipAudioCall call, final int errorCode, final String errorMessage)
                {
                	runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "onError "+" "+errorCode+" "+errorMessage, Toast.LENGTH_LONG).show();
                            if(errorCode==-7)
                        	{
                        		//show a dialog
                        		WalkieTalkieActivity.this.showDialog(INCORRECT_ADDRESS);
                        	}
                        }
                    });
                	Log.d("yo-yo Error", ""+errorCode+"  "+errorMessage);
                }
                
                @Override
                public void onCallBusy(SipAudioCall call)
                {
                	runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "onCallBusy", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            };

            		call = manager.makeAudioCall(me.getUriString(), sipAddress, listener, 30);
            		call.setListener(listener, true);
//                	call.setListener(listener, true);

        }
        catch (Exception e) {
            Log.i("WalkieTalkieActivity/InitiateCall", "Error when trying to close manager."+e, e);
            if (me != null) {
                try {
                    manager.close(me.getUriString());
                } catch (Exception ee) {
                    Log.i("WalkieTalkieActivity/InitiateCall",
                            "Error when trying to close manager.", ee);
                    ee.printStackTrace();
                }
            }
            if (call != null) {
                call.close();
            }
        }
    }

    /**
     * Updates the status box at the top of the UI with a messege of your choice.
     * @param status The String to display in the status box.
     */
    public void updateStatus(final String status) {
        // Be a good citizen.  Make sure UI changes fire on the UI thread.
        this.runOnUiThread(new Runnable() {
            public void run() {
                TextView labelView = (TextView) findViewById(R.id.sipLabel);
                labelView.setText(status);
            }
        });
    }

    /**
     * Updates the status box with the SIP address of the current call.
     * @param call The current, active call.
     */
    public void updateStatus(SipAudioCall call) {
        String useName = call.getPeerProfile().getDisplayName();
        if(useName == null) {
          useName = call.getPeerProfile().getUserName();
        }
        updateStatus(useName + "@" + call.getPeerProfile().getSipDomain());
    }

    /**
     * Updates whether or not the user's voice is muted, depending on whether the button is pressed.
     * @param v The View where the touch event is being fired.
     * @param event The motion to act on.
     * @return boolean Returns false to indicate that the parent view should handle the touch event
     * as it normally would.
     */
    public boolean onTouch(View v, MotionEvent event) {
        if (call == null) {
            return false;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN && call != null && call.isMuted()) {
            call.toggleMute();
        } else if (event.getAction() == MotionEvent.ACTION_UP && !call.isMuted()) {
            call.toggleMute();
        }
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, CALL_ADDRESS, 0, "Call").setIcon(R.drawable.phone);
        menu.add(0, SET_AUTH_INFO, 2, "Settings").setIcon(R.drawable.settings);
//        menu.add(0, HANG_UP, 4, "End Current Call.");
        menu.add(0, HOLD_CALL, 4, "Hold").setIcon(R.drawable.pause);
        menu.add(0, SHOW_HISTORY, 6, "Call Log").setIcon(R.drawable.calllog);
        if(speakerPhone)
        {
        	menu.add(0, TOGGLE_LOUDSPEAKER, 8, "Speaker").setIcon(R.drawable.speaker);
        }
        else
        {
        	menu.add(0, TOGGLE_LOUDSPEAKER, 8, "Speaker").setIcon(R.drawable.speakerx);
        }
        if(walkieMode)
        {
        	menu.add(0, TOGGLE_WALKIEMODE, 10, "Walkie-Talkie").setIcon(R.drawable.walkie);
        }
        else
        {
        	menu.add(0, TOGGLE_WALKIEMODE, 10, "Walkie-Talkie").setIcon(R.drawable.walkiex);
        }

        return true;
    }
    
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	super.onPrepareOptionsMenu(menu);
    	MenuItem hold=menu.findItem(HOLD_CALL);
    	MenuItem resume=menu.findItem(RESUME_CALL);
    	if(call!=null && call.isInCall())
    	{
    		if(menu.findItem(CALL_ADDRESS)!=null && menu.findItem(HANG_UP)==null)
    		{
    			menu.removeItem(CALL_ADDRESS);
    			menu.add(0, HANG_UP, 0, "End Call").setIcon(R.drawable.phonex);
    		}
    		if(call.isOnHold() && hold!=null)
    		{
    			menu.removeItem(HOLD_CALL);
    			menu.add(0, RESUME_CALL, 4, "Resume").setIcon(R.drawable.play);
    		}
    		if(!call.isOnHold() && resume!=null)
    		{
    			menu.removeItem(RESUME_CALL);
    			menu.add(0, HOLD_CALL, 4, "Hold").setIcon(R.drawable.pause);
    		}
/*    		if(callHeld && menu.findItem(HOLD_CALL)!=null)
    		{
    			menu.findItem(HOLD_CALL).setEnabled(false);
    		}
    		if(!callHeld && menu.findItem(HOLD_CALL)!=null &&!menu.findItem(HOLD_CALL).isEnabled())
    		{
    			menu.findItem(HOLD_CALL).setEnabled(true);
    		}
    		if(!IncomingCallReceiver.callHeld && menu.findItem(HOLD_CALL)!=null)
    		{
    			menu.findItem(HOLD_CALL).setEnabled(false);
    		}
    		if(IncomingCallReceiver.callHeld && menu.findItem(HOLD_CALL)!=null &&!menu.findItem(HOLD_CALL).isEnabled())
    		{
    			menu.findItem(HOLD_CALL).setEnabled(true);
    		}
    		if(!call.isOnHold() && menu.findItem(RESUME_CALL)!=null)
    		{
    			menu.removeItem(RESUME_CALL);
    			menu.add(0, HOLD_CALL, 4, "Hold Call");
    		}*/
    		if(outgoingCall)
    		{
    			if(callHeld && hold!=null)
        		{
        			hold.setEnabled(false);
        		}
        		if(!callHeld && hold!=null &&!hold.isEnabled())
        		{
        			hold.setEnabled(true);
        		}
    		}
    		else
    		{
    			if(!IncomingCallActivity.callHeld && hold!=null)
        		{
        			hold.setEnabled(false);
        		}
        		if(IncomingCallActivity.callHeld && hold!=null &&!hold.isEnabled())
        		{
        			hold.setEnabled(true);
        		}
    		}
    		
    	}
    	else
    	{
    		if(menu.findItem(CALL_ADDRESS)==null && menu.findItem(HANG_UP)!=null)
    		{
    			menu.removeItem(HANG_UP);
    			menu.add(0, CALL_ADDRESS, 0, "Call").setIcon(R.drawable.phone);
    		}
    		if(hold!=null && !hold.isEnabled())
    		{
    			hold.setEnabled(true);
    		}
    		if(resume!=null)
    		{
    			menu.removeItem(RESUME_CALL);
    			menu.add(0, HOLD_CALL, 4, "Hold");
    		}
    	}
    	if(speakerPhone)
    	{
    		menu.findItem(TOGGLE_LOUDSPEAKER).setIcon(R.drawable.speaker);
    	}
    	else
    	{
    		menu.findItem(TOGGLE_LOUDSPEAKER).setIcon(R.drawable.speakerx);
    	}
    	if(walkieMode)
    	{
    		menu.findItem(TOGGLE_WALKIEMODE).setIcon(R.drawable.walkie);
    	}
    	else
    	{
    		menu.findItem(TOGGLE_WALKIEMODE).setIcon(R.drawable.walkiex);
    	}
    	return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CALL_ADDRESS:
                showDialog(CALL_ADDRESS);
                break;
            case SET_AUTH_INFO:
                updatePreferences();
                break;
            case HANG_UP:
                endCurrentCall();
                break;
            case SHOW_HISTORY:
            	Intent intent=new Intent(this, History.class);
            	if(call!=null)
            		intent.putExtra(CALLSTATUS, call.isInCall());
            	startActivity(intent);
            	break;
            case TOGGLE_LOUDSPEAKER:
            	if(am==null)
            	{
            		Context c=getApplicationContext();
            		am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            	}
                if(am.isSpeakerphoneOn())
                {
                	am.setSpeakerphoneOn(false);
                	speakerPhone=false;
                	Toast.makeText(this, "Loudspeaker off", Toast.LENGTH_LONG).show();
                }
                else
                {
                	am.setSpeakerphoneOn(true);
                	speakerPhone=true;
                	Toast.makeText(this, "Loudspeaker on", Toast.LENGTH_LONG).show();
                }
            	break;
            case TOGGLE_WALKIEMODE:
            	if(call!=null)
            	{
            		if(walkieMode)
            		{
            			walkieMode=false;
//            			pushToTalkButton.setPressed(true);
            			pushToTalkButton.setBackgroundResource(R.drawable.mic_on);
            			pushToTalkButton.setClickable(false);
            			if(call.isMuted())
            			{
            				call.toggleMute();
            			}
            			Toast.makeText(this, "Walkie mode off", Toast.LENGTH_LONG).show();
            		}
            		else
            		{
            			walkieMode=true;
//            			pushToTalkButton.setPressed(false);
            			pushToTalkButton.setBackgroundResource(R.drawable.btn_record);
            			pushToTalkButton.setClickable(true);
            			if(!call.isMuted())
            			{
            				call.toggleMute();
            			}
            			Toast.makeText(this, "Walkie mode on", Toast.LENGTH_LONG).show();
            		}
            	}
            	else if(call==null)
            	{
            		if(walkieMode)
            		{
            			walkieMode=false;
            			pushToTalkButton.setBackgroundResource(R.drawable.mic_on);
//                		pushToTalkButton.setPressed(true);
                		pushToTalkButton.setClickable(false);
                		Toast.makeText(this, "Walkie mode off", Toast.LENGTH_LONG).show();
            		}
            		else
            		{
            			walkieMode=true;
//                		pushToTalkButton.setPressed(false);
            			pushToTalkButton.setBackgroundResource(R.drawable.btn_record);
                		pushToTalkButton.setClickable(true);
                		Toast.makeText(this, "Walkie mode on", Toast.LENGTH_LONG).show();
            		}
            	}
            	break;
            case HOLD_CALL:
            	try
            	{
            		if(call!=null && call.isInCall())
            		{
            			if(!call.isOnHold() && !callHeld && IncomingCallActivity.callHeld)
            			{
            				Toast.makeText(this, ""+call.isOnHold(), Toast.LENGTH_LONG).show();
            				call.holdCall(30);
            				Toast.makeText(this, ""+call.isOnHold(), Toast.LENGTH_LONG).show();
            			}
            		}
            	}
            	catch(Exception e)
            	{
            		Toast.makeText(this, ""+e, Toast.LENGTH_LONG).show();
            	}
            	break;
            case RESUME_CALL:
            	try
            	{
            		call.continueCall(30);
//            		Toast.makeText(this, ""+call.isOnHold(), Toast.LENGTH_LONG).show();
            	}
            	catch(Exception e)
            	{
            		Toast.makeText(this, ""+e, Toast.LENGTH_LONG).show();
            	}
            	break;
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CALL_ADDRESS:

                LayoutInflater factory = LayoutInflater.from(this);
                final View textBoxView = factory.inflate(R.layout.call_address_dialog, null);
                TextView tv=(TextView)textBoxView.findViewById(R.id.calladdress_edit);
                tv.setText("sip:");
                return new AlertDialog.Builder(this)
                        .setTitle("Call Someone.")
                        .setView(textBoxView)
                        .setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        EditText textField = (EditText)
                                                (textBoxView.findViewById(R.id.calladdress_edit));
                                        sipAddress = textField.getText().toString();
                                        initiateCall();

                                    }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();

            case UPDATE_SETTINGS_DIALOG:
                return new AlertDialog.Builder(this)
                        .setMessage("Please update your SIP Account Settings.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                updatePreferences();
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();
            case CALL_CONNECTED_DIALOG:
    			return new AlertDialog.Builder(this)
    				.setMessage("You are currently in call.\nIf you select this option your current call will be disconnected")
    				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
    					
//    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    						// TODO Auto-generated method stub
    						endCurrentCall();
    						WalkieTalkieActivity.this.finish();
    					}
    				})
    				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
    					
//    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    						// TODO Auto-generated method stub
    						
    					}
    				})
    				.setCancelable(false)
    				.create();
            case INCORRECT_ADDRESS:
            	LayoutInflater factory2 = LayoutInflater.from(this);
                final View textBoxView2 = factory2.inflate(R.layout.call_address_dialog, null);
                TextView tv2=(TextView)textBoxView2.findViewById(R.id.calladdress_view);
                tv2.setText("Incorrect sip address\nRe-enter sip address");
                return new AlertDialog.Builder(this)
                        .setTitle("Incorrect Address.")
                        .setView(textBoxView2)
                        .setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        EditText textField = (EditText)
                                                (textBoxView2.findViewById(R.id.calladdress_edit));
                                        sipAddress = textField.getText().toString();
                                        initiateCall();

                                    }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();
            case INCOMING_CALL:
    			return new AlertDialog.Builder(this)
    				.setMessage("Incoming Call")
    				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
    					
//    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    						// TODO Auto-generated method stub
    						IncomingCallReceiver.flag=true;
    					}
    				})
    				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
    					
//    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    						// TODO Auto-generated method stub
    						IncomingCallReceiver.flag=false;
    					}
    				})
    				.setCancelable(false)
    				.create();
        }
        return null;
    }

    public void updatePreferences() {
        Intent settingsActivity = new Intent(getBaseContext(),
                SipSettings.class);
        startActivity(settingsActivity);
    }
    
    public void endCurrentCall()
    {
//    	Toast.makeText(this, "i m here "+call+" "+call.isInCall(), Toast.LENGTH_LONG).show();
    	if(call != null && call.isInCall()) {
            try {
//              call.endCall();
//            	Toast.makeText(this, ""+call.getState(), Toast.LENGTH_LONG).show();
              updateStatus("Call ended by you.");
              try
              {
              	if(startTime > 0 && call.isInCall())
              	{
//            	  	Toast toast=Toast.makeText(getApplicationContext(), "Entering data in file", Toast.LENGTH_LONG);
//          			toast.show();
          			
              		stopTime=SystemClock.elapsedRealtime();
              		long milliseconds=stopTime-startTime;
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
          			callDuration=sb.toString();
              		CallInfo cinfo=new CallInfo(sipAddress.substring(4),callDate,callDuration,outgoingCall,false);
              		startTime=0;
              		
                    Context c=getApplicationContext();
                    am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
                    am.setMode(AudioManager.MODE_NORMAL);
                    am.setSpeakerphoneOn(false);
                    
                    Toast.makeText(this, "yeepi", Toast.LENGTH_LONG).show();
              		
              		file=new File("/data/data/com.example.android.sip/files/"+FILENAME);
                    if(!file.exists())
                    {
                    	fos = openFileOutput(FILENAME, Context.MODE_APPEND);
                    	oos=new ObjectOutputStream(fos);
                    }
                    else
                    {
                    	fos = openFileOutput(FILENAME, Context.MODE_APPEND);
                    	oos=new AppendableObjectOutputStream(fos);
                    }
              		oos.writeObject(cinfo);
              		oos.flush();
              		fos.close();
              		oos.close();
              		
              		if(mNotificationManager!=null)
              			mNotificationManager.cancel(HELLO_ID);
              		if(mSensorManager!=null)
              			mSensorManager.unregisterListener(this);
              		
//              		Toast toast2=Toast.makeText(getApplicationContext(), "Data entered successfully", Toast.LENGTH_LONG);
//                    toast2.show();
                    
                    call.endCall();
              	}
              }
              catch(Exception e)
              {
            	  Toast toast=Toast.makeText(getApplicationContext(), "Unable to enter data to file "+e, Toast.LENGTH_LONG);
                  toast.show();
              }
            } 
/*                    catch (SipException se) {
                Log.d("WalkieTalkieActivity/onOptionsItemSelected",
                        "Error ending call.", se);
                Toast toast=Toast.makeText(getApplicationContext(), "SipException "+se, Toast.LENGTH_LONG);
                toast.show();
            }*/finally{}
            call.close();
        }
    }
}
