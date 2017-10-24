package com.mkjihu.audioedit;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;

public class MyApplication extends Application{

	private static Context context;
    private static MyApplication instance;
	
    public MyApplication() {
        instance = this;
    }
	@Override
	public void onCreate() {
		super.onCreate();
		MyApplication.context = getApplicationContext();
		instance = this;
		
	}
	public static Context getAppContext() {
        return MyApplication.context;
    }
	public static MyApplication getInstance() {
        return instance;
    } 
	   
	    
	   
    /**判定是否在主線程*/
    public static String isInMainThread() {
        return Looper.myLooper() == Looper.getMainLooper() ? "是主線程":"不是主線程";
    }
  
}
