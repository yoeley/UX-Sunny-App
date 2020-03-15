package com.example.sunnyapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.Timestamp;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean firstSignIn = true;

        WeatherDataController weatherDataController = WeatherDataController.getInstance();
//        WeatherDataDTO weatherData = weatherDataController.
//                getWeatherDataByLocation(weatherDataController.
//                        weatherDataPathBuilder("Israel", "Jerusalem"));
        // Need to insert data converted from GPS to country + city.


//        Intent ImageManagerActivity = new Intent(getBaseContext(), ImageManagerActivity.class);
//        startActivity(ImageManagerActivity);


        /* to activate Yoel's sign-in sequence upon start, uncomment this section
        if(firstSignIn) {
            Intent firstSignInActivity = new Intent(getBaseContext(), FirstSignInActivity.class);
            startActivity(firstSignInActivity);
        }
         */

        Date date = new Date();
        Timestamp now = new Timestamp(date);
        FullWeatherData fullWeatherData = new FullWeatherData("Country", "City", 20, now);
        FullWeatherData receivedData = weatherDataController.getWeatherDataByLocation(this, "Country", "City");
        weatherDataController.saveWeatherDataByLocation(this, fullWeatherData);


//        weatherDataDTO = weatherDataController.
//                getWeatherDataByLocation(weatherDataController.
//                        weatherDataPathBuilder("Israel", "Jerusalem"));


        // this section goes to Aviad's weather prediction page without log in or sign in
//        Intent loadWeatherActivity = new Intent(getBaseContext(), LoadWeatherActivity.class);
//        startActivity(loadWeatherActivity);


//        String s = WeatherDataController.getInstance().getWeatherDataByLocation();
    }


}
