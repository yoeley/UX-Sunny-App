package com.example.sunnyapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class ErrorScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_screen);

        loadBrokenCloudImage();
        loadBackgroundLayout();
    }



    private void loadBrokenCloudImage() {
        ImageView brokenCloudImage = findViewById(R.id.error_cloud);
        brokenCloudImage.setImageResource(R.drawable.broken_cloud);
        }

    private void loadBackgroundLayout(){
        Drawable background = ContextCompat.getDrawable(this, R.drawable.midday);
        View view = this.getWindow().getDecorView();
        view.setBackground(background);
    }

    public void retryRetrievingData(View view){
        Intent mainActivity = new Intent(getBaseContext(), MainActivity.class);
        startActivity(mainActivity);
    }
}
