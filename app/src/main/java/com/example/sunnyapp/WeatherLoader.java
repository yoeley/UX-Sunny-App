package com.example.sunnyapp;

import android.location.Location;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

public class WeatherLoader {

    private final int EARTH_RADIUS = 6371;

    private final static String apiKey = "oOAbPsquosOnBVI0xjiZGLuYWAZBYajt";
    private final static String keyRequestBodyFormat = "https://dataservice.accuweather.com/locations/v1/cities/geoposition/search?apikey=%s&q=%s%%2C%s&toplevel=true";
    private final static String weatherRequestBodyFormat = "http://dataservice.accuweather.com/forecasts/v1/hourly/12hour/%s?apikey=%s&details=true&metric=true";
    private final static String currConditionsRequestBodyFormat = "http://dataservice.accuweather.com/currentconditions/v1/%s?apikey=%s&details=true";
    private final static String daily5DaysRequestBodyFormat = "http://dataservice.accuweather.com/forecasts/v1/daily/5day/%s?apikey=%s&details=true&metric=true";
    private final WeatherDataController weatherDataController = WeatherDataController.getInstance();

    private final static long ONE_HOUR = 3600000; // in millis
    private final static long ONE_DAY = 86400000; // in millis
    private final int NUM_OF_ATTEMPTS = 5;

    private Location location = null;

    private static WeatherLoader weatherLoader = null;
    private MainActivity mainActivity;

    private Forecast forecast;
    private SunriseSunset sunriseSunset;
    private LocationInfo locationInfo;

    private WeatherLoader() {
    }

    public static WeatherLoader getInstance() {
        if (weatherLoader == null) {
            //synchronized block to remove overhead
            synchronized (WeatherLoader.class) {
                if (weatherLoader == null) {
                    // if instance is null, initialize
                    weatherLoader = new WeatherLoader();
                }
            }
        }
        return weatherLoader;
    }

