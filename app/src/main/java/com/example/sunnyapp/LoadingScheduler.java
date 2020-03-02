package com.example.sunnyapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LoadingScheduler extends BroadcastReceiver
{
    private LoadWeatherActivity loadWeatherActivity;

    public void setLoadWeatherActivity(LoadWeatherActivity loadWeatherActivity) {
        this.loadWeatherActivity = loadWeatherActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        ExternalGetLastLocationHandler.getInstance().handleExternalGetLastLocation();
    }
}
