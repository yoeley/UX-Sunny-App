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

        // this section goes to Aviad's weather prediction page without log in or sign in
        Intent loadWeatherActivity = new Intent(getBaseContext(), LoadWeatherActivity.class);
        startActivity(loadWeatherActivity);


//        String s = WeatherDataController.getInstance().getWeatherDataByLocation();
    }


}
