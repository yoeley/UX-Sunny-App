package com.example.sunnyapp;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * generates Forecast objects out of raw data.
 * this generator is written to match the current AccuWeather API and the current computation of the
 * "Sunny factor". this implementation can and will probably change over time to match new API or
 * new definitions for the "Sunny factor"
 */
public class ForecastGenerator {

    private static final Double bestRealFeelTemperature = 25.0; // in Celsius
    private static final Double bestWindSpeed = 0.0; // to be reevaluated in the future
    private static final Double bestWindDirection = 0.0; // to be reevaluated in the future
    private static final Double bestWindGust = 0.0; // to be reevaluated in the future
    private static final Double bestRain = 0.0; // to be reevaluated in the future
    private static final Double bestSnow = 0.0; // to be reevaluated in the future
    private static final Double bestIce = 0.0; // to be reevaluated in the future
    private static final Double bestPercip1Hour = 0.0; // to be reevaluated in the future
    private static final Double bestUVIndex = 2.0; // out of 10
    private static final Double bestCloudCover = 20.0; // out of 100

    /**
     * generates a forecast object out of data (JSON strings) retrieved from AccuWeather
     * @param location
     * @param locationKey
     * @param currDateTime
     * @param forecastJSON
     * @param currConditionsJSON
     * @return
     * @throws JSONException
     */
    public static Forecast generate(Location location, String locationKey, Date currDateTime, JSONArray forecastJSON, JSONArray currConditionsJSON) throws JSONException {
        Forecast forecast = new Forecast();

        forecast.setLatitude(location.getLatitude());
        forecast.setLongitude(location.getLongitude());
        forecast.setLocationKey(locationKey);
        forecast.setDateTime(DateStringConverter.dateToString(currDateTime));

        forecast.setWeatherIcon(weatherIcon(currConditionsJSON));
        forecast.setForeCast(forecastSunnyFactor(forecastJSON, currConditionsJSON));
        return forecast;
    }

    /**
     * gets the number of the weather icon (which sums up the current weather for visual display)
     * @param currConditionsJSON
     * @return
     * @throws JSONException
     */
    private static int weatherIcon(JSONArray currConditionsJSON) throws JSONException {
        return currConditionsJSON.getJSONObject(0).getInt("WeatherIcon");
    }

    /**
     * gets the SunnyFactor for future time
     * @param forecastOneHour
     * @return
     * @throws JSONException
     */
    private static Double forecastSunnyFactor(JSONObject forecastOneHour) throws JSONException {
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
        return  (100
                - Math.abs(bestRealFeelTemperature - realFeelTemperature)
                - Math.log(Math.abs(bestWindSpeed - windSpeed) + 1)
                - Math.log(Math.abs(bestRain - rain) + 1)
                - Math.log(Math.abs(bestSnow - snow) + 1)
                - Math.abs(bestUVIndex - UVIndex)
                - Math.log(Math.abs(bestCloudCover - cloudCover) + 1));
    }

    /**
     * gets the SunnyFactor for current time
     * @param forecastOneHour
     * @return
     * @throws JSONException
     */
    private static Double currConditionsSunnyFactor(JSONObject forecastOneHour) throws JSONException {
        Double realFeelTemperature = forecastOneHour.getJSONObject("RealFeelTemperature").getJSONObject("Metric").getDouble("Value");
        Double windSpeed = forecastOneHour.getJSONObject("Wind").getJSONObject("Speed").getJSONObject("Metric").getDouble("Value");
        Double windDirection = forecastOneHour.getJSONObject("Wind").getJSONObject("Direction").getDouble("Degrees");
        Double windGust = forecastOneHour.getJSONObject("WindGust").getJSONObject("Speed").getJSONObject("Metric").getDouble("Value");
        Double percip1Hour = forecastOneHour.getJSONObject("Precip1hr").getJSONObject("Metric").getDouble("Value");
        Double UVIndex = forecastOneHour.getDouble("UVIndex");
        Double cloudCover = forecastOneHour.getDouble("CloudCover");

        // temporary formula for the sunny factor. this formula is to be reevaluated when we gather more data from users
        return  (100
                - Math.abs(bestRealFeelTemperature - realFeelTemperature)
                - Math.log(Math.abs(bestWindSpeed - windSpeed) + 1)
                - Math.log(Math.abs(bestPercip1Hour - percip1Hour) + 1)
                - Math.abs(bestUVIndex - UVIndex)
                - Math.log(Math.abs(bestCloudCover - cloudCover) + 1));
    }

    /**
     * gets the SunnyFactor for the next 12 hours
     * @param forecastJSON
     * @param currConditionsJSON
     * @return
     * @throws JSONException
     */
    private static ArrayList<Double> forecastSunnyFactor(JSONArray forecastJSON, JSONArray currConditionsJSON) throws JSONException {
        ArrayList<Double> forecastSunnyFactor = new ArrayList<Double>();

        forecastSunnyFactor.add(currConditionsSunnyFactor(currConditionsJSON.getJSONObject(0)));
        for (int i = 0; i < forecastJSON.length(); ++i) {
            forecastSunnyFactor.add(forecastSunnyFactor(forecastJSON.getJSONObject(i)));
        }

        Double minSunnyFactor = Collections.min(forecastSunnyFactor);
        Double maxSunnyFactor = Collections.max(forecastSunnyFactor) - minSunnyFactor;

        for (int i = 0; i < forecastSunnyFactor.size(); ++i) {
            forecastSunnyFactor.set(i, (forecastSunnyFactor.get(i) - minSunnyFactor) / maxSunnyFactor);
        }
        return forecastSunnyFactor;
    }
}
