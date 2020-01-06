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
        String s = WeatherDataController.getInstance().getJerusalemWeatherData();
        if(firstSignIn) {
            Intent firstSignInActivity = new Intent(getBaseContext(), FirstSignInActivity.class);
            startActivity(firstSignInActivity);
        }
    }


}
