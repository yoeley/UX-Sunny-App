package com.example.sunnyapp;

import android.content.Context;
import android.content.Intent;
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

/**
 * loads weather data for the app
 */
public class WeatherLoader {

    private final int EARTH_RADIUS = 6371;

    private final static String API_KEY = "oOAbPsquosOnBVI0xjiZGLuYWAZBYajt";
    private final static String KEY_REQUEST_BODY_FORMAT = "https://dataservice.accuweather.com/locations/v1/cities/geoposition/search?apikey=%s&q=%s%%2C%s&toplevel=true";
    private final static String WEATHER_REQUEST_BODY_FORMAT = "http://dataservice.accuweather.com/forecasts/v1/hourly/12hour/%s?apikey=%s&details=true&metric=true";
    private final static String CURR_CONDITIONS_REQUEST_BODY_FORMAT = "http://dataservice.accuweather.com/currentconditions/v1/%s?apikey=%s&details=true";
    private final static String DAILY_5_DAYS_REQUEST_BODY_FORMAT = "http://dataservice.accuweather.com/forecasts/v1/daily/5day/%s?apikey=%s&details=true&metric=true";

    private final static long ONE_HOUR = 3600000; // in millis
    private final static long ONE_DAY = 86400000; // in millis
    private final int NUM_OF_ATTEMPTS = 5;

    private final WeatherDataController weatherDataController = WeatherDataController.getInstance();

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

