package com.phonegap.reminder;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class NotifyStarterActivity extends Activity {

	public static final String APP_NAME = "com.phonegap.reminder";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PackageManager manager = getPackageManager();
        Intent launchApp= null;
        
        try {
        	
        	launchApp = manager.getLaunchIntentForPackage(APP_NAME);
        	
            if (launchApp == null)
                throw new PackageManager.NameNotFoundException();
            
            launchApp.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(launchApp);
            
            finish();
            
        } catch (PackageManager.NameNotFoundException e) {
        	Log.e("Reminder Loader", "loading intent error: "+APP_NAME);
        }
        
    }
}