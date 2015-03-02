package com.phonegap.reminder;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;

public class ReminderService extends Service implements LocationListener, NotificationInterface{
	
	private final static String name = "ReminderService";
	private Location startLoc;
	private Location lastloc;
	private LocationManager locationManager;
	
	private String title;
	private String content;
	private float distance;
	private long interval;
	private boolean whistle;
		
	private float radiusDistance;
	private float linearDistance;
	private Integer desiredAccuracy = 100;
	private long currentMsTime;

	private Handler mUserLocationHandler = null;
	private Thread triggerService = null;
	
	private boolean locSubscribed = false;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		title = intent.getExtras().getString("title");
		content = intent.getExtras().getString("content");
		distance = intent.getExtras().getFloat("distance");
		interval = intent.getExtras().getLong("interval");
		whistle = intent.getExtras().getBoolean("whistle");
		
		radiusDistance = 0;
		linearDistance = 0;
		
		startLoc = new Location("");
		startLoc.setLongitude(0);
		startLoc.setLatitude(0);
		
		lastloc = new Location("");
		lastloc.setLongitude(0);
		lastloc.setLatitude(0);
		
		currentMsTime = System.currentTimeMillis();
		
		final ReminderService thisObj = this;
		
		triggerService = new Thread(new Runnable(){
			@TargetApi(16)
	        public void run(){
	            try{
	            	
	                Looper.prepare();
	                
	                if(isRunning() && !locSubscribed){
	                	
	                	locSubscribed = true;
	                	
		                mUserLocationHandler = new Handler();
		                
		                Criteria c = new Criteria();
		                c.setAccuracy(Criteria.ACCURACY_COARSE);
		                c.setHorizontalAccuracy(translateDesiredAccuracy(desiredAccuracy));
		                c.setPowerRequirement(Criteria.POWER_HIGH);
		                
		                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		                final String PROVIDER = locationManager.getBestProvider(c, true);
		                
		        		locationManager.requestLocationUpdates(PROVIDER, 0, 0, thisObj);
		        		
	                }
	                
	                Looper.loop();
		              
	            }catch(Exception ex){
	            	
	            }
	        }
	    }, "LocationThread");
	    
	    setRunning(true);
	    
	    triggerService.start();
	    
	    return START_REDELIVER_INTENT;
	    
	}
	
	@Override
	public boolean stopService(Intent intent) {
		
		cleanUp();
		
		return super.stopService(intent);

	}

	@Override
	public void onDestroy() {
	
		cleanUp();
		
	}
	
	public boolean isRunning() {
	    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	    return pref.getBoolean(SERVICE_IS_RUNNING, false);
	}
	
	public void setRunning(boolean running) {
	    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	    SharedPreferences.Editor editor = pref.edit();

	    editor.putBoolean(SERVICE_IS_RUNNING, running);
	    editor.apply();
	}
	
	private void makeWhistle(){
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
	    r.play();
	}
	
	private boolean timeOut(){
		return System.currentTimeMillis() >= (currentMsTime + interval);
	}
	
	private void cleanUp() {
		
		locationManager.removeUpdates(this);
	
		if(mUserLocationHandler != null){
			mUserLocationHandler.getLooper().quit();
		}
		
		triggerService.interrupt();
		
	}
	
	@TargetApi(16)
	private void showNotification(){
		
		if(!isRunning()){
			
    		cleanUp();
    		
		}
		else{
			
			Notification.Builder builder = new Notification.Builder(this)
			        .setSmallIcon(getResources().getIdentifier("ic_billclick_large", "drawable", getPackageName()))
			        .setContentTitle(title)
			        .setContentText(content.replace("#METER", String.valueOf(linearDistance)))
			        .setAutoCancel(true);
	
			int requestID = (int) System.currentTimeMillis();
			
			PackageManager pm = getPackageManager();
			Intent resultIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
			
			resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); 

			PendingIntent resultPendingIntent =
			    PendingIntent.getActivity(
			    this,
			    requestID,
			    resultIntent,
			    PendingIntent.FLAG_UPDATE_CURRENT
			);
			
			builder.setContentIntent(resultPendingIntent);
			
			NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			
			Notification note = builder.build();
			note.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
			
			mNotificationManager.notify(NOTIFICATION_ID, note);
			
			if(whistle){
				makeWhistle();
			}
			
		}
		
	}

	@Override
	public void onLocationChanged(Location location) {
		
		if(startLoc.getLatitude() == 0 && startLoc.getLongitude() == 0){
			linearDistance = 0;
			startLoc.set(location);
			lastloc.set(location);
		}
		else{
			linearDistance += lastloc.distanceTo(location);
			radiusDistance = startLoc.distanceTo(location);
			lastloc.set(location);
		}
		
		if( linearDistance >= distance && timeOut()){
			startLoc.set(location);
			showNotification();
			linearDistance = 0;
			currentMsTime = System.currentTimeMillis();
		}
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Integer translateDesiredAccuracy(Integer accuracy) {
		switch (accuracy) {
		case 1000:
		accuracy = Criteria.ACCURACY_LOW;
		break;
		case 100:
		accuracy = Criteria.ACCURACY_MEDIUM;
		break;
		case 10:
		accuracy = Criteria.ACCURACY_HIGH;
		break;
		case 0:
		accuracy = Criteria.ACCURACY_HIGH;
		break;
		default:
		accuracy = Criteria.ACCURACY_MEDIUM;
		}
		return accuracy;
	}

}