package com.example.sunnyapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

/**
 * show an error screen in case weather data could not be retrieved
 */
public class ErrorScreenActivity extends AppCompatActivity {

    /**
     * sets the activity upon it's creation
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_screen);
    }

    /**
     * try to refresh the app and get the weather data
     * @param view
     */
    public void retryRetrievingData(View view){
        Intent mainActivity = new Intent(getBaseContext(), MainActivity.class);
        startActivity(mainActivity);
    }

    /**
     * sets the back button to move the app to the background
     * (otherwise it would go back to the loading screen, which is undesirable.
     * the app updates automatically, and we want to allow the user to quit it naturally)
     */
    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }
}
