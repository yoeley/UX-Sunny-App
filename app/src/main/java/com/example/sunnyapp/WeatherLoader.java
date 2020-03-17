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

        private String obtainLocationKey(OkHttpClient client) throws JSONException, IOException {
            Request keyRequest = new Request.Builder()
                    .url(String.format(keyRequestBodyFormat, apiKey, Double.toString(location.getLatitude()), Double.toString(location.getLongitude())))
                    .build();

            Response keyResponse = client.newCall(keyRequest).execute();
            String keyResult = keyResponse.body().string();

            JSONObject keyResultJSON = new JSONObject(keyResult);
            return keyResultJSON.getString("Key");
        }

        private JSONArray obtainWeatherForecastJSON(OkHttpClient client, String locationKey) throws IOException, JSONException {
            Request weatherRrequest = new Request.Builder()
                    .url(String.format(weatherRequestBodyFormat, locationKey, apiKey))
                    .build();

            Response weatherResponse = client.newCall(weatherRrequest).execute();
            String weatherResult = weatherResponse.body().string();

            return new JSONArray(weatherResult);
        }

        private JSONArray obtainCurrConditions(OkHttpClient client, String locationKey) throws IOException, JSONException {
            Request currConditionsRrequest = new Request.Builder()
                    .url(String.format(currConditionsRequestBodyFormat, locationKey, apiKey))
                    .build();

            Response currConditionsResponse = client.newCall(currConditionsRrequest).execute();
            String currConditionsResult = currConditionsResponse.body().string();

            return new JSONArray(currConditionsResult);
        }

        private JSONObject obtainDaily5DayForecast(OkHttpClient client, String locationKey) throws IOException, JSONException {
            Request daily5DaysRrequest = new Request.Builder()
                    .url(String.format(daily5DaysRequestBodyFormat, locationKey, apiKey))
                    .build();

            Response daily5DaysResponse = client.newCall(daily5DaysRrequest).execute();
            String daily5DaysResult = daily5DaysResponse.body().string();

            return new JSONObject(daily5DaysResult);
        }

        private Boolean checkLocationUpToDate(Double formerLatitude, Double formerLongitude) {
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();

            float results[] = new float[1];
            Location.distanceBetween(formerLatitude, formerLongitude, latitude, longitude, results);

            return !(results[0] > 1000);
        }

        private Boolean sunriseSunsetUpToDate(Date currDateTime) {
            long formerTime = DateStringConverter.stringToDate(sunriseSunset.getDateTime()).getTime();
            long currTime = currDateTime.getTime();

            // if current time and time when the sunriseSunset was taken don't have the same day, return false
            if ((formerTime - (formerTime % ONE_DAY)) - (currTime - (currTime % ONE_DAY)) != 0) {
                return false;
            }
            return true;
        }

        private Boolean checkSunriseSunsetUpToDate(Date currDateTime) {
            if (sunriseSunset == null ||
                    !checkLocationUpToDate(sunriseSunset.getLatitude(), sunriseSunset.getLongitude()) ||
                    !sunriseSunsetUpToDate(currDateTime)) {
                return false;
            }
            return true;
        }

        private Boolean forecastUpToDate(Date currDateTime) {
            long formerTime = DateStringConverter.stringToDate(forecast.getDateTime()).getTime();
            long currTime = currDateTime.getTime();

            // if current time and time when the forecast was taken don't have the same hour in the day, return false
            if ((formerTime - (formerTime % ONE_HOUR)) - (currTime - (currTime % ONE_HOUR)) != 0) {
                return false;
            }
            return true;
        }

        private Boolean checkForecastUpToDate(Date currDateTime) {
            if (forecast == null ||
                    !checkLocationUpToDate(forecast.getLatitude(), forecast.getLongitude()) ||
                    !forecastUpToDate(currDateTime)) {
                return false;
            }
            return true;
        }

        private void updateLocationInfo(OkHttpClient client) throws IOException, JSONException {
            Request keyRequest = new Request.Builder()
                    .url(String.format(keyRequestBodyFormat, apiKey, Double.toString(location.getLatitude()), Double.toString(location.getLongitude())))
                    .build();

            Response keyResponse = client.newCall(keyRequest).execute();
            String keyResult = keyResponse.body().string();

            JSONObject keyResultJSON = new JSONObject(keyResult);

            locationInfo = new LocationInfo(keyResultJSON.getString("Key"), keyResultJSON.getJSONObject("Country").getString("ID"), keyResultJSON.getString("EnglishName"));
        }

        private void obtainForecastAndSunriseExample() throws JSONException {
            Date currDateTime = DateStringConverter.stringToDate("2020-03-17T11:50:00+02:00");

            String locationKey = locationKeyExample;

            JSONArray forecastJSON = new JSONArray(forecastJSONExample);
            JSONArray currConditionsJSON = new JSONArray(currConditionsJSONExample);
            JSONObject daily5DaysJSON = new JSONObject(daily5DaysJSONExample);

            forecast = ForecastGenerator.generate(location, locationKey, currDateTime, forecastJSON, currConditionsJSON);
            sunriseSunset = SunriseSunsetGenerator.generate(location, locationKey, currDateTime, daily5DaysJSON);
            weatherDataController.saveForecastDataByLocation(mainActivity, forecast, locationInfo.getCountry(), locationInfo.getCity());
            weatherDataController.saveSunTimesDataByLocation(mainActivity, sunriseSunset, locationInfo.getCountry(), locationInfo.getCity());
        }

        private Boolean uploadDataFromFirebase(OkHttpClient client, Date currDateTime) throws IOException, JSONException {
            if (!checkSunriseSunsetUpToDate(currDateTime)) {
                updateLocationInfo(client);
                getForcastFromDB();
                getSunTimeFromDB();
            } else if (!checkForecastUpToDate(currDateTime)) {
                updateLocationInfo(client);
                getForcastFromDB();
            } else {
                // everything already up to date - no upload done
                return false;
            }
            return true;
        }

        private void uploadSunriseFromAccuWeather(OkHttpClient client, Date currDateTime, String locationKey) throws IOException, JSONException {
            JSONObject daily5DaysJSON = obtainDaily5DayForecast(client, locationKey);
            sunriseSunset = SunriseSunsetGenerator.generate(location, locationKey, currDateTime, daily5DaysJSON);
            weatherDataController.saveSunTimesDataByLocation(mainActivity, sunriseSunset, locationInfo.getCountry(), locationInfo.getCity());
        }

        private void uploadForecastFromAccuWeather(OkHttpClient client, Date currDateTime, String locationKey) throws IOException, JSONException {
            JSONArray forecastJSON = obtainWeatherForecastJSON(client, locationKey);
            JSONArray currConditionsJSON = obtainCurrConditions(client, locationKey);
            forecast = ForecastGenerator.generate(location, locationKey, currDateTime, forecastJSON, currConditionsJSON);
            weatherDataController.saveForecastDataByLocation(mainActivity, forecast, locationInfo.getCountry(), locationInfo.getCity());
        }

        private void uploadDataFromAccuWeather(OkHttpClient client, Date currDateTime) throws IOException, JSONException {
            String locationKey = locationInfo.getLocationKey();

            if (!checkSunriseSunsetUpToDate(currDateTime)) {
                uploadSunriseFromAccuWeather(client, currDateTime, locationKey);
                uploadForecastFromAccuWeather(client, currDateTime, locationKey);
            } else if (!checkForecastUpToDate(currDateTime)) {
                uploadForecastFromAccuWeather(client, currDateTime, locationKey);
            } else {
                // everything from firebase was already up to date - quit function
                return;
            }
        }

        private void obtainForecastAndSunrise() throws IOException, JSONException {
            OkHttpClient client = new OkHttpClient();
            Date currDateTime = Calendar.getInstance().getTime();

            if (!uploadDataFromFirebase(client, currDateTime)) {
                // everything already up to date - quit function
                return;
            }
            uploadDataFromAccuWeather(client, currDateTime);
        }

        @Override
        protected Integer doInBackground(Integer... callers) {

            try {
//                obtainForecastAndSunrise();
                obtainForecastAndSunriseExample();
            } catch (IOException | JSONException e) {
                // TODO: go to error screen in this case
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Integer result) {
            mainActivity.goToDisplayWeatherActivity();
        }

        private final String locationKeyExample = "213225";
        private final String forecastJSONExample = "[{\"DateTime\":\"2020-03-17T12:00:00+02:00\",\"EpochDateTime\":1584439200,\"WeatherIcon\":4,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":true,\"Temperature\":{\"Value\":13.6,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":12,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":11.6,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":9.8,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":29.6,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":193,\"Localized\":\"SSW\",\"English\":\"SSW\"}},\"WindGust\":{\"Speed\":{\"Value\":42.6,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":78,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":3322,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":5,\"UVIndexText\":\"Moderate\",\"PrecipitationProbability\":11,\"RainProbability\":11,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":53,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=12&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=12&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T13:00:00+02:00\",\"EpochDateTime\":1584442800,\"WeatherIcon\":13,\"IconPhrase\":\"Mostly cloudy w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"IsDaylight\":true,\"Temperature\":{\"Value\":13.2,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":9,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":11.3,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":9.4,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":31.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":199,\"Localized\":\"SSW\",\"English\":\"SSW\"}},\"WindGust\":{\"Speed\":{\"Value\":44.4,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":77,\"Visibility\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":4999,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":4,\"UVIndexText\":\"Moderate\",\"PrecipitationProbability\":64,\"RainProbability\":64,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":1,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":1,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":70,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=13&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=13&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T14:00:00+02:00\",\"EpochDateTime\":1584446400,\"WeatherIcon\":13,\"IconPhrase\":\"Mostly cloudy w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Moderate\",\"IsDaylight\":true,\"Temperature\":{\"Value\":12.6,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":6.9,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":10.9,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":9,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":33.3,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":210,\"Localized\":\"SSW\",\"English\":\"SSW\"}},\"WindGust\":{\"Speed\":{\"Value\":44.4,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":79,\"Visibility\":{\"Value\":8,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":0,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":2,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":69,\"RainProbability\":69,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":2,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":2,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":67,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=14&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=14&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T15:00:00+02:00\",\"EpochDateTime\":1584450000,\"WeatherIcon\":4,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":true,\"Temperature\":{\"Value\":11.6,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":6.9,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":10,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":8.2,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":35.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":232,\"Localized\":\"SW\",\"English\":\"SW\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":79,\"Visibility\":{\"Value\":8,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":1128,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":1,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":49,\"RainProbability\":49,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":72,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=15&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=15&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T16:00:00+02:00\",\"EpochDateTime\":1584453600,\"WeatherIcon\":12,\"IconPhrase\":\"Showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"IsDaylight\":true,\"Temperature\":{\"Value\":10.3,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":2.9,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":8.7,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":6.9,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":35.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":256,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":80,\"Visibility\":{\"Value\":8,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":1707,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":1,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":51,\"RainProbability\":51,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":1,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":1,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":76,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=16&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=16&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T17:00:00+02:00\",\"EpochDateTime\":1584457200,\"WeatherIcon\":6,\"IconPhrase\":\"Mostly cloudy\",\"HasPrecipitation\":false,\"IsDaylight\":true,\"Temperature\":{\"Value\":8.9,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":2.2,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":7.4,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":5.6,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":37,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":270,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":80,\"Visibility\":{\"Value\":11.3,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":1920,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":47,\"RainProbability\":47,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":81,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=17&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=17&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T18:00:00+02:00\",\"EpochDateTime\":1584460800,\"WeatherIcon\":38,\"IconPhrase\":\"Mostly cloudy\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":8.5,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":1.5,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":7,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":5,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":37,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":264,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":48.2,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":79,\"Visibility\":{\"Value\":11.3,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":1920,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":49,\"RainProbability\":49,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":81,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=18&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=18&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T19:00:00+02:00\",\"EpochDateTime\":1584464400,\"WeatherIcon\":12,\"IconPhrase\":\"Showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"IsDaylight\":false,\"Temperature\":{\"Value\":8.1,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":-0.7,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":6.5,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":4.4,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":37,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":253,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":78,\"Visibility\":{\"Value\":11.3,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":5547,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":55,\"RainProbability\":55,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0.7,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0.7,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":80,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=19&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=19&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T20:00:00+02:00\",\"EpochDateTime\":1584468000,\"WeatherIcon\":12,\"IconPhrase\":\"Showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"IsDaylight\":false,\"Temperature\":{\"Value\":7.6,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":-1.3,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":6.1,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":3.9,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":37,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":247,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":78,\"Visibility\":{\"Value\":11.3,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":5517,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":55,\"RainProbability\":55,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0.7,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0.7,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":80,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=20&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=20&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T21:00:00+02:00\",\"EpochDateTime\":1584471600,\"WeatherIcon\":38,\"IconPhrase\":\"Mostly cloudy\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":7.8,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":0.4,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":6.1,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":3.8,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":38.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":256,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":48.2,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":76,\"Visibility\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":5517,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":49,\"RainProbability\":49,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":79,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=21&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=21&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T22:00:00+02:00\",\"EpochDateTime\":1584475200,\"WeatherIcon\":38,\"IconPhrase\":\"Mostly cloudy\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":7.7,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":-0.1,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":5.9,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":3.6,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":42.6,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":271,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":51.9,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":75,\"Visibility\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":5517,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":34,\"RainProbability\":34,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":77,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=22&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=22&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-17T23:00:00+02:00\",\"EpochDateTime\":1584478800,\"WeatherIcon\":38,\"IconPhrase\":\"Mostly cloudy\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":7.4,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":-0.7,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":5.7,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":3.4,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":44.4,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":277,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":53.7,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":76,\"Visibility\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":9144,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":34,\"RainProbability\":34,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":76,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=23&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=23&unit=c&lang=en-us\"}]";
        private final String currConditionsJSONExample = "[{\"LocalObservationDateTime\":\"2020-03-17T11:50:00+02:00\",\"EpochTime\":1584438600,\"WeatherText\":\"Partly sunny\",\"WeatherIcon\":3,\"HasPrecipitation\":false,\"PrecipitationType\":null,\"IsDayTime\":true,\"Temperature\":{\"Metric\":{\"Value\":13.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":56,\"Unit\":\"F\",\"UnitType\":18}},\"RealFeelTemperature\":{\"Metric\":{\"Value\":10.8,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":51,\"Unit\":\"F\",\"UnitType\":18}},\"RealFeelTemperatureShade\":{\"Metric\":{\"Value\":8.4,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":47,\"Unit\":\"F\",\"UnitType\":18}},\"RelativeHumidity\":73,\"DewPoint\":{\"Metric\":{\"Value\":8.4,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":47,\"Unit\":\"F\",\"UnitType\":18}},\"Wind\":{\"Direction\":{\"Degrees\":203,\"Localized\":\"SSW\",\"English\":\"SSW\"},\"Speed\":{\"Metric\":{\"Value\":31.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Imperial\":{\"Value\":19.8,\"Unit\":\"mi\\/h\",\"UnitType\":9}}},\"WindGust\":{\"Speed\":{\"Metric\":{\"Value\":46.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Imperial\":{\"Value\":29.1,\"Unit\":\"mi\\/h\",\"UnitType\":9}}},\"UVIndex\":4,\"UVIndexText\":\"Moderate\",\"Visibility\":{\"Metric\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Imperial\":{\"Value\":10,\"Unit\":\"mi\",\"UnitType\":2}},\"ObstructionsToVisibility\":\"\",\"CloudCover\":31,\"Ceiling\":{\"Metric\":{\"Value\":1646,\"Unit\":\"m\",\"UnitType\":5},\"Imperial\":{\"Value\":5400,\"Unit\":\"ft\",\"UnitType\":0}},\"Pressure\":{\"Metric\":{\"Value\":1013,\"Unit\":\"mb\",\"UnitType\":14},\"Imperial\":{\"Value\":29.91,\"Unit\":\"inHg\",\"UnitType\":12}},\"PressureTendency\":{\"LocalizedText\":\"Steady\",\"Code\":\"S\"},\"Past24HourTemperatureDeparture\":{\"Metric\":{\"Value\":-4.5,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":-8,\"Unit\":\"F\",\"UnitType\":18}},\"ApparentTemperature\":{\"Metric\":{\"Value\":16.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":61,\"Unit\":\"F\",\"UnitType\":18}},\"WindChillTemperature\":{\"Metric\":{\"Value\":13.3,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":56,\"Unit\":\"F\",\"UnitType\":18}},\"WetBulbTemperature\":{\"Metric\":{\"Value\":10.7,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":51,\"Unit\":\"F\",\"UnitType\":18}},\"Precip1hr\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"PrecipitationSummary\":{\"Precipitation\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"PastHour\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past3Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past6Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past9Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past12Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past18Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past24Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}}},\"TemperatureSummary\":{\"Past6HourRange\":{\"Minimum\":{\"Metric\":{\"Value\":10.4,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":51,\"Unit\":\"F\",\"UnitType\":18}},\"Maximum\":{\"Metric\":{\"Value\":13.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":56,\"Unit\":\"F\",\"UnitType\":18}}},\"Past12HourRange\":{\"Minimum\":{\"Metric\":{\"Value\":9.5,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":49,\"Unit\":\"F\",\"UnitType\":18}},\"Maximum\":{\"Metric\":{\"Value\":13.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":56,\"Unit\":\"F\",\"UnitType\":18}}},\"Past24HourRange\":{\"Minimum\":{\"Metric\":{\"Value\":9.5,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":49,\"Unit\":\"F\",\"UnitType\":18}},\"Maximum\":{\"Metric\":{\"Value\":18.4,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":65,\"Unit\":\"F\",\"UnitType\":18}}}},\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/current-weather\\/213225?lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/current-weather\\/213225?lang=en-us\"}]";
        private final String daily5DaysJSONExample = "{\"Headline\":{\"EffectiveDate\":\"2020-03-17T13:00:00+02:00\",\"EffectiveEpochDate\":1584442800,\"Severity\":3,\"Text\":\"Expect rainy weather Tuesday afternoon through late Tuesday night\",\"Category\":\"rain\",\"EndDate\":\"2020-03-18T07:00:00+02:00\",\"EndEpochDate\":1584507600,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/extended-weather-forecast\\/213225?unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?unit=c&lang=en-us\"},\"DailyForecasts\":[{\"Date\":\"2020-03-17T07:00:00+02:00\",\"EpochDate\":1584421200,\"Sun\":{\"Rise\":\"2020-03-17T05:46:00+02:00\",\"EpochRise\":1584416760,\"Set\":\"2020-03-17T17:49:00+02:00\",\"EpochSet\":1584460140},\"Moon\":{\"Rise\":\"2020-03-17T01:18:00+02:00\",\"EpochRise\":1584400680,\"Set\":\"2020-03-17T11:35:00+02:00\",\"EpochSet\":1584437700,\"Phase\":\"WaningCrescent\",\"Age\":23},\"Temperature\":{\"Minimum\":{\"Value\":7,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":13.6,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":-2.4,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":12,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":-2.4,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":10.9,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":6.6,\"DegreeDaySummary\":{\"Heating\":{\"Value\":8,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":89,\"Category\":\"Moderate\",\"CategoryValue\":2,\"Type\":\"Particle Pollution\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":5,\"Category\":\"Moderate\",\"CategoryValue\":2}],\"Day\":{\"Icon\":13,\"IconPhrase\":\"Mostly cloudy w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"Increasingly windy\",\"LongPhrase\":\"Becoming windier and cooler with increasing clouds; brief showers this afternoon\",\"PrecipitationProbability\":76,\"ThunderstormProbability\":20,\"RainProbability\":76,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":27.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":256,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":48.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":264,\"Localized\":\"W\",\"English\":\"W\"}},\"TotalLiquid\":{\"Value\":4,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":4,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":2,\"HoursOfRain\":2,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":51},\"Night\":{\"Icon\":18,\"IconPhrase\":\"Rain\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"Very windy; rain and drizzle\",\"LongPhrase\":\"Very windy; a couple of evening showers followed by occasional rain and drizzle late\",\"PrecipitationProbability\":68,\"ThunderstormProbability\":20,\"RainProbability\":68,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":38.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":262,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":53.7,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":269,\"Localized\":\"W\",\"English\":\"W\"}},\"TotalLiquid\":{\"Value\":5.2,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":5.2,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":3,\"HoursOfRain\":3,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":82},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=1&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=1&unit=c&lang=en-us\"},{\"Date\":\"2020-03-18T07:00:00+02:00\",\"EpochDate\":1584507600,\"Sun\":{\"Rise\":\"2020-03-18T05:45:00+02:00\",\"EpochRise\":1584503100,\"Set\":\"2020-03-18T17:50:00+02:00\",\"EpochSet\":1584546600},\"Moon\":{\"Rise\":\"2020-03-18T02:12:00+02:00\",\"EpochRise\":1584490320,\"Set\":\"2020-03-18T12:29:00+02:00\",\"EpochSet\":1584527340,\"Phase\":\"WaningCrescent\",\"Age\":24},\"Temperature\":{\"Minimum\":{\"Value\":7.6,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":9.5,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":2.6,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":4.2,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":2.6,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":3.4,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":4.2,\"DegreeDaySummary\":{\"Heating\":{\"Value\":9,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":43,\"Category\":\"Good\",\"CategoryValue\":1,\"Type\":\"Ozone\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":3,\"Category\":\"Moderate\",\"CategoryValue\":2}],\"Day\":{\"Icon\":32,\"IconPhrase\":\"Windy\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"Strong winds subsiding\",\"LongPhrase\":\"Strong winds gradually subsiding; a morning shower in spots; otherwise, chilly with variable clouds\",\"PrecipitationProbability\":40,\"ThunderstormProbability\":20,\"RainProbability\":40,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":38.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":267,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":51.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":282,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"TotalLiquid\":{\"Value\":0.2,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0.2,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0.5,\"HoursOfRain\":0.5,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":61},\"Night\":{\"Icon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Winds gradually subsiding\",\"LongPhrase\":\"Partly cloudy with winds gradually subsiding\",\"PrecipitationProbability\":25,\"ThunderstormProbability\":0,\"RainProbability\":25,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":25.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":258,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":42.6,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":258,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":52},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=2&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=2&unit=c&lang=en-us\"},{\"Date\":\"2020-03-19T07:00:00+02:00\",\"EpochDate\":1584594000,\"Sun\":{\"Rise\":\"2020-03-19T05:44:00+02:00\",\"EpochRise\":1584589440,\"Set\":\"2020-03-19T17:50:00+02:00\",\"EpochSet\":1584633000},\"Moon\":{\"Rise\":\"2020-03-19T03:01:00+02:00\",\"EpochRise\":1584579660,\"Set\":\"2020-03-19T13:25:00+02:00\",\"EpochSet\":1584617100,\"Phase\":\"WaningCrescent\",\"Age\":25},\"Temperature\":{\"Minimum\":{\"Value\":6.1,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":10.8,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":0.1,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":7.8,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":0.1,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":5,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":5,\"DegreeDaySummary\":{\"Heating\":{\"Value\":10,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":55,\"Category\":\"Moderate\",\"CategoryValue\":2,\"Type\":\"Particle Pollution\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":7,\"Category\":\"High\",\"CategoryValue\":3}],\"Day\":{\"Icon\":4,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"A p.m. shower in places\",\"LongPhrase\":\"Windy and cool with intervals of clouds and sunshine; a shower in spots in the afternoon\",\"PrecipitationProbability\":44,\"ThunderstormProbability\":20,\"RainProbability\":44,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":31.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":254,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":42.6,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":258,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"TotalLiquid\":{\"Value\":0.6,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0.6,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0.5,\"HoursOfRain\":0.5,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":60},\"Night\":{\"Icon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"Partly cloudy, a shower late\",\"LongPhrase\":\"Windy in the evening; partly cloudy with a stray shower late\",\"PrecipitationProbability\":40,\"ThunderstormProbability\":20,\"RainProbability\":40,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":25.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":264,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":42.6,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":267,\"Localized\":\"W\",\"English\":\"W\"}},\"TotalLiquid\":{\"Value\":0.2,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0.2,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0.5,\"HoursOfRain\":0.5,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":63},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=3&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=3&unit=c&lang=en-us\"},{\"Date\":\"2020-03-20T07:00:00+02:00\",\"EpochDate\":1584680400,\"Sun\":{\"Rise\":\"2020-03-20T05:42:00+02:00\",\"EpochRise\":1584675720,\"Set\":\"2020-03-20T17:51:00+02:00\",\"EpochSet\":1584719460},\"Moon\":{\"Rise\":\"2020-03-20T03:43:00+02:00\",\"EpochRise\":1584668580,\"Set\":\"2020-03-20T14:21:00+02:00\",\"EpochSet\":1584706860,\"Phase\":\"WaningCrescent\",\"Age\":26},\"Temperature\":{\"Minimum\":{\"Value\":4.3,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":8.9,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":0.9,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":6.2,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":0.9,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":4.6,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":4,\"DegreeDaySummary\":{\"Heating\":{\"Value\":11,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":35,\"Category\":\"Good\",\"CategoryValue\":1,\"Type\":\"Ozone\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":3,\"Category\":\"Moderate\",\"CategoryValue\":2}],\"Day\":{\"Icon\":6,\"IconPhrase\":\"Mostly cloudy\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Moderate\",\"ShortPhrase\":\"A shower in the morning\",\"LongPhrase\":\"A morning shower; otherwise, mostly cloudy and chilly, becoming breezy in the afternoon\",\"PrecipitationProbability\":62,\"ThunderstormProbability\":20,\"RainProbability\":62,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":22.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":259,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":38.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":266,\"Localized\":\"W\",\"English\":\"W\"}},\"TotalLiquid\":{\"Value\":3.1,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":3.1,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":2,\"HoursOfRain\":2,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":77},\"Night\":{\"Icon\":12,\"IconPhrase\":\"Showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"A brief evening shower or two\",\"LongPhrase\":\"A brief shower or two in the evening; otherwise, mostly cloudy and chilly\",\"PrecipitationProbability\":67,\"ThunderstormProbability\":20,\"RainProbability\":67,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":11.1,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":285,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"WindGust\":{\"Speed\":{\"Value\":33.3,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":267,\"Localized\":\"W\",\"English\":\"W\"}},\"TotalLiquid\":{\"Value\":2.9,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":2.9,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":1.5,\"HoursOfRain\":1.5,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":88},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=4&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=4&unit=c&lang=en-us\"},{\"Date\":\"2020-03-21T07:00:00+02:00\",\"EpochDate\":1584766800,\"Sun\":{\"Rise\":\"2020-03-21T05:41:00+02:00\",\"EpochRise\":1584762060,\"Set\":\"2020-03-21T17:52:00+02:00\",\"EpochSet\":1584805920},\"Moon\":{\"Rise\":\"2020-03-21T04:21:00+02:00\",\"EpochRise\":1584757260,\"Set\":\"2020-03-21T15:17:00+02:00\",\"EpochSet\":1584796620,\"Phase\":\"WaningCrescent\",\"Age\":27},\"Temperature\":{\"Minimum\":{\"Value\":5.6,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":9.9,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":2.4,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":11.8,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":2.4,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":7.5,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":4.9,\"DegreeDaySummary\":{\"Heating\":{\"Value\":10,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":17,\"Category\":\"Good\",\"CategoryValue\":1,\"Type\":\"Ozone\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":6,\"Category\":\"High\",\"CategoryValue\":3}],\"Day\":{\"Icon\":14,\"IconPhrase\":\"Partly sunny w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Moderate\",\"ShortPhrase\":\"Spotty morning showers\",\"LongPhrase\":\"A couple of morning showers; otherwise, chilly with times of clouds and sun\",\"PrecipitationProbability\":80,\"ThunderstormProbability\":20,\"RainProbability\":80,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":14.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":296,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"WindGust\":{\"Speed\":{\"Value\":33.3,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":298,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"TotalLiquid\":{\"Value\":5,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":5,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":2,\"HoursOfRain\":2,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":65},\"Night\":{\"Icon\":35,\"IconPhrase\":\"Partly cloudy\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Partly cloudy\",\"LongPhrase\":\"Partly cloudy\",\"PrecipitationProbability\":3,\"ThunderstormProbability\":0,\"RainProbability\":3,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":14.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":301,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"WindGust\":{\"Speed\":{\"Value\":29.6,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":300,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":36},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=5&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=5&unit=c&lang=en-us\"}]}";
    }

    private void getForcastFromDB() {
        int attempts = 0;
        forecast = null;
        while (forecast == null && attempts < NUM_OF_ATTEMPTS) {
            forecast = weatherDataController.getForecastDataByLocation(mainActivity,
                    locationInfo.getCountry(), locationInfo.getCity());
            attempts++;
            if (attempts == 5) {
                //TODO Error to user? or what actions should we do. maybe log num of attempts.
            }
        }
    }

    private void getSunTimeFromDB() {
        int attempts = 0;
        sunriseSunset = null;
        while (sunriseSunset == null && attempts < NUM_OF_ATTEMPTS) {
            sunriseSunset = weatherDataController.getSunTimesDataByLocation(mainActivity,
                    locationInfo.getCountry(), locationInfo.getCity());
            attempts++;
            if (attempts == 5) {
                //TODO Error to user? or what actions should we do. maybe log num of attempts.
            }
        }
    }
}
