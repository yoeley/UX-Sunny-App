package com.example.sunnyapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean firstSignIn = true;

        WeatherDataController weatherDataController = WeatherDataController.getInstance();

        // Need to insert data converted from GPS to country + city.
//        Intent ImageManagerActivity = new Intent(getBaseContext(), ImageManagerActivity.class);
//        startActivity(ImageManagerActivity);


        /* to activate Yoel's sign-in sequence upon start, uncomment this section
        if(firstSignIn) {
            Intent firstSignInActivity = new Intent(getBaseContext(), FirstSignInActivity.class);
            startActivity(firstSignInActivity);
        }
         */

        ArrayList<Double> forecastData = new ArrayList<Double>() {
            {
                add(10.0);
                add(11.0);
                add(12.0);
                add(13.0);
                add(14.0);
            }
        };
        SunriseSunset sunriseSunset = new SunriseSunset(10.0, 10.0, "locationKey",  "dateTime","sunrise", "sunset");

        Forecast forecast = new Forecast(10.0, 10.0, "locationKey", "dateTime",
                11.0, forecastData);

        FullWeatherData fullWeatherData = new FullWeatherData("England", "London", sunriseSunset, forecast);

        weatherDataController.saveForecastDataByLocation(this, fullWeatherData.getForecast(), fullWeatherData.getCountry(), fullWeatherData.getCity());
        weatherDataController.saveSunTimesDataByLocation(this, fullWeatherData.getSunriseSunset(), fullWeatherData.getCountry(), fullWeatherData.getCity());

        SunriseSunset sunriseSunset1 = weatherDataController.getSunTimesDataByLocation(this, "Israel", "Tel-Aviv");
        Forecast forecast1 = weatherDataController.getForecastDataByLocation(this, "Israel", "Tel-Aviv");


        Toast.makeText(this, "Got data?",
                Toast.LENGTH_LONG).show();



                // this section goes to Aviad's weather prediction page without log in or sign in
        Intent loadWeatherActivity = new Intent(getBaseContext(), LoadWeatherActivity.class);
        startActivity(loadWeatherActivity);
    }


}