    /**
     * starts the task to load weather
     * @param context
     */
    public void loadWeather(Context context) {
        new getWeatherTask(context).execute();
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

    /**
     * this method is required for a cache mechanism we haven't implemented for this version.
     * it will be implemented in future versions
     * @return
     */
    @NonNull
    public static String objectToString() {
        return null;
    }

    /**
     * gets the best time for an outdoors break, in millis
     * @return
     */
    public long getPickWeatherTimeMillis() {
        ArrayList<Double> currForecast = forecast.getForeCast();
        Double maxForecast = Collections.max(currForecast);
        long indexOfMaxForecast = currForecast.indexOf(maxForecast);

        long forecastTimeInMillis = DateStringConverter.stringToDate(forecast.getDateTime()).getTime();
        long forecastTimeRoundedToNextHour = forecastTimeInMillis + (ONE_HOUR - (forecastTimeInMillis % ONE_HOUR));

        return forecastTimeRoundedToNextHour + indexOfMaxForecast * ONE_HOUR;
    }

    /**
     * a task to load weather data for the app
     */
    private class getWeatherTask extends AsyncTask<Integer, Integer, Integer> {

        private Context context;
        private Boolean wasTaskSuccessful;

        public getWeatherTask(Context context){
            this.context = context;
        }

        /**
         * gets location key from AccuWeather.
         * location key is required for farther weather inquiries
         * @param client
         * @return
         * @throws JSONException
         * @throws IOException
         */
        private String obtainLocationKey(OkHttpClient client) throws JSONException, IOException {
            Request keyRequest = new Request.Builder()
                    .url(String.format(KEY_REQUEST_BODY_FORMAT, API_KEY, Double.toString(location.getLatitude()), Double.toString(location.getLongitude())))
                    .build();

            Response keyResponse = client.newCall(keyRequest).execute();
            String keyResult = keyResponse.body().string();

            JSONObject keyResultJSON = new JSONObject(keyResult);
            return keyResultJSON.getString("Key");
        }

        /**
         * gets the weather forecast from AccuWeather
         * @param client
         * @param locationKey
         * @return
         * @throws IOException
         * @throws JSONException
         */
        private JSONArray obtainWeatherForecastJSON(OkHttpClient client, String locationKey) throws IOException, JSONException {
            Request weatherRrequest = new Request.Builder()
                    .url(String.format(WEATHER_REQUEST_BODY_FORMAT, locationKey, API_KEY))
                    .build();

            Response weatherResponse = client.newCall(weatherRrequest).execute();
            String weatherResult = weatherResponse.body().string();

            return new JSONArray(weatherResult);
        }

        /**
         * gets the current weather conditions from AccuWeather
         * @param client
         * @param locationKey
         * @return
         * @throws IOException
         * @throws JSONException
         */
        private JSONArray obtainCurrConditions(OkHttpClient client, String locationKey) throws IOException, JSONException {
            Request currConditionsRrequest = new Request.Builder()
                    .url(String.format(CURR_CONDITIONS_REQUEST_BODY_FORMAT, locationKey, API_KEY))
                    .build();

            Response currConditionsResponse = client.newCall(currConditionsRrequest).execute();
            String currConditionsResult = currConditionsResponse.body().string();

            return new JSONArray(currConditionsResult);
        }

        /**
         * gets the 5 days forecast from AccuWeather (to obtain sunrise and sunset times)
         * @param client
         * @param locationKey
         * @return
         * @throws IOException
         * @throws JSONException
         */
        private JSONObject obtainDaily5DayForecast(OkHttpClient client, String locationKey) throws IOException, JSONException {
            Request daily5DaysRrequest = new Request.Builder()
                    .url(String.format(DAILY_5_DAYS_REQUEST_BODY_FORMAT, locationKey, API_KEY))
                    .build();

            Response daily5DaysResponse = client.newCall(daily5DaysRrequest).execute();
            String daily5DaysResult = daily5DaysResponse.body().string();

            return new JSONObject(daily5DaysResult);
        }

        /**
         * checks if registered location matches current location
         * @param formerLatitude
         * @param formerLongitude
         * @return
         */
        private Boolean checkLocationUpToDate(Double formerLatitude, Double formerLongitude) {
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();

            float results[] = new float[1];
            Location.distanceBetween(formerLatitude, formerLongitude, latitude, longitude, results);

            return !(results[0] > 1000);
        }

        /**
         * checks if SunriseSunset is updated for today
         * @param currDateTime
         * @return
         */
        private Boolean sunriseSunsetUpToDate(Date currDateTime) {
            long formerTime = DateStringConverter.stringToDate(sunriseSunset.getDateTime()).getTime();
            long currTime = currDateTime.getTime();

            // if current time and time when the sunriseSunset was taken don't have the same day, return false
            if ((formerTime - (formerTime % ONE_DAY)) - (currTime - (currTime % ONE_DAY)) != 0) {
                return false;
            }
            return true;
        }

        /**
         * checks if SunriseSunset is updated for today, for current location, and that it's not null
         * @param currDateTime
         * @return
         */
        private Boolean checkSunriseSunsetUpToDate(Date currDateTime) {
            if (sunriseSunset == null ||
                    !checkLocationUpToDate(sunriseSunset.getLatitude(), sunriseSunset.getLongitude()) ||
                    !sunriseSunsetUpToDate(currDateTime)) {
                return false;
            }
            return true;
        }

        /**
         * checks if Forecast is updated for this hour
         * @param currDateTime
         * @return
         */
        private Boolean forecastUpToDate(Date currDateTime) {
            long formerTime = DateStringConverter.stringToDate(forecast.getDateTime()).getTime();
            long currTime = currDateTime.getTime();

            // if current time and time when the forecast was taken don't have the same hour in the day, return false
            if ((formerTime - (formerTime % ONE_HOUR)) - (currTime - (currTime % ONE_HOUR)) != 0) {
                return false;
            }
            return true;
        }

        /**
         * checks if Forecast is updated for this hour, for current location, and that it's not null
         * @param currDateTime
         * @return
         */
        private Boolean checkForecastUpToDate(Date currDateTime) {
            if (forecast == null ||
                    !checkLocationUpToDate(forecast.getLatitude(), forecast.getLongitude()) ||
                    !forecastUpToDate(currDateTime)) {
                return false;
            }
            return true;
        }

        /**
         * updates location info for current location
         * @param client
         * @throws IOException
         * @throws JSONException
         */
        private void updateLocationInfo(OkHttpClient client) throws IOException, JSONException {
            Request keyRequest = new Request.Builder()
                    .url(String.format(KEY_REQUEST_BODY_FORMAT, API_KEY, Double.toString(location.getLatitude()), Double.toString(location.getLongitude())))
                    .build();

            Response keyResponse = client.newCall(keyRequest).execute();
            String keyResult = keyResponse.body().string();

            JSONObject keyResultJSON = new JSONObject(keyResult);

            locationInfo = new LocationInfo(keyResultJSON.getString("Key"), keyResultJSON.getJSONObject("Country").getString("ID"), keyResultJSON.getString("EnglishName"));
        }

        /**
         * gets example Forecast and SunriseSunset.
         * since we are limited to 50 calls to AccuWeather a day, this method is useful for testing
         * @throws JSONException
         */
        private void obtainForecastAndSunriseExample() throws JSONException {
            Date currDateTime = DateStringConverter.stringToDate(currDateTimeExample);

            String locationKey = locationKeyExample;

            JSONArray forecastJSON = new JSONArray(forecastJSONExample);
            JSONArray currConditionsJSON = new JSONArray(currConditionsJSONExample);
            JSONObject daily5DaysJSON = new JSONObject(daily5DaysJSONExample);

            forecast = ForecastGenerator.generate(location, locationKey, currDateTime, forecastJSON, currConditionsJSON);
            sunriseSunset = SunriseSunsetGenerator.generate(location, locationKey, currDateTime, daily5DaysJSON);
            weatherDataController.saveForecastDataByLocation(mainActivity, forecast, "Isreal", "Jerusalem");
            weatherDataController.saveSunTimesDataByLocation(mainActivity, sunriseSunset, "Isreal", "Jerusalem");
        }

        /**
         * uploads Forecast and SunriseSunset from Firebase
         * @param client
         * @param currDateTime
         * @return
         * @throws IOException
         * @throws JSONException
         */
        private Boolean uploadDataFromFirebase(OkHttpClient client, Date currDateTime) throws IOException, JSONException {
            if (!checkSunriseSunsetUpToDate(currDateTime)) {
                updateLocationInfo(client);
                getForecastFromDB(context);
                getSunTimeFromDB(context);
            } else if (!checkForecastUpToDate(currDateTime)) {
                updateLocationInfo(client);
                getForecastFromDB(context);
            } else {
                // everything already up to date - no upload done
                return false;
            }
            return true;
        }

        /**
         * uploads SunriseSunset from AccuWeather
         * @param client
         * @param currDateTime
         * @param locationKey
         * @throws IOException
         * @throws JSONException
         */
        private void uploadSunriseFromAccuWeather(OkHttpClient client, Date currDateTime, String locationKey) throws IOException, JSONException {
            JSONObject daily5DaysJSON = obtainDaily5DayForecast(client, locationKey);
            sunriseSunset = SunriseSunsetGenerator.generate(location, locationKey, currDateTime, daily5DaysJSON);
            weatherDataController.saveSunTimesDataByLocation(mainActivity, sunriseSunset, locationInfo.getCountry(), locationInfo.getCity());
        }

        /**
         * uploads Forecast from AccuWeather
         * @param client
         * @param currDateTime
         * @param locationKey
         * @throws IOException
         * @throws JSONException
         */
        private void uploadForecastFromAccuWeather(OkHttpClient client, Date currDateTime, String locationKey) throws IOException, JSONException {
            JSONArray forecastJSON = obtainWeatherForecastJSON(client, locationKey);
            JSONArray currConditionsJSON = obtainCurrConditions(client, locationKey);
            forecast = ForecastGenerator.generate(location, locationKey, currDateTime, forecastJSON, currConditionsJSON);
            weatherDataController.saveForecastDataByLocation(mainActivity, forecast, locationInfo.getCountry(), locationInfo.getCity());
        }

        /**
         * uploads all weather data from AccuWeather
         * @param client
         * @param currDateTime
         * @throws IOException
         * @throws JSONException
         */
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

        /**
         * if weather data is up to date - nothing is done
         * else, tries to upload it from Firebase.
         * if Firebase data is not up to date, uploads it from AccuWeather
         * @throws IOException
         * @throws JSONException
         */
        private void obtainForecastAndSunrise() throws IOException, JSONException {
            OkHttpClient client = new OkHttpClient();
            Date currDateTime = Calendar.getInstance().getTime();

            if (!uploadDataFromFirebase(client, currDateTime)) {
                // everything already up to date - quit function
                return;
            }
            uploadDataFromAccuWeather(client, currDateTime);
        }

        /**
         * upload weather data in the background
         * when the "obtainForecastAndSunrise" in uncommented, real data is obtained
         * when the "obtainForecastAndSunriseExample" in uncommented, example data is obtained
         * @param callers
         * @return
         */
        @Override
        protected Integer doInBackground(Integer... callers) {
            try {
                obtainForecastAndSunrise();
            } catch (IOException | JSONException e) {
                wasTaskSuccessful = false;
                e.printStackTrace();
                return null;
            }

//            try {
//                obtainForecastAndSunriseExample();
//            } catch (JSONException e) {
//                wasTaskSuccessful = false;
//                e.printStackTrace();
//                return null;
//            }

            wasTaskSuccessful = true;
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        /**
         * goes to display activity after task execution if it was successful,
         * goes to error screen if not
         * @param result
         */
        protected void onPostExecute(Integer result) {
            if (wasTaskSuccessful) {
                mainActivity.goToDisplayWeatherActivity();
            } else {
                Intent errorScreen = new Intent(context, ErrorScreenActivity.class);
                context.startActivity(errorScreen);
            }
        }

        private final String currDateTimeExample = "2020-03-19T10:37:00+02:00";
        private final String locationKeyExample = "213225";
        private final String forecastJSONExample = "[{\"DateTime\":\"2020-03-19T11:00:00+02:00\",\"EpochDateTime\":1584608400,\"WeatherIcon\":4,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":true,\"Temperature\":{\"Value\":9.2,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":5.6,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":8.4,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":7.4,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":31.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":238,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":42.6,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":88,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":3444,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":4,\"UVIndexText\":\"Moderate\",\"PrecipitationProbability\":5,\"RainProbability\":5,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":62,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=11&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=11&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T12:00:00+02:00\",\"EpochDateTime\":1584612000,\"WeatherIcon\":4,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":true,\"Temperature\":{\"Value\":9.9,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":6.8,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":9,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":7.9,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":33.3,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":243,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":44.4,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":87,\"Visibility\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":7498,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":6,\"UVIndexText\":\"High\",\"PrecipitationProbability\":8,\"RainProbability\":8,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":70,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=12&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=12&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T13:00:00+02:00\",\"EpochDateTime\":1584615600,\"WeatherIcon\":4,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":true,\"Temperature\":{\"Value\":10.4,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":7.5,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":8.8,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":7,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":35.2,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":249,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":79,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":8047,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":6,\"UVIndexText\":\"High\",\"PrecipitationProbability\":49,\"RainProbability\":49,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":56,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=13&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=13&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T14:00:00+02:00\",\"EpochDateTime\":1584619200,\"WeatherIcon\":13,\"IconPhrase\":\"Mostly cloudy w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"IsDaylight\":true,\"Temperature\":{\"Value\":10.7,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":4.8,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":8.7,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":6.4,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":38.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":255,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":48.2,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":74,\"Visibility\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":8595,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":4,\"UVIndexText\":\"Moderate\",\"PrecipitationProbability\":56,\"RainProbability\":56,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0.7,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0.7,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":70,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=14&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=14&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T15:00:00+02:00\",\"EpochDateTime\":1584622800,\"WeatherIcon\":14,\"IconPhrase\":\"Partly sunny w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"IsDaylight\":true,\"Temperature\":{\"Value\":9.9,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":3.4,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":8.2,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":6,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":38.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":262,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":50,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":77,\"Visibility\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":9144,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":3,\"UVIndexText\":\"Moderate\",\"PrecipitationProbability\":56,\"RainProbability\":56,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0.7,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0.7,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":49,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=15&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=15&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T16:00:00+02:00\",\"EpochDateTime\":1584626400,\"WeatherIcon\":3,\"IconPhrase\":\"Partly sunny\",\"HasPrecipitation\":false,\"IsDaylight\":true,\"Temperature\":{\"Value\":9.1,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":3.3,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":7.6,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":5.8,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":40.7,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":264,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":50,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":80,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":9144,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":2,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":16,\"RainProbability\":16,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":45,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=16&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=16&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T17:00:00+02:00\",\"EpochDateTime\":1584630000,\"WeatherIcon\":3,\"IconPhrase\":\"Partly sunny\",\"HasPrecipitation\":false,\"IsDaylight\":true,\"Temperature\":{\"Value\":8.1,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":1.3,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":7,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":5.5,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":38.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":266,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":50,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":84,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":9144,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":1,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":12,\"RainProbability\":12,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":40,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=17&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=17&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T18:00:00+02:00\",\"EpochDateTime\":1584633600,\"WeatherIcon\":35,\"IconPhrase\":\"Partly cloudy\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":7.5,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":0.2,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":6.5,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":5,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":37,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":268,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":50,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":84,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":9144,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":10,\"RainProbability\":10,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":45,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=18&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=18&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T19:00:00+02:00\",\"EpochDateTime\":1584637200,\"WeatherIcon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":6.9,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":0.1,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":5.9,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":4.4,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":31.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":269,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":84,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":9144,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":7,\"RainProbability\":7,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":50,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=19&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=19&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T20:00:00+02:00\",\"EpochDateTime\":1584640800,\"WeatherIcon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":6.6,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":0.2,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":5.4,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":3.8,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":27.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":270,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":40.7,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":82,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":9144,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":7,\"RainProbability\":7,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":55,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=20&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=20&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T21:00:00+02:00\",\"EpochDateTime\":1584644400,\"WeatherIcon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":6.4,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":0.1,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":5.2,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":3.4,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":27.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":269,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":35.2,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":82,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":9144,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":7,\"RainProbability\":7,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":54,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=21&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=21&unit=c&lang=en-us\"},{\"DateTime\":\"2020-03-19T22:00:00+02:00\",\"EpochDateTime\":1584648000,\"WeatherIcon\":36,\"IconPhrase\":\"Intermittent clouds\",\"HasPrecipitation\":false,\"IsDaylight\":false,\"Temperature\":{\"Value\":6,\"Unit\":\"C\",\"UnitType\":17},\"RealFeelTemperature\":{\"Value\":-0.3,\"Unit\":\"C\",\"UnitType\":17},\"WetBulbTemperature\":{\"Value\":4.8,\"Unit\":\"C\",\"UnitType\":17},\"DewPoint\":{\"Value\":3,\"Unit\":\"C\",\"UnitType\":17},\"Wind\":{\"Speed\":{\"Value\":25.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":266,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":35.2,\"Unit\":\"km\\/h\",\"UnitType\":7}},\"RelativeHumidity\":81,\"Visibility\":{\"Value\":16.1,\"Unit\":\"km\",\"UnitType\":6},\"Ceiling\":{\"Value\":9144,\"Unit\":\"m\",\"UnitType\":5},\"UVIndex\":0,\"UVIndexText\":\"Low\",\"PrecipitationProbability\":7,\"RainProbability\":7,\"SnowProbability\":0,\"IceProbability\":0,\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"CloudCover\":54,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=22&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/hourly-weather-forecast\\/213225?day=1&hbhhour=22&unit=c&lang=en-us\"}]";
        private final String currConditionsJSONExample = "[{\"LocalObservationDateTime\":\"2020-03-19T10:26:00+02:00\",\"EpochTime\":1584606360,\"WeatherText\":\"Clouds and sun\",\"WeatherIcon\":4,\"HasPrecipitation\":false,\"PrecipitationType\":null,\"IsDayTime\":true,\"Temperature\":{\"Metric\":{\"Value\":9,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":48,\"Unit\":\"F\",\"UnitType\":18}},\"RealFeelTemperature\":{\"Metric\":{\"Value\":3.7,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":39,\"Unit\":\"F\",\"UnitType\":18}},\"RealFeelTemperatureShade\":{\"Metric\":{\"Value\":2.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":36,\"Unit\":\"F\",\"UnitType\":18}},\"RelativeHumidity\":88,\"DewPoint\":{\"Metric\":{\"Value\":7.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":45,\"Unit\":\"F\",\"UnitType\":18}},\"Wind\":{\"Direction\":{\"Degrees\":180,\"Localized\":\"S\",\"English\":\"S\"},\"Speed\":{\"Metric\":{\"Value\":37.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Imperial\":{\"Value\":23.5,\"Unit\":\"mi\\/h\",\"UnitType\":9}}},\"WindGust\":{\"Speed\":{\"Metric\":{\"Value\":51.9,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Imperial\":{\"Value\":32.2,\"Unit\":\"mi\\/h\",\"UnitType\":9}}},\"UVIndex\":3,\"UVIndexText\":\"Moderate\",\"Visibility\":{\"Metric\":{\"Value\":9.7,\"Unit\":\"km\",\"UnitType\":6},\"Imperial\":{\"Value\":6,\"Unit\":\"mi\",\"UnitType\":2}},\"ObstructionsToVisibility\":\"\",\"CloudCover\":51,\"Ceiling\":{\"Metric\":{\"Value\":1372,\"Unit\":\"m\",\"UnitType\":5},\"Imperial\":{\"Value\":4500,\"Unit\":\"ft\",\"UnitType\":0}},\"Pressure\":{\"Metric\":{\"Value\":1015,\"Unit\":\"mb\",\"UnitType\":14},\"Imperial\":{\"Value\":29.97,\"Unit\":\"inHg\",\"UnitType\":12}},\"PressureTendency\":{\"LocalizedText\":\"Falling\",\"Code\":\"F\"},\"Past24HourTemperatureDeparture\":{\"Metric\":{\"Value\":0.9,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":2,\"Unit\":\"F\",\"UnitType\":18}},\"ApparentTemperature\":{\"Metric\":{\"Value\":10.6,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":51,\"Unit\":\"F\",\"UnitType\":18}},\"WindChillTemperature\":{\"Metric\":{\"Value\":4.4,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":40,\"Unit\":\"F\",\"UnitType\":18}},\"WetBulbTemperature\":{\"Metric\":{\"Value\":8.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":47,\"Unit\":\"F\",\"UnitType\":18}},\"Precip1hr\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"PrecipitationSummary\":{\"Precipitation\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"PastHour\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past3Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past6Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past9Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past12Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past18Hours\":{\"Metric\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0,\"Unit\":\"in\",\"UnitType\":1}},\"Past24Hours\":{\"Metric\":{\"Value\":0.2,\"Unit\":\"mm\",\"UnitType\":3},\"Imperial\":{\"Value\":0.01,\"Unit\":\"in\",\"UnitType\":1}}},\"TemperatureSummary\":{\"Past6HourRange\":{\"Minimum\":{\"Metric\":{\"Value\":7.6,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":46,\"Unit\":\"F\",\"UnitType\":18}},\"Maximum\":{\"Metric\":{\"Value\":9,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":48,\"Unit\":\"F\",\"UnitType\":18}}},\"Past12HourRange\":{\"Minimum\":{\"Metric\":{\"Value\":7.6,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":46,\"Unit\":\"F\",\"UnitType\":18}},\"Maximum\":{\"Metric\":{\"Value\":9.1,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":48,\"Unit\":\"F\",\"UnitType\":18}}},\"Past24HourRange\":{\"Minimum\":{\"Metric\":{\"Value\":7.6,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":46,\"Unit\":\"F\",\"UnitType\":18}},\"Maximum\":{\"Metric\":{\"Value\":9.7,\"Unit\":\"C\",\"UnitType\":17},\"Imperial\":{\"Value\":50,\"Unit\":\"F\",\"UnitType\":18}}}},\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/current-weather\\/213225?lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/current-weather\\/213225?lang=en-us\"}]";
        private final String daily5DaysJSONExample = "{\"Headline\":{\"EffectiveDate\":\"2020-03-19T13:00:00+02:00\",\"EffectiveEpochDate\":1584615600,\"Severity\":5,\"Text\":\"Expect showers Thursday afternoon\",\"Category\":\"rain\",\"EndDate\":\"2020-03-19T19:00:00+02:00\",\"EndEpochDate\":1584637200,\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/extended-weather-forecast\\/213225?unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?unit=c&lang=en-us\"},\"DailyForecasts\":[{\"Date\":\"2020-03-19T07:00:00+02:00\",\"EpochDate\":1584594000,\"Sun\":{\"Rise\":\"2020-03-19T05:44:00+02:00\",\"EpochRise\":1584589440,\"Set\":\"2020-03-19T17:50:00+02:00\",\"EpochSet\":1584633000},\"Moon\":{\"Rise\":\"2020-03-19T03:01:00+02:00\",\"EpochRise\":1584579660,\"Set\":\"2020-03-19T13:25:00+02:00\",\"EpochSet\":1584617100,\"Phase\":\"WaningCrescent\",\"Age\":25},\"Temperature\":{\"Minimum\":{\"Value\":5,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":10.7,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":-0.3,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":7.5,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":-0.3,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":5,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":5.2,\"DegreeDaySummary\":{\"Heating\":{\"Value\":10,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":58,\"Category\":\"Moderate\",\"CategoryValue\":2,\"Type\":\"Particle Pollution\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":6,\"Category\":\"High\",\"CategoryValue\":3}],\"Day\":{\"Icon\":14,\"IconPhrase\":\"Partly sunny w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"Windy with clouds and sun\",\"LongPhrase\":\"Windy and cool with intervals of clouds and sunshine; a shower in spots this afternoon\",\"PrecipitationProbability\":56,\"ThunderstormProbability\":20,\"RainProbability\":56,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":33.3,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":255,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":50,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":264,\"Localized\":\"W\",\"English\":\"W\"}},\"TotalLiquid\":{\"Value\":1.4,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":1.4,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":1,\"HoursOfRain\":1,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":58},\"Night\":{\"Icon\":39,\"IconPhrase\":\"Partly cloudy w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"Windy early; partly cloudy\",\"LongPhrase\":\"Windy this evening; partly cloudy with a stray shower late\",\"PrecipitationProbability\":42,\"ThunderstormProbability\":20,\"RainProbability\":42,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":20.4,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":269,\"Localized\":\"W\",\"English\":\"W\"}},\"WindGust\":{\"Speed\":{\"Value\":46.3,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":269,\"Localized\":\"W\",\"English\":\"W\"}},\"TotalLiquid\":{\"Value\":0.4,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0.4,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0.5,\"HoursOfRain\":0.5,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":64},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=1&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=1&unit=c&lang=en-us\"},{\"Date\":\"2020-03-20T07:00:00+02:00\",\"EpochDate\":1584680400,\"Sun\":{\"Rise\":\"2020-03-20T05:42:00+02:00\",\"EpochRise\":1584675720,\"Set\":\"2020-03-20T17:51:00+02:00\",\"EpochSet\":1584719460},\"Moon\":{\"Rise\":\"2020-03-20T03:43:00+02:00\",\"EpochRise\":1584668580,\"Set\":\"2020-03-20T14:21:00+02:00\",\"EpochSet\":1584706860,\"Phase\":\"WaningCrescent\",\"Age\":26},\"Temperature\":{\"Minimum\":{\"Value\":3.8,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":7.5,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":5.5,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":4.3,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":4.3,\"DegreeDaySummary\":{\"Heating\":{\"Value\":12,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":34,\"Category\":\"Good\",\"CategoryValue\":1,\"Type\":\"Ozone\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":3,\"Category\":\"Moderate\",\"CategoryValue\":2}],\"Day\":{\"Icon\":14,\"IconPhrase\":\"Partly sunny w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Moderate\",\"ShortPhrase\":\"A morning shower\",\"LongPhrase\":\"A morning shower; otherwise, cold with times of clouds and sun, becoming breezy in the afternoon\",\"PrecipitationProbability\":61,\"ThunderstormProbability\":20,\"RainProbability\":61,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":18.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":249,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"WindGust\":{\"Speed\":{\"Value\":33.3,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":256,\"Localized\":\"WSW\",\"English\":\"WSW\"}},\"TotalLiquid\":{\"Value\":3,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":3,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":2,\"HoursOfRain\":2,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":70},\"Night\":{\"Icon\":12,\"IconPhrase\":\"Showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Light\",\"ShortPhrase\":\"A couple of showers\",\"LongPhrase\":\"Mostly cloudy and chilly with a couple of showers, mainly early\",\"PrecipitationProbability\":63,\"ThunderstormProbability\":20,\"RainProbability\":63,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":9.3,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":293,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"WindGust\":{\"Speed\":{\"Value\":27.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":274,\"Localized\":\"W\",\"English\":\"W\"}},\"TotalLiquid\":{\"Value\":2.1,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":2.1,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":1.5,\"HoursOfRain\":1.5,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":79},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=2&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=2&unit=c&lang=en-us\"},{\"Date\":\"2020-03-21T07:00:00+02:00\",\"EpochDate\":1584766800,\"Sun\":{\"Rise\":\"2020-03-21T05:41:00+02:00\",\"EpochRise\":1584762060,\"Set\":\"2020-03-21T17:52:00+02:00\",\"EpochSet\":1584805920},\"Moon\":{\"Rise\":\"2020-03-21T04:21:00+02:00\",\"EpochRise\":1584757260,\"Set\":\"2020-03-21T15:17:00+02:00\",\"EpochSet\":1584796620,\"Phase\":\"WaningCrescent\",\"Age\":27},\"Temperature\":{\"Minimum\":{\"Value\":5.7,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":9.9,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":2.6,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":10.2,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":2.6,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":6.3,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":5.3,\"DegreeDaySummary\":{\"Heating\":{\"Value\":10,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":35,\"Category\":\"Good\",\"CategoryValue\":1,\"Type\":\"Ozone\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":7,\"Category\":\"High\",\"CategoryValue\":3}],\"Day\":{\"Icon\":14,\"IconPhrase\":\"Partly sunny w\\/ showers\",\"HasPrecipitation\":true,\"PrecipitationType\":\"Rain\",\"PrecipitationIntensity\":\"Moderate\",\"ShortPhrase\":\"A couple of morning showers\",\"LongPhrase\":\"A couple of morning showers; otherwise, chilly with times of clouds and sun\",\"PrecipitationProbability\":73,\"ThunderstormProbability\":20,\"RainProbability\":73,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":18.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":299,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"WindGust\":{\"Speed\":{\"Value\":31.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":306,\"Localized\":\"NW\",\"English\":\"NW\"}},\"TotalLiquid\":{\"Value\":3.6,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":3.6,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":2,\"HoursOfRain\":2,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":62},\"Night\":{\"Icon\":35,\"IconPhrase\":\"Partly cloudy\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Partly cloudy\",\"LongPhrase\":\"Partly cloudy\",\"PrecipitationProbability\":2,\"ThunderstormProbability\":0,\"RainProbability\":2,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":13,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":302,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"WindGust\":{\"Speed\":{\"Value\":27.8,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":298,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":35},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=3&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=3&unit=c&lang=en-us\"},{\"Date\":\"2020-03-22T07:00:00+02:00\",\"EpochDate\":1584853200,\"Sun\":{\"Rise\":\"2020-03-22T05:40:00+02:00\",\"EpochRise\":1584848400,\"Set\":\"2020-03-22T17:52:00+02:00\",\"EpochSet\":1584892320},\"Moon\":{\"Rise\":\"2020-03-22T04:54:00+02:00\",\"EpochRise\":1584845640,\"Set\":\"2020-03-22T16:12:00+02:00\",\"EpochSet\":1584886320,\"Phase\":\"WaningCrescent\",\"Age\":28},\"Temperature\":{\"Minimum\":{\"Value\":5.3,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":13.2,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":5.9,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":16.9,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":5.9,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":11.6,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":8,\"DegreeDaySummary\":{\"Heating\":{\"Value\":9,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":45,\"Category\":\"Good\",\"CategoryValue\":1,\"Type\":\"Ozone\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":7,\"Category\":\"High\",\"CategoryValue\":3}],\"Day\":{\"Icon\":3,\"IconPhrase\":\"Partly sunny\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Partly sunny\",\"LongPhrase\":\"Partly sunny\",\"PrecipitationProbability\":0,\"ThunderstormProbability\":0,\"RainProbability\":0,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":11.1,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":302,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"WindGust\":{\"Speed\":{\"Value\":18.5,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":294,\"Localized\":\"WNW\",\"English\":\"WNW\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":37},\"Night\":{\"Icon\":34,\"IconPhrase\":\"Mostly clear\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Mainly clear\",\"LongPhrase\":\"Mainly clear\",\"PrecipitationProbability\":0,\"ThunderstormProbability\":0,\"RainProbability\":0,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":5.6,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":215,\"Localized\":\"SW\",\"English\":\"SW\"}},\"WindGust\":{\"Speed\":{\"Value\":11.1,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":313,\"Localized\":\"NW\",\"English\":\"NW\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":33},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=4&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=4&unit=c&lang=en-us\"},{\"Date\":\"2020-03-23T07:00:00+02:00\",\"EpochDate\":1584939600,\"Sun\":{\"Rise\":\"2020-03-23T05:39:00+02:00\",\"EpochRise\":1584934740,\"Set\":\"2020-03-23T17:53:00+02:00\",\"EpochSet\":1584978780},\"Moon\":{\"Rise\":\"2020-03-23T05:26:00+02:00\",\"EpochRise\":1584933960,\"Set\":\"2020-03-23T17:06:00+02:00\",\"EpochSet\":1584975960,\"Phase\":\"WaningCrescent\",\"Age\":29},\"Temperature\":{\"Minimum\":{\"Value\":9.7,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":17.4,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperature\":{\"Minimum\":{\"Value\":9.8,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":20.7,\"Unit\":\"C\",\"UnitType\":17}},\"RealFeelTemperatureShade\":{\"Minimum\":{\"Value\":9.8,\"Unit\":\"C\",\"UnitType\":17},\"Maximum\":{\"Value\":16.8,\"Unit\":\"C\",\"UnitType\":17}},\"HoursOfSun\":6.6,\"DegreeDaySummary\":{\"Heating\":{\"Value\":4,\"Unit\":\"C\",\"UnitType\":17},\"Cooling\":{\"Value\":0,\"Unit\":\"C\",\"UnitType\":17}},\"AirAndPollen\":[{\"Name\":\"AirQuality\",\"Value\":43,\"Category\":\"Good\",\"CategoryValue\":1,\"Type\":\"Particle Pollution\"},{\"Name\":\"Grass\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Mold\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Ragweed\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"Tree\",\"Value\":0,\"Category\":\"Low\",\"CategoryValue\":1},{\"Name\":\"UVIndex\",\"Value\":7,\"Category\":\"High\",\"CategoryValue\":3}],\"Day\":{\"Icon\":3,\"IconPhrase\":\"Partly sunny\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Partly sunny\",\"LongPhrase\":\"Partly sunny\",\"PrecipitationProbability\":1,\"ThunderstormProbability\":0,\"RainProbability\":1,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":9.3,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":167,\"Localized\":\"SSE\",\"English\":\"SSE\"}},\"WindGust\":{\"Speed\":{\"Value\":16.7,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":147,\"Localized\":\"SSE\",\"English\":\"SSE\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":47},\"Night\":{\"Icon\":33,\"IconPhrase\":\"Clear\",\"HasPrecipitation\":false,\"ShortPhrase\":\"Clear\",\"LongPhrase\":\"Clear\",\"PrecipitationProbability\":1,\"ThunderstormProbability\":0,\"RainProbability\":1,\"SnowProbability\":0,\"IceProbability\":0,\"Wind\":{\"Speed\":{\"Value\":7.4,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":201,\"Localized\":\"SSW\",\"English\":\"SSW\"}},\"WindGust\":{\"Speed\":{\"Value\":13,\"Unit\":\"km\\/h\",\"UnitType\":7},\"Direction\":{\"Degrees\":232,\"Localized\":\"SW\",\"English\":\"SW\"}},\"TotalLiquid\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Rain\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"Snow\":{\"Value\":0,\"Unit\":\"cm\",\"UnitType\":4},\"Ice\":{\"Value\":0,\"Unit\":\"mm\",\"UnitType\":3},\"HoursOfPrecipitation\":0,\"HoursOfRain\":0,\"HoursOfSnow\":0,\"HoursOfIce\":0,\"CloudCover\":0},\"Sources\":[\"AccuWeather\"],\"MobileLink\":\"http:\\/\\/m.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=5&unit=c&lang=en-us\",\"Link\":\"http:\\/\\/www.accuweather.com\\/en\\/il\\/jerusalem\\/213225\\/daily-weather-forecast\\/213225?day=5&unit=c&lang=en-us\"}]}";
    }

    /**
     * loads Forecast from Firebase
     * @param context
     */
    private void getForecastFromDB(Context context) {
        int attempts = 0;
        forecast = null;
        while (forecast == null && attempts < NUM_OF_ATTEMPTS) {
            forecast = weatherDataController.getForecastDataByLocation(mainActivity,
                    locationInfo.getCountry(), locationInfo.getCity());
            attempts++;
            if (attempts == 5) {
                Intent errorScreen = new Intent(context, ErrorScreenActivity.class);
                context.startActivity(errorScreen);
            }
        }
    }

    /**
     * loads SunriseSunset from Firebase
     * @param context
     */
    private void getSunTimeFromDB(Context context) {
        int attempts = 0;
        sunriseSunset = null;
        while (sunriseSunset == null && attempts < NUM_OF_ATTEMPTS) {
            sunriseSunset = weatherDataController.getSunTimesDataByLocation(mainActivity,
                    locationInfo.getCountry(), locationInfo.getCity());
            attempts++;
            if (attempts == 5) {
                Intent errorScreen = new Intent(context, ErrorScreenActivity.class);
                context.startActivity(errorScreen);
            }
        }
    }
}