    public void loadWeather() {
        new getWeatherTask().execute();
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
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

    @NonNull
    public static String staticToString() {
        return "The object as a string.";
    }

    public long getPickWeatherTimeMillis() {
        ArrayList<Double> currForecast = forecast.getForeCast();
        Double maxForecast = Collections.max(currForecast);
        long indexOfMaxForecast = currForecast.indexOf(maxForecast);

        long forecastTimeInMillis = DateStringConverter.stringToDate(forecast.getDateTime()).getTime();
        long forecastTimeRoundedToNextHour = forecastTimeInMillis + (ONE_HOUR - (forecastTimeInMillis % ONE_HOUR));

        return forecastTimeRoundedToNextHour + indexOfMaxForecast * ONE_HOUR;
    }

    private class getWeatherTask extends AsyncTask<Integer, Integer, Integer> {

        private String obtainLocationKey(OkHttpClient client) {
            Request keyRequest = new Request.Builder()
                    .url(String.format(keyRequestBodyFormat, apiKey, Double.toString(location.getLatitude()), Double.toString(location.getLongitude())))
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
                    .url(String.format(weatherRequestBodyFormat, locationKey, apiKey))
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
                    .url(String.format(currConditionsRequestBodyFormat, locationKey, apiKey))
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
                    .url(String.format(daily5DaysRequestBodyFormat, locationKey, apiKey))
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

        private Boolean checkLocationUpToDate(Double formerLatitude, Double formerLongitude) {
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();

            float results[] = new float[1];
            try {
                Location.distanceBetween(formerLatitude, formerLongitude, latitude, longitude, results);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

            if (results[0] > 1000) {
                return false;
            }
            return true;
        }

        private Boolean checkSunriseSunsetUpToDate(Date currDateTime) {
            if (sunriseSunset == null) {
                return false;
            }

            if (!checkLocationUpToDate(sunriseSunset.getLatitude(), sunriseSunset.getLongitude())) {
                return false;
            }

            long formerTime = DateStringConverter.stringToDate(sunriseSunset.getDateTime()).getTime();
            long currTime = currDateTime.getTime();

            // if current time and time when the sunriseSunset was taken don't have the same day, return false
            if ((formerTime - (formerTime % ONE_DAY)) - (currTime - (currTime % ONE_DAY)) != 0) {
                return false;
            }
            return true;
        }

        private Boolean checkForecastUpToDate(Date currDateTime) {
            if (forecast == null) {
                return false;
            }

            if (!checkLocationUpToDate(forecast.getLatitude(), forecast.getLongitude())) {
                return false;
            }

            long formerTime = DateStringConverter.stringToDate(forecast.getDateTime()).getTime();
            long currTime = currDateTime.getTime();

            // if current time and time when the forecast was taken don't have the same hour in the day, return false
            if ((formerTime - (formerTime % ONE_HOUR)) - (currTime - (currTime % ONE_HOUR)) != 0) {
                return false;
            }
            return true;
        }

        private void updateLocationInfo(OkHttpClient client) {
            Request keyRequest = new Request.Builder()
                    .url(String.format(keyRequestBodyFormat,  apiKey, Double.toString(location.getLatitude()), Double.toString(location.getLongitude())))
                    .build();

            Response keyResponse = null;
            String keyResult = null;
            JSONObject keyResultJSON = null;
            String locationKey = null;
            String country = null;
            String city = null;
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
                    country = keyResultJSON.getJSONObject("Country").getString("ID");
                    city = keyResultJSON.getString("EnglishName");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            locationInfo = new LocationInfo(locationKey, country, city);
        }

        @Override
        protected Integer doInBackground(Integer... callers) {

            OkHttpClient client = new OkHttpClient();
            Date currDateTime = Calendar.getInstance().getTime();

            // checking if current forecast and sunriseSunset are up to date. if not, try loading from firebase.
            if (!checkSunriseSunsetUpToDate(currDateTime)) {
                updateLocationInfo(client);
                getForcastFromDB();
                getSunTimeFromDB();
            }
            else if (!checkForecastUpToDate(currDateTime)) {
                updateLocationInfo(client);
                getForcastFromDB();
            }
            else {
                // everything was already up to date - quit function
                return null;
            }

//            String locationKey = locationInfo.getLocationKey();
            String locationKey = locationKeyExample;

//            checking if firebase forecast and sunriseSunset are up to date. if not, try querying AccuWeather.
            if (!checkSunriseSunsetUpToDate(currDateTime)) {
//                uncomment this section to work with real data from AccuWeather
                ////////////////////////////////////////////////////////////////////////////////////
//                JSONArray forecastJSON = obtainWeatherForecastJSON(client, locationKey);
//                JSONArray currConditionsJSON = obtainCurrConditions(client, locationKey);
//                JSONObject daily5DaysJSON = obtainDaily5DayForecast(client, locationKey);
                ////////////////////////////////////////////////////////////////////////////////////

//                uncomment this section to work with example data
                ////////////////////////////////////////////////////////////////////////////////////
                JSONArray forecastJSON = null;
                JSONArray currConditionsJSON = null;
                JSONObject daily5DaysJSON = null;
                try {
                    forecastJSON = new JSONArray(forecastJSONExample);
                    currConditionsJSON = new JSONArray(currConditionsJSONExample);
                    daily5DaysJSON = new JSONObject(daily5DaysJSONExample);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ////////////////////////////////////////////////////////////////////////////////////

                forecast = ForecastGenerator.generate(location, locationKey, currDateTime, forecastJSON, currConditionsJSON);
                sunriseSunset = SunriseSunsetGenerator.generate(location, locationKey, currDateTime, daily5DaysJSON);
                weatherDataController.saveForecastDataByLocation(mainActivity, forecast, locationInfo.getCountry(), locationInfo.getCity());
                weatherDataController.saveSunTimesDataByLocation(mainActivity, sunriseSunset, locationInfo.getCountry(), locationInfo.getCity());

            }
            else if (!checkForecastUpToDate(currDateTime)) {
//                uncomment this section to work with real data from AccuWeather
                ////////////////////////////////////////////////////////////////////////////////////
//                JSONArray forecastJSON = obtainWeatherForecastJSON(client, locationKey);
//                JSONArray currConditionsJSON = obtainCurrConditions(client, locationKey);
                ////////////////////////////////////////////////////////////////////////////////////

//                uncomment this section to work with example data
                ////////////////////////////////////////////////////////////////////////////////////
                JSONArray forecastJSON = null;
                JSONArray currConditionsJSON = null;
                try {
                    forecastJSON = new JSONArray(forecastJSONExample);
                    currConditionsJSON = new JSONArray(currConditionsJSONExample);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ////////////////////////////////////////////////////////////////////////////////////

                forecast = ForecastGenerator.generate(location, locationKey, currDateTime, forecastJSON, currConditionsJSON);
                weatherDataController.saveForecastDataByLocation(mainActivity, forecast, locationInfo.getCountry(), locationInfo.getCity());
            }
            else {
                // everything from firebase was already up to date - quit function
                return null;
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Integer result) {
            mainActivity.goToDisplayWeatherActivity();
        }

        private final String locationKeyExample = "213225";
        private final String forecastJSONExample = "[{\"DateTime\":\"2020-02-29T20:00:00+02:00\",\"EpochDateTime\":1582999200,\"WeatherIcon\":35,\"IconPhrase\":\"Partly cloudy\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":15.4,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":13.5,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":12.1,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":9.2,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":14.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":184,\"Localized\":\"S\",\"English\":\"S\"}},\"WindGust\":{\"Speed\":{\"Value\":18.5,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":67,\"Visibility\":{\"Value\":8,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":0,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":48,\"RainProbability\":48,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":41,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=1&hbhhour=20&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=1&hbhhour=20&unit=c&lang=en-us\"},{\"DateTime\":\"2020-02-29T21:00:00+02:00\",\"EpochDateTime\":1583002800,\"WeatherIcon\":40,\"IconPhrase\":\"Mostly cloudy w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Moderate\",\"IsDaylight\":false,\"Temperature\":{\"Value\":14.7,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":11.1,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":12.3,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":10,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":14.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":187,\"Localized\":\"S\",\"English\":\"S\"}},\"WindGust\":{\"Speed\":{\"Value\":20.4,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":73,\"Visibility\":{\"Value\":8,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":0,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":52,\"RainProbability\":52,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":2.1,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":2.1,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":56,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=1&hbhhour=21&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=1&hbhhour=21&unit=c&lang=en-us\"},{\"DateTime\":\"2020-02-29T22:00:00+02:00\",\"EpochDateTime\":1583006400,\"WeatherIcon\":12,\"IconPhrase\":\"Showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Moderate\",\"IsDaylight\":false,\"Temperature\":{\"Value\":14.1,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":9.1,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":12.6,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":11.2,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":18.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":188,\"Localized\":\"S\",\"English\":\"S\"}},\"WindGust\":{\"Speed\":{\"Value\":31.5,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":83,\"Visibility\":{\"Value\":6.4,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":0,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":64,\"RainProbability\":64,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":4.2,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":4.2,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":77,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=1&hbhhour=22&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=1&hbhhour=22&unit=c&lang=en-us\"},{\"DateTime\":\"2020-02-29T23:00:00+02:00\",\"EpochDateTime\":1583010000,\"WeatherIcon\":7,\"IconPhrase\":\"Cloudy\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":13.6,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":10.1,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":12,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":10.4,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":22.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":190,\"Localized\":\"S\",\"English\":\"S\"}},\"WindGust\":{\"Speed\":{\"Value\":38.9,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":81,\"Visibility\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":0,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":49,\"RainProbability\":49,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":93,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=1&hbhhour=23&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=1&hbhhour=23&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-01T00:00:00+02:00\",\"EpochDateTime\":1583013600,\"WeatherIcon\":38,\"IconPhrase\":\"Mostly cloudy\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":13.3,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":9.7,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":11.3,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":9.3,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":22.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":197,\"Localized\":\"SSW\",\"English\":\"SSW\"}},\"WindGust\":{\"Speed\":{\"Value\":42.6,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":77,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":427,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":39,\"RainProbability\":39,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":82,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=0&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=0&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-01T01:00:00+02:00\",\"EpochDateTime\":1583017200,\"WeatherIcon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":12.9,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":9.2,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":10.6,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":8.3,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":22.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":234,\"Localized\":\"SW\",\"English\":\"SW\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":73,\"Visibility\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":762,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":20,\"RainProbability\":20,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":71,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=1&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=1&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-01T02:00:00+02:00\",\"EpochDateTime\":1583020800,\"WeatherIcon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":12,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":7.8,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":10.2,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":8.4,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":24.1,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":265,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":48.2,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":79,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":9144,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":20,\"RainProbability\":20,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":60,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=2&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=2&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-01T03:00:00+02:00\",\"EpochDateTime\":1583024400,\"WeatherIcon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":12.7,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":8.6,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":10.9,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":9.1,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":24.1,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":263,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":79,\"Visibility\":{\"Value\":8,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":1951,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":20,\"RainProbability\":20,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":70,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=3&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=3&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-01T04:00:00+02:00\",\"EpochDateTime\":1583028000,\"WeatherIcon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":12.9,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":9,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":11.1,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":9.3,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":24.1,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":262,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":78,\"Visibility\":{\"Value\":8,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":1951,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":20,\"RainProbability\":20,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":70,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=4&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=4&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-01T05:00:00+02:00\",\"EpochDateTime\":1583031600,\"WeatherIcon\":38,\"IconPhrase\":\"Mostly cloudy\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":13,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":9.4,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":11.5,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":10,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":22.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":262,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":44.4,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":82,\"Visibility\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":1951,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":20,\"RainProbability\":20,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":75,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=5&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=5&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-01T06:00:00+02:00\",\"EpochDateTime\":1583035200,\"WeatherIcon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":13.2,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":9.6,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":11.7,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":10.2,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":22.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":263,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":42.6,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":82,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":1951,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":18,\"RainProbability\":18,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":68,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=6&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=6&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-01T07:00:00+02:00\",\"EpochDateTime\":1583038800,\"WeatherIcon\":4,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":true,\"Temperature\":{\"Value\":13.2,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":10.2,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":11.7,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":10.2,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":20.4,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":264,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":40.7,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":82,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":1951,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":14,\"RainProbability\":14,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":61,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=7&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/hourly-weather-forecast\\/212559?day=2&hbhhour=7&unit=c&lang=en-us\"}]\n";
        private final String currConditionsJSONExample = "[{\"LocalObservationDateTime\":\"2020-02-29T19:45:00+02:00\",\"EpochTime\":1582998300,\"WeatherText\":\"Clear\",\"WeatherIcon\":33,\"HasPrecipitation\":false,\"PrecipitationType\":null,\"IsDayTime\":false,\"Temperature\":{\"Metric\":{\"Value\":16.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":61,\"Unit\":\"F\",\"UnitType\":18}},\"RealFeelTemperature\":{\"Metric\":{\"Value\":13.8,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":57,\"Unit\":\"F\",\"UnitType\":18}},\"RealFeelTemperatureShade\":{\"Metric\":{\"Value\":13.8,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":57,\"Unit\":\"F\",\"UnitType\":18}},\"RelativeHumidity\":67,\"DewPoint\":{\"Metric\":{\"Value\":10,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":50,\"Unit\":\"F\",\"UnitType\":18}},\"Wind\":{\"Direction\":{\"Degrees\":180,\"Localized\":\"S\",\"English\":\"S\"},\"Speed\":{\"Metric\":{\"Value\":18.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Imperial\":{\"Value\":11.5,\"Unit\":\"mi\\/h\",\"UnitType\":9}}},\"WindGust\":{\"Speed\":{\"Metric\":{\"Value\":18.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Imperial\":{\"Value\":11.5,\"Unit\":\"mi\\/h\",\"UnitType\":9}}},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"Visibility\":{\"Metric\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Imperial\":{\"Value\":10,\"Unit\":\"mi\",\"UnitType\":2}},\"ObstructionsToVisibility\":\"\",\"CloudCover\":0,\"Ceiling\":{\"Metric\":{\"Value\":0,\"Unit\":\"m\",\"UnitType\":5},\"Imperial\":{\"Value\":0,\"Unit\":\"ft\",\"UnitType\":0}},\"Pressure\":{\"Metric\":{\"Value\":1013,\"Unit\":\"mb\",\"UnitType\":14},\"Imperial\":{\"Value\":29.91,\"Unit\":\"inHg\",\"UnitType\":12}},\"PressureTendency\":{\"LocalizedText\":\"Falling\",\"Code\":\"F\"},\"Past24HourTemperatureDeparture\":{\"Metric\":{\"Value\":-1.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":-2,\"Unit\":\"F\",\"UnitType\":18}},\"ApparentTemperature\":{\"Metric\":{\"Value\":18.3,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":65,\"Unit\":\"F\",\"UnitType\":18}},\"WindChillTemperature\":{\"Metric\":{\"Value\":16.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":61,\"Unit\":\"F\",\"UnitType\":18}},\"WetBulbTemperature\":{\"Metric\":{\"Value\":12.8,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":55,\"Unit\":\"F\",\"UnitType\":18}},\"Precip1hr\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"PrecipitationSummary\":{\"Precipitation\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"PastHour\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past3Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past6Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past9Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past12Hours\":{\"Metric\":{\"Value\":0.8,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0.03,\"Unit\":\"in\",\"UnitType\":1}},\"Past18Hours\":{\"Metric\":{\"Value\":0.8,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0.03,\"Unit\":\"in\",\"UnitType\":1}},\"Past24Hours\":{\"Metric\":{\"Value\":0.8,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0.03,\"Unit\":\"in\",\"UnitType\":1}}},\"TemperatureSummary\":{\"Past6HourRange\":{\"Minimum\":{\"Metric\":{\"Value\":16.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":61,\"Unit\":\"F\",\"UnitType\":18}},\"Maximum\":{\"Metric\":{\"Value\":18.9,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":66,\"Unit\":\"F\",\"UnitType\":18}}},\"Past12HourRange\":{\"Minimum\":{\"Metric\":{\"Value\":12.7,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":55,\"Unit\":\"F\",\"UnitType\":18}},\"Maximum\":{\"Metric\":{\"Value\":18.9,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":66,\"Unit\":\"F\",\"UnitType\":18}}},\"Past24HourRange\":{\"Minimum\":{\"Metric\":{\"Value\":11.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":52,\"Unit\":\"F\",\"UnitType\":18}},\"Maximum\":{\"Metric\":{\"Value\":18.9,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":66,\"Unit\":\"F\",\"UnitType\":18}}}},\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/current-weather\\/212559?lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/current-weather\\/212559?lang=en-us\"}]\n";
        private final String daily5DaysJSONExample = "{\"Headline\":{\"EffectiveDate\":\"2020-02-29T19:00:00+02:00\",\"EffectiveEpochDate\":1582995600,\"Severity\":4,\"Text\":\"Expect showers Saturday evening\",\"Category\":\"rain\",\"EndDate\":\"2020-03-01T01:00:00+02:00\",\"EndEpochDate\":1583017200,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/extended-weather-forecast\\/212559?unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?unit=c&lang=en-us\"},\"DailyForecasts\":[{\"Date\":\"2020-02-29T07:00:00+02:00\",\"EpochDate\":1582952400,\"Sun\":{\"Rise\":\"2020-02-29T06:08:00+02:00\",\"EpochRise\":1582949280,\"Set\":\"2020-02-29T17:38:00+02:00\",\"EpochSet\":1582990680},\"Moon\":{\"Rise\":\"2020-02-29T09:20:00+02:00\",\"EpochRise\":1582960800,\"Set\":\"2020-02-29T22:47:00+02:00\",\"EpochSet\":1583009220,\"Phase\":\"WaxingCrescent\",\"Age\":6},\"Temperature\":{\"Minimum\":{\"Value\":12,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":18.9,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":7.8,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":18,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":7.8,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":16.7,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":6.1,\"DegreeDaySummary\":{\"Heating\":{\"Value\":3,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":37,\"Category\":\"Good\",\"CategoryValue\":1,\"Type\":\"Ozone\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":5,\"Category\":\"Moderate\",\"CategoryValue\":2}],\"Day\":{\"Icon\":14,\"IconPhrase\":\"Partly sunny w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"Not as warm\",\"LongPhrase\":\"A passing shower this morning, then becoming breezy; not as warm\",\"PrecipitationProbability\":56,\"ThunderstormProbability\":20,\"RainProbability\":56,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":24.1,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":230,\"Localized\":\"SW\",\"English\":\"SW\"}},\"WindGust\":{\"Speed\":{\"Value\":63,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":220,\"Localized\":\"SW\",\"English\":\"SW\"}},\"TotalLiquid\":{\"Value\":0.8,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0.8,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0.5,\"HoursOfRain\":0.5,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":51},\"Night\":{\"Icon\":39,\"IconPhrase\":\"Partly cloudy w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Moderate\",\"ShortPhrase\":\"A couple of evening showers\",\"LongPhrase\":\"Spotty evening showers; otherwise, partly cloudy\",\"PrecipitationProbability\":86,\"ThunderstormProbability\":20,\"RainProbability\":86,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":20.4,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":256,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":48.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":265,\"Localized\":\"W\",\"English\":\"W\"}},\"TotalLiquid\":{\"Value\":6.3,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":6.3,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":2,\"HoursOfRain\":2,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":63},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?day=1&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?day=1&unit=c&lang=en-us\"},{\"Date\":\"2020-03-01T07:00:00+02:00\",\"EpochDate\":1583038800,\"Sun\":{\"Rise\":\"2020-03-01T06:07:00+02:00\",\"EpochRise\":1583035620,\"Set\":\"2020-03-01T17:39:00+02:00\",\"EpochSet\":1583077140},\"Moon\":{\"Rise\":\"2020-03-01T09:53:00+02:00\",\"EpochRise\":1583049180,\"Set\":\"2020-03-01T23:44:00+02:00\",\"EpochSet\":1583099040,\"Phase\":\"WaxingCrescent\",\"Age\":7},\"Temperature\":{\"Minimum\":{\"Value\":10.1,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":17.7,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":9.5,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":16,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":9.5,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":15.5,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":4.8,\"DegreeDaySummary\":{\"Heating\":{\"Value\":4,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":38,\"Category\":\"Good\",\"CategoryValue\":1,\"Type\":\"Ozone\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":2,\"Category\":\"Low\",\"CategoryValue\":1}],\"Day\":{\"Icon\":14,\"IconPhrase\":\"Partly sunny w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"Clouds and sun with a shower\",\"LongPhrase\":\"Times of clouds and sun with a stray shower\",\"PrecipitationProbability\":40,\"ThunderstormProbability\":20,\"RainProbability\":40,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":22.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":283,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"WindGust\":{\"Speed\":{\"Value\":44.4,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":289,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"TotalLiquid\":{\"Value\":0.5,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0.5,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":1,\"HoursOfRain\":1,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":63},\"Night\":{\"Icon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Partly cloudy\",\"LongPhrase\":\"Partly cloudy\",\"PrecipitationProbability\":3,\"ThunderstormProbability\":0,\"RainProbability\":3,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":7.4,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":180,\"Localized\":\"S\",\"English\":\"S\"}},\"WindGust\":{\"Speed\":{\"Value\":25.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":256,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":59},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?day=2&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?day=2&unit=c&lang=en-us\"},{\"Date\":\"2020-03-02T07:00:00+02:00\",\"EpochDate\":1583125200,\"Sun\":{\"Rise\":\"2020-03-02T06:06:00+02:00\",\"EpochRise\":1583121960,\"Set\":\"2020-03-02T17:39:00+02:00\",\"EpochSet\":1583163540},\"Moon\":{\"Rise\":\"2020-03-02T10:30:00+02:00\",\"EpochRise\":1583137800,\"Set\":\"2020-03-03T00:41:00+02:00\",\"EpochSet\":1583188860,\"Phase\":\"First\",\"Age\":8},\"Temperature\":{\"Minimum\":{\"Value\":8.7,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":18.4,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":9,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":18,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":9,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":17.1,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":3.3,\"DegreeDaySummary\":{\"Heating\":{\"Value\":4,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":42,\"Category\":\"Good\",\"CategoryValue\":1,\"Type\":\"Ozone\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":2,\"Category\":\"Low\",\"CategoryValue\":1}],\"Day\":{\"Icon\":6,\"IconPhrase\":\"Mostly cloudy\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Sun and areas of low clouds\",\"LongPhrase\":\"Sun and areas of low clouds\",\"PrecipitationProbability\":1,\"ThunderstormProbability\":0,\"RainProbability\":1,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":13,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":248,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":25.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":269,\"Localized\":\"W\",\"English\":\"W\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":78},\"Night\":{\"Icon\":33,\"IconPhrase\":\"Clear\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Clear\",\"LongPhrase\":\"Clear\",\"PrecipitationProbability\":0,\"ThunderstormProbability\":0,\"RainProbability\":0,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":5.6,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":90,\"Localized\":\"E\",\"English\":\"E\"}},\"WindGust\":{\"Speed\":{\"Value\":16.7,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":98,\"Localized\":\"E\",\"English\":\"E\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":6},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?day=3&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?day=3&unit=c&lang=en-us\"},{\"Date\":\"2020-03-03T07:00:00+02:00\",\"EpochDate\":1583211600,\"Sun\":{\"Rise\":\"2020-03-03T06:05:00+02:00\",\"EpochRise\":1583208300,\"Set\":\"2020-03-03T17:40:00+02:00\",\"EpochSet\":1583250000},\"Moon\":{\"Rise\":\"2020-03-03T11:13:00+02:00\",\"EpochRise\":1583226780,\"Set\":\"2020-03-04T01:40:00+02:00\",\"EpochSet\":1583278800,\"Phase\":\"WaxingGibbous\",\"Age\":9},\"Temperature\":{\"Minimum\":{\"Value\":11,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":22.2,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":11.2,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":24.1,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":11.2,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":20.7,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":11.2,\"DegreeDaySummary\":{\"Heating\":{\"Value\":1,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":60,\"Category\":\"Moderate\",\"CategoryValue\":2,\"Type\":\"Particle Pollution\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":5,\"Category\":\"Moderate\",\"CategoryValue\":2}],\"Day\":{\"Icon\":1,\"IconPhrase\":\"Sunny\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Sunny and warmer\",\"LongPhrase\":\"Warmer with brilliant sunshine\",\"PrecipitationProbability\":0,\"ThunderstormProbability\":0,\"RainProbability\":0,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":11.1,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":347,\"Localized\":\"NNW\",\"English\":\"NNW\"}},\"WindGust\":{\"Speed\":{\"Value\":31.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":319,\"Localized\":\"NW\",\"English\":\"NW\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":7},\"Night\":{\"Icon\":33,\"IconPhrase\":\"Clear\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Clear\",\"LongPhrase\":\"Clear\",\"PrecipitationProbability\":0,\"ThunderstormProbability\":0,\"RainProbability\":0,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":7.4,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":134,\"Localized\":\"SE\",\"English\":\"SE\"}},\"WindGust\":{\"Speed\":{\"Value\":20.4,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":92,\"Localized\":\"E\",\"English\":\"E\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":0},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?day=4&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?day=4&unit=c&lang=en-us\"},{\"Date\":\"2020-03-04T07:00:00+02:00\",\"EpochDate\":1583298000,\"Sun\":{\"Rise\":\"2020-03-04T06:04:00+02:00\",\"EpochRise\":1583294640,\"Set\":\"2020-03-04T17:41:00+02:00\",\"EpochSet\":1583336460},\"Moon\":{\"Rise\":\"2020-03-04T12:02:00+02:00\",\"EpochRise\":1583316120,\"Set\":\"2020-03-05T02:38:00+02:00\",\"EpochSet\":1583368680,\"Phase\":\"WaxingGibbous\",\"Age\":10},\"Temperature\":{\"Minimum\":{\"Value\":12.2,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":26.3,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":12.5,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":27.9,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":12.5,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":25,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":11.6,\"DegreeDaySummary\":{\"Heating\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":1,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":88,\"Category\":\"Moderate\",\"CategoryValue\":2,\"Type\":\"Particle Pollution\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":5,\"Category\":\"Moderate\",\"CategoryValue\":2}],\"Day\":{\"Icon\":1,\"IconPhrase\":\"Sunny\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Sunny and very warm\",\"LongPhrase\":\"Very warm with plenty of sunshine\",\"PrecipitationProbability\":0,\"ThunderstormProbability\":0,\"RainProbability\":0,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":9.3,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":83,\"Localized\":\"E\",\"English\":\"E\"}},\"WindGust\":{\"Speed\":{\"Value\":27.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":108,\"Localized\":\"ESE\",\"English\":\"ESE\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":0},\"Night\":{\"Icon\":34,\"IconPhrase\":\"Mostly clear\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Mainly clear\",\"LongPhrase\":\"Mainly clear\",\"PrecipitationProbability\":0,\"ThunderstormProbability\":0,\"RainProbability\":0,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":5.6,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":131,\"Localized\":\"SE\",\"English\":\"SE\"}},\"WindGust\":{\"Speed\":{\"Value\":20.4,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":170,\"Localized\":\"S\",\"English\":\"S\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":13},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?day=5&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/nehalim\\/212559\\/daily-weather-forecast\\/212559?day=5&unit=c&lang=en-us\"}]}";
    }

    private void getForcastFromDB()
    {
        int attempts = 0;
        forecast = null;
        while (forecast == null && attempts < NUM_OF_ATTEMPTS) {
            forecast = weatherDataController.getForecastDataByLocation(mainActivity,
                    locationInfo.getCountry(), locationInfo.getCity());
            attempts++;
            if(attempts == 5){
                //TODO Error to user? or what actions should we do. maybe log num of attempts.
            }
        }
    }

    private void getSunTimeFromDB()
    {
        int attempts = 0;
        sunriseSunset = null;
        while (sunriseSunset == null && attempts < NUM_OF_ATTEMPTS) {
            sunriseSunset = weatherDataController.getSunTimesDataByLocation(mainActivity,
                    locationInfo.getCountry(), locationInfo.getCity());
            attempts++;
            if(attempts == 5){
                //TODO Error to user? or what actions should we do. maybe log num of attempts.
            }
        }
    }
}
