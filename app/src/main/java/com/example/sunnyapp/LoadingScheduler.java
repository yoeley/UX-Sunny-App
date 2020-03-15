package com.example.sunnyapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LoadingScheduler extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent loadWeatherActivity = new Intent(context, LoadWeatherActivity.class);
        context.startActivity(loadWeatherActivity);
    }
}
