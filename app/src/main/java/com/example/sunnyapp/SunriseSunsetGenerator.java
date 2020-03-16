package com.example.sunnyapp;

import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class SunriseSunsetGenerator {

    public static SunriseSunset generate(Location location, String locationKey, Date currDateTime, JSONObject daily5DaysJSON) {
        SunriseSunset sunriseSunset = new SunriseSunset();

        sunriseSunset.setLatitude(location.getLatitude());
        sunriseSunset.setLongitude(location.getLongitude());
        sunriseSunset.setLocationKey(locationKey);
        sunriseSunset.setDateTime(DateStringConverter.dateToString(currDateTime));

        sunriseSunset.setSunrise(extractSunParamFromJSON(daily5DaysJSON, "Rise"));
        sunriseSunset.setSunset(extractSunParamFromJSON(daily5DaysJSON, "Set"));

        return sunriseSunset;
    }


    public static String extractSunParamFromJSON(JSONObject daily5DaysJSON, String param) {
        try {
            return daily5DaysJSON.getJSONArray("DailyForecasts").getJSONObject(0).getJSONObject("Sun").getString(param);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
