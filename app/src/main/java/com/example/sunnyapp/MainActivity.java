package com.example.sunnyapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean firstSignIn = true;
//        WeatherDataController weatherDataController = WeatherDataController.getInstance();
//        WeatherData weatherData = weatherDataController.
//                getWeatherDataByLocation(weatherDataController.
//                        weatherDataPathBuilder("Israel", "Jerusalem"));
        // Need to insert data converted from GPS to country + city.


        Intent ImageManagerActivity = new Intent(getBaseContext(), ImageManagerActivity.class);
        startActivity(ImageManagerActivity);

//        if(firstSignIn) {
//            Intent firstSignInActivity = new Intent(getBaseContext(), FirstSignInActivity.class);
//            startActivity(firstSignInActivity);
//        }
//        String s = WeatherDataController.getInstance().getWeatherDataByLocation();
    }


}
