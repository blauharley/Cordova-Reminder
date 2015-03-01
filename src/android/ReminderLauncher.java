package com.phonegap.reminder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class ReminderLauncher extends CordovaPlugin implements NotificationInterface, RunningInterface{

	public static final String ACTION_START = "start";
	public static final String ACTION_CLEAR = "clear";
	
	private String title;
	private String content;
	private float distance;
	private long interval;
	
	private Activity thisAct;
	private CallbackContext callCtx;
	
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
		
		try {
			
			thisAct = this.cordova.getActivity();
			
			title = args.getString(0);
			content = args.getString(1);
			
			interval = args.getInt(2);
			distance = (float)args.getDouble(3);
			
			if (ACTION_START.equalsIgnoreCase(action)) {
				callCtx = callbackContext;
				startReminderService();
				return true;
			}
			else if(ACTION_CLEAR.equalsIgnoreCase(action)){
				if(isRunning()){
					stopReminderService();
				}
				callbackContext.success();
				return true;
			}
			else{
				callbackContext.error("Call undefined action: "+action);
				return false;
			}
		
		} catch (JSONException e) {
			callbackContext.error("Reminder exception occured: "+e.toString());
			return false;
		}
		
		
	}

	private void startReminderService(){
		
		int currentapiVersion = Build.VERSION.SDK_INT;
		
		if (currentapiVersion >= Build.VERSION_CODES.FROYO){
		    
		    callCtx.success();
		    
			Intent mServiceIntent = new Intent(thisAct, ReminderService.class);
			mServiceIntent.putExtra("title", title);
			mServiceIntent.putExtra("content", content);
			mServiceIntent.putExtra("distance", distance);
			mServiceIntent.putExtra("interval", interval);
			
			thisAct.startService(mServiceIntent);
			
		} 
		else{
			callCtx.error("device does not support notifications");
		}
		
	}
	
	private void stopReminderService(){
		setRunning(false);
		NotificationManager mNotificationManager =
			    (NotificationManager) thisAct.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_ID);
	}
	
	public boolean isRunning() {
	    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(thisAct.getApplicationContext());
	    return pref.getBoolean(SERVICE_IS_RUNNING, false);
	}
	
	public void setRunning(boolean running) {
	    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(thisAct.getApplicationContext());
	    SharedPreferences.Editor editor = pref.edit();

	    editor.putBoolean(SERVICE_IS_RUNNING, running);
	    editor.apply();
	}
	
}