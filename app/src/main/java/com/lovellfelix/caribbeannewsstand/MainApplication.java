package com.lovellfelix.caribbeannewsstand;

import android.app.Application;
import android.content.Context;

import com.lovellfelix.caribbeannewsstand.utils.PrefUtils;

public class MainApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false); // init
    }

    public static Context getContext() {
        return context;
    }
}
