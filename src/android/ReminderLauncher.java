package com.phonegap.reminder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.GpsStatus;

import java.util.List;
import java.util.Iterator;


public class ReminderLauncher extends CordovaPlugin implements NotificationInterface, RunningInterface, LocationListener, GpsStatus.Listener{

	public static final String ACTION_START = "start";
	public static final String ACTION_CLEAR = "clear";
	public static final String ACTION_REQUEST_PROVIDER = "request";
	public static final String ACTION_IS_RUNNING = "isrunning";
	
	private String title;
	private String content;
	private float distance;
	private long interval;
	private boolean whistle;
	private boolean closeApp;
	private String stopDate;
	private float distanceTolerance;
	private String mode;
	private double aimLat;
	private double aimLong;
	private boolean aggressive;
	
	// wait at the beginning
	private long startTime;
	private long warmUpTime = 10000;
	private LocationManager locationManager;
	
	private Handler serviceHandler = null;
	
	private boolean providerEnabled = true;
	private int providerStatus = -1;
	private Location mLastLocation = null;
	private long mLastLocationMillis = 0;
	private boolean isGPSAvailable = false;
	private GpsStatus gpsStatus = null;
	
	private Activity thisAct;
	private CallbackContext callCtx;
	
	class timer implements Runnable {
          public void run() {
          	
          	if(mLastLocation == null || isGPSAvailable == false){
          		providerStatus = LocationProvider.OUT_OF_SERVICE;
          		sendProviderResponseByLocation(new Location(getBestProvider()));
          	}
          	
          }
    }
    
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
		
		try {
			
			thisAct = this.cordova.getActivity();
			
			callCtx = callbackContext;
			
			startTime = System.currentTimeMillis();
			
			if (ACTION_START.equalsIgnoreCase(action)) {
				
				title = args.getString(0);
				content = args.getString(1);
				
				interval = args.getInt(2);
				distance = (float)args.getDouble(3);
				
				whistle = args.getBoolean(4);
				closeApp = args.getBoolean(5);
				
				stopDate = args.getString(6);
				
				aggressive = args.getBoolean(11);
				
				distanceTolerance = (float)args.getDouble(7);
				mode = args.getString(8);
				
				aimLat = (float)args.getDouble(9);
				aimLong = (float)args.getDouble(10);
				
				if(isRunning()){
					stopReminderService();
				}
				
				startReminderService();
				return true;
			}
			else if(ACTION_CLEAR.equalsIgnoreCase(action)){
				stopReminderService();
				callbackContext.success();
				return true;
			}
			else if(ACTION_REQUEST_PROVIDER.equalsIgnoreCase(action)){
				PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
				r.setKeepCallback(true);
				callbackContext.sendPluginResult(r);
				requestLocationAccurancy();
				return true;
			}
			else if(ACTION_IS_RUNNING.equalsIgnoreCase(action)){
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("isRunning",isRunning());
				callbackContext.success(jsonObj);
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
			mServiceIntent.putExtra("whistle", whistle);
			mServiceIntent.putExtra("stopDate", stopDate);
			mServiceIntent.putExtra("distanceTolerance", distanceTolerance);
			mServiceIntent.putExtra("mode", mode);
			mServiceIntent.putExtra("aimLat", aimLat);
			mServiceIntent.putExtra("aimLong", aimLong);
			mServiceIntent.putExtra("aggressive", aggressive);
			
			mServiceIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			
			thisAct.startService(mServiceIntent);
			
			setRunning(true);
			
			if(closeApp){
				thisAct.finish();
			}
			
		} 
		else{
			callCtx.error("device does not support notifications");
		}
		
	}
	
	private void stopReminderService(){
		Intent mServiceIntent = new Intent(thisAct, ReminderService.class);
		thisAct.stopService(mServiceIntent);
	}
	
	private String getBestProvider(){
		
		Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_LOW);
        c.setHorizontalAccuracy(DESIRED_LOCATION_ACCURANCY_MEDIUM);
        c.setPowerRequirement(Criteria.POWER_HIGH);

        locationManager = (LocationManager) thisAct.getSystemService(Context.LOCATION_SERVICE);
        
        return locationManager.getBestProvider(c,true);
        
	}
	
	private void requestLocationAccurancy(){
		
		locationManager = (LocationManager) thisAct.getSystemService(Context.LOCATION_SERVICE);
		
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
		   locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER )){
			
			List<String> allProviders = locationManager.getAllProviders();
	        for (String provider: allProviders) {
	        	locationManager.requestLocationUpdates(provider, 0, 0, this);
	        }
	        locationManager.addGpsStatusListener(this);
	        
			serviceHandler = new Handler();
	        serviceHandler.postDelayed( new timer(),warmUpTime);
	        
	        mLastLocation = null;
	        isGPSAvailable = false;
	        providerEnabled = false;
	        gpsStatus = null;
			
		}
		else{
			
			PluginResult r = new PluginResult(PluginResult.Status.ERROR,"provider is not enabled");
			callCtx.sendPluginResult(r);
			
		}
		
	}
	
	private boolean timeWarmUpOut(){
		return System.currentTimeMillis() >= (startTime + warmUpTime);
	}
	
	public boolean isRunning() {
		ActivityManager manager = (ActivityManager) thisAct.getSystemService(thisAct.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (ReminderService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	public void setRunning(boolean running) {}
	
	private void sendProviderResponseByLocation(Location location){
		
		try{
			
			JSONObject jsonObj = new JSONObject();
	
	        JSONObject coords = new JSONObject();
	        
	        coords.put("latitude",location.getLatitude());
	        coords.put("longitude",location.getLongitude());
	        
	        coords.put("accurancy", location.getAccuracy());
	        coords.put("provider_enabled", providerEnabled);
	        coords.put("gps_fix", isGPSAvailable);
	        
	        if(location.hasBearing()){
	        	coords.put("heading", location.getBearing());	
	        }
	        if(location.hasAltitude()){
	        	coords.put("altitude", location.getAltitude());	
	        }
	        if(location.hasSpeed()){
	        	coords.put("speed", location.getSpeed());	
	        }
	        
	        coords.put("out_of_service", (LocationProvider.OUT_OF_SERVICE == providerStatus ? true : false));
			
			jsonObj.put("coords",coords);
			jsonObj.put("timestamp", System.currentTimeMillis()-gpsStatus.getTimeToFirstFix());
			
			callCtx.sendPluginResult(new PluginResult(PluginResult.Status.OK, jsonObj));
			
		}
		catch (JSONException e){
			callCtx.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
        }
	}
	
    @Override
    public void onLocationChanged(Location location) {
		
		providerEnabled = true;
		
		mLastLocation = location;
		mLastLocationMillis = System.currentTimeMillis();
		
		if(!timeWarmUpOut()){
			return;
		}
		
		locationManager.removeUpdates(this);
		locationManager.removeGpsStatusListener(this);
		
		sendProviderResponseByLocation(location);
		
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
		providerStatus = status;
    }

    @Override
    public void onProviderEnabled(String provider) {
		providerEnabled = true;
    }

    @Override
    public void onProviderDisabled(String provider) {
		providerEnabled = false;
    }
    
    public void onGpsStatusChanged(int event) {
    	gpsStatus = locationManager.getGpsStatus(gpsStatus);
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            
                if (mLastLocation != null){
                    isGPSAvailable = (System.currentTimeMillis() - mLastLocationMillis) < warmUpTime;
				}
				else{
					isGPSAvailable = false;
				}
				
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                isGPSAvailable = true;
                break;
        }
    }
}