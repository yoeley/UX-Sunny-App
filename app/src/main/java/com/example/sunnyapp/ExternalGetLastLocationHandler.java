package com.example.sunnyapp;

public class ExternalGetLastLocationHandler {

    private static ExternalGetLastLocationHandler externalGetLastLocationHandler;

    private LoadWeatherActivity loadWeatherActivity;

    private ExternalGetLastLocationHandler() {
    }

    public static ExternalGetLastLocationHandler getInstance() {
        if (externalGetLastLocationHandler == null) {
            //synchronized block to remove overhead
            synchronized (ExternalGetLastLocationHandler.class) {
                if (externalGetLastLocationHandler == null) {
                    // if instance is null, initialize
                    externalGetLastLocationHandler = new ExternalGetLastLocationHandler();
                }
            }
        }
        return externalGetLastLocationHandler;
    }

    public void setLoadWeatherActivity(LoadWeatherActivity loadWeatherActivity) {
        this.loadWeatherActivity = loadWeatherActivity;
    }

    public void handleExternalGetLastLocation() {
        if (loadWeatherActivity != null) {
            loadWeatherActivity.externalGetLastLocation();
        }
    }
}
