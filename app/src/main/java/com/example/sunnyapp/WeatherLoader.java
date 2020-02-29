package com.example.sunnyapp;

import android.location.Location;
import android.os.AsyncTask;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class WeatherLoader {

    private final static String apiKey = "oOAbPsquosOnBVI0xjiZGLuYWAZBYajt";
    private final static String keyRequestBodyFormat = "https://dataservice.accuweather.com/locations/v1/cities/geoposition/search?apikey=%s&q=%s%%2C%s&toplevel=true";
    private final static String weatherRequestBodyFormat = "http://dataservice.accuweather.com/forecasts/v1/hourly/24hour/%s?apikey=%s&details=true&metric=true";
    private final static String currConditionsRequestBodyFormat = "http://dataservice.accuweather.com/currentconditions/v1/%s?apikey=%s&details=true";
    private final static String daily5DaysRequestBodyFormat = "http://dataservice.accuweather.com/forecasts/v1/daily/5day/%s?apikey=%s&details=true&metric=true";

    private Location location = null;

    private static WeatherLoader weatherLoader = null;
    private LoadWeatherActivity loadWeatherActivity;

    private Forecast forecast;
    private SunriseSunset sunriseSunset;

    private WeatherLoader() {
    }

    public static WeatherLoader getInstance()
    {
        if (weatherLoader == null)
        {
            //synchronized block to remove overhead
            synchronized (WeatherLoader.class)
            {
                if(weatherLoader == null)
                {
                    // if instance is null, initialize
                    weatherLoader = new WeatherLoader();
                }
            }
        }
        return weatherLoader;
    }

    public void getWeather() {
        new getWeatherTask().execute();
    }

    public void setLoadWeatherActivity(LoadWeatherActivity loadWeatherActivity) {
        this.loadWeatherActivity = loadWeatherActivity;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Forecast getForecast() {
        return forecast;
    }

    public SunriseSunset getSunriseSunset() {
        return sunriseSunset;
    }

    private class getWeatherTask extends AsyncTask<Integer, Integer, Integer> {

        private String obtainLocationKey(OkHttpClient client) {
            Request keyRequest = new Request.Builder()
                    .url(String.format(keyRequestBodyFormat,  apiKey, Double.toString(location.getLatitude()), Double.toString(location.getLongitude())))
                    .build();

            Response keyResponse = null;
            String keyResult = null;
            JSONObject keyResultJSON = null;
            String locationKey = null;
            try {
                keyResponse = client.newCall(keyRequest).execute();
                keyResult = keyResponse.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (keyResult != null) {
                try {
                    keyResultJSON = new JSONObject(keyResult);
                    locationKey = keyResultJSON.getString("Key");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return locationKey;
        }

        private JSONArray obtainWeatherForecastJSON(OkHttpClient client, String locationKey) {
            Request weatherRrequest = new Request.Builder()
                    .url(String.format(weatherRequestBodyFormat,  locationKey, apiKey))
                    .build();

            Response weatherResponse = null;
            String weatherResult = null;
            JSONArray weatherResultJSON = null;
            try {
                weatherResponse = client.newCall(weatherRrequest).execute();
                weatherResult = weatherResponse.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (weatherResult != null) {
                try {
                    weatherResultJSON = new JSONArray(weatherResult);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return weatherResultJSON;
        }

        private JSONArray obtainCurrConditions(OkHttpClient client, String locationKey) {
            Request currConditionsRrequest = new Request.Builder()
                    .url(String.format(currConditionsRequestBodyFormat,  locationKey, apiKey))
                    .build();

            Response currConditionsResponse = null;
            String currConditionsResult = null;
            JSONArray currConditionsJSON = null;
            try {
                currConditionsResponse = client.newCall(currConditionsRrequest).execute();
                currConditionsResult = currConditionsResponse.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (currConditionsResult != null) {
                try {
                    currConditionsJSON = new JSONArray(currConditionsResult);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return currConditionsJSON;
        }

        private JSONObject obtainDaily5DayForecast(OkHttpClient client, String locationKey) {
            Request daily5DaysRrequest = new Request.Builder()
                    .url(String.format(daily5DaysRequestBodyFormat,  locationKey, apiKey))
                    .build();

            Response daily5DaysResponse = null;
            String daily5DaysResult = null;
            JSONObject daily5DaysJSON = null;
            try {
                daily5DaysResponse = client.newCall(daily5DaysRrequest).execute();
                daily5DaysResult = daily5DaysResponse.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (daily5DaysResult != null) {
                try {
                    daily5DaysJSON = new JSONObject(daily5DaysResult);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return daily5DaysJSON;
        }

        @Override
        protected Integer doInBackground(Integer... callers) {

            OkHttpClient client = new OkHttpClient();

            String locationKey = obtainLocationKey(client);
            JSONArray forecastJSON = obtainWeatherForecastJSON(client, locationKey);
            JSONArray currConditionsJSON = obtainCurrConditions(client, locationKey);
            JSONObject daily5DaysJSON = obtainDaily5DayForecast(client, locationKey);

            Date currDateTime = Calendar.getInstance().getTime();

            forecast = ForecastGenerator.generate(location, locationKey, currDateTime, forecastJSON, currConditionsJSON);
            sunriseSunset = SunriseSunsetGenerator.generate(location, locationKey, currDateTime, daily5DaysJSON);

            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Integer result) {
            //TODO: call loadWeatherActivity's method for going to display activity
        }
    }
}
