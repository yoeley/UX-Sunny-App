package com.example.sunnyapp;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;

/**
 * this generator is written to match the current AccuWeather API and the current coputation of the
 * "Sunny factor" (for now, the sunny factor is simply the "RealFeelTemperature" from AccuWeather).
 * this implementation can and will probably change over time to match new API or new definitions
 * for the "Sunny factor"
 */
public class ForecastGenerator {

    public static Forecast generate(Location location, String locationKey, Date currDateTime, JSONArray forecastJSON, JSONArray currConditionsJSON) {
        Forecast forecast = new Forecast();

        forecast.setLatitude(location.getLatitude());
        forecast.setLongitude(location.getLongitude());
        forecast.setLocationKey(locationKey);
        forecast.setDateTime(currDateTime.toString());

        forecast.setCurrCondition(currConditionSunnyFactor(currConditionsJSON));
        forecast.setForeCast(forecastSunnyFactor(forecastJSON));
        return forecast;
    }

    private static Double currConditionSunnyFactor(JSONArray currConditionsJSON) {
        try {
           return currConditionsJSON.getJSONObject(0).getJSONObject("RealFeelTemperature").getJSONObject("Metric").getDouble("Value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ArrayList<Double> forecastSunnyFactor(JSONArray forecastJSON) {
        ArrayList<Double> forecastSunnyFactor = new ArrayList<Double>();

        try {
            for (int i = 0; i < forecastJSON.length(); ++i) {
                forecastSunnyFactor.add(forecastJSON.getJSONObject(i).getJSONObject("RealFeelTemperature").getDouble("Value"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return forecastSunnyFactor;
    }
}
