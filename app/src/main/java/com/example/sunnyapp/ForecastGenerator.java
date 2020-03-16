package com.example.sunnyapp;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * this generator is written to match the current AccuWeather API and the current coputation of the
 * "Sunny factor" (for now, the sunny factor is simply the "RealFeelTemperature" from AccuWeather).
 * this implementation can and will probably change over time to match new API or new definitions
 * for the "Sunny factor"
 */
public class ForecastGenerator {

    private static final Double bestRealFeelTemperature = 25.0; // in Celsius
    private static final Double bestWindSpeed = 0.0; // to be reevaluated in the future
    private static final Double bestWindDirection = 0.0; // to be reevaluated in the future
    private static final Double bestWindGust = 0.0; // to be reevaluated in the future
    private static final Double bestRain = 0.0; // to be reevaluated in the future
    private static final Double bestSnow = 0.0; // to be reevaluated in the future
    private static final Double bestIce = 0.0; // to be reevaluated in the future
    private static final Double bestUVIndex = 2.0; // out of 10
    private static final Double bestCloudCover = 20.0; // out of 100

    public static Forecast generate(Location location, String locationKey, Date currDateTime, JSONArray forecastJSON, JSONArray currConditionsJSON) {
        Forecast forecast = new Forecast();

        forecast.setLatitude(location.getLatitude());
        forecast.setLongitude(location.getLongitude());
        forecast.setLocationKey(locationKey);
        forecast.setDateTime(DateStringConverter.dateToString(currDateTime));

        forecast.setWeatherIcon(weatherIcon(currConditionsJSON));
        forecast.setCurrCondition(currConditionSunnyFactor(currConditionsJSON));
        forecast.setForeCast(forecastSunnyFactor(forecastJSON));
        return forecast;
    }

    private static int weatherIcon(JSONArray currConditionsJSON) {
        Integer weatherIcon = 0;
        try {
            weatherIcon = currConditionsJSON.getJSONObject(0).getInt("WeatherIcon");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return weatherIcon;
    }

    private static Double sunnyFactor(JSONObject forecastOneHour) {
        try {
            Double realFeelTemperature = forecastOneHour.getJSONObject("RealFeelTemperature").getDouble("Value");
            Double windSpeed = forecastOneHour.getJSONObject("Wind").getJSONObject("Speed").getDouble("Value");
            Double windDirection = forecastOneHour.getJSONObject("Wind").getJSONObject("Direction").getDouble("Degrees");
            Double windGust = forecastOneHour.getJSONObject("WindGust").getJSONObject("Speed").getDouble("Value");
            Double rain = forecastOneHour.getJSONObject("Rain").getDouble("Value");
            Double snow = forecastOneHour.getJSONObject("Snow").getDouble("Value");
            Double ice = forecastOneHour.getJSONObject("Ice").getDouble("Value");
            Double UVIndex = forecastOneHour.getDouble("UVIndex");
            Double cloudCover = forecastOneHour.getDouble("CloudCover");

            // temporary formula for the sunny factor. this formula is to be reevaluated when we gather more data from users
            Double sunnyFactor = 100
                    - Math.abs(bestRealFeelTemperature - realFeelTemperature)
                    - Math.log(Math.abs(bestWindSpeed - windSpeed) + 1)
                    - Math.log(Math.abs(bestRain - rain) + 1)
                    - Math.log(Math.abs(bestSnow - snow) + 1)
                    - Math.abs(bestUVIndex - UVIndex)
                    - Math.log(Math.abs(bestCloudCover - cloudCover) + 1);

            return sunnyFactor;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0.0;
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
                forecastSunnyFactor.add(sunnyFactor(forecastJSON.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Double minSunnyFactor = Collections.min(forecastSunnyFactor);
        Double maxSunnyFactor = Collections.max(forecastSunnyFactor) - minSunnyFactor;

        for (int i = 0; i < forecastSunnyFactor.size(); ++i) {
            forecastSunnyFactor.set(i, (forecastSunnyFactor.get(i) - minSunnyFactor) / maxSunnyFactor);
        }

        return forecastSunnyFactor;
    }
}
