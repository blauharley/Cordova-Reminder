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

import java.util.Calendar;

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
	private String stopDate;
	private float distanceTolerance;
	private boolean movingStatusChange;
	
	private float radiusDistance;
	private float linearDistance;
	private Integer desiredAccuracy = 0;
	private long currentMsTime;
	private int stopServiceDate = -1;
	
	private Handler mUserLocationHandler = null;
	private Thread triggerService = null;
	
	private boolean locSubscribed = false;
	
	private boolean goToHold = false;
	
	// wait at the beginning
	private long startTime;
	private long warmUpTime = 5000;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		title = intent.getExtras().getString("title");
		content = intent.getExtras().getString("content");
		distance = intent.getExtras().getFloat("distance");
		interval = intent.getExtras().getLong("interval");
		whistle = intent.getExtras().getBoolean("whistle");
		stopDate = intent.getExtras().getString("stopDate");
		distanceTolerance = intent.getExtras().getFloat("distanceTolerance");
		movingStatusChange = intent.getExtras().getBoolean("movingStatusChange");
		
		if(STOP_SERVICE_DATE_TOMORROW.equalsIgnoreCase(stopDate)){
			Calendar calendar = Calendar.getInstance();
			stopServiceDate = calendar.get(Calendar.DAY_OF_WEEK);
		}
		
		radiusDistance = 0;
		linearDistance = 0;
		
		startLoc = new Location("");
		startLoc.setLongitude(0);
		startLoc.setLatitude(0);
		
		lastloc = new Location("");
		lastloc.setLongitude(0);
		lastloc.setLatitude(0);
		
		startTime = System.currentTimeMillis();
		currentMsTime = startTime;
		
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
		                c.setHorizontalAccuracy(DESIRED_LOCATION_ACCURANCY_HIGH);
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
	
	private boolean timeWarmUpOut(){
		return System.currentTimeMillis() >= (startTime + warmUpTime);
	}
	
	private boolean handleServiceStop(){
		Calendar calendar = Calendar.getInstance();
		int currDay = calendar.get(Calendar.DAY_OF_WEEK);
		return stopServiceDate != -1 && stopServiceDate != currDay;	
	}
	
	private void cleanUp() {
		
		PackageManager pm = getPackageManager();
		Intent callingIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
		
		NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_ID);
		
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
			        .setContentText(content.replace("#ML", String.valueOf(linearDistance)).replace("#MR", String.valueOf(radiusDistance)))
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
		
		if(handleServiceStop()){
			stopSelf();
			return;
		}
		
		if(!timeWarmUpOut()){
			return;
		}
		
		float distanceStep = lastloc.distanceTo(location);
		boolean isStanding = goToHold;
		
		if(!movingStatusChange && distanceStep < distanceTolerance){
			return;
		}
		else if(movingStatusChange && distanceStep < distanceTolerance){
			distanceStep = 0;
			goToHold = true;
		}
		else if(movingStatusChange && distanceStep >= distanceTolerance){
			goToHold = false;
		}
		
		if(startLoc.getLatitude() == 0 && startLoc.getLongitude() == 0){
			linearDistance = 0;
			startLoc.set(location);
			lastloc.set(location);
		}
		else{
			linearDistance += distanceStep;
			radiusDistance = startLoc.distanceTo(location);
			lastloc.set(location);
		}
		
		/*
		 * 1.) either a certain distance is reached
		 * 2.) or user's moving is changed (run -> stop, stop -> run)
		 * either way a certain time has to be passed
		 */
		if( ((!movingStatusChange && linearDistance >= distance) || 
		    (movingStatusChange && isStanding != goToHold)) && 
		    timeOut()
		){
			
			startLoc.set(location);
			
			/*
			 * 1.) show notification when time and distance is reached
			 * 2.) show notification when user came to a stop
			 */
			if(!movingStatusChange || (movingStatusChange && goToHold)){
				showNotification();
			}
			
			linearDistance = 0;
			currentMsTime = System.currentTimeMillis();
			goToHold = isStanding;
			
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
	
}