package com.example.sunnyapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class WeatherDataController {

    private static final WeatherDataController ourInstance = new WeatherDataController();

    private FirebaseController firebaseController;
    private FirebaseFirestore db;
    private DocumentReference mDocRef;
    private WeatherData weatherData = null;
    private final String BASE_PATH = "weather_by_loc/Countries/";

    public static WeatherDataController getInstance() {
        return ourInstance;
    }

    private WeatherDataController() {
        firebaseController = FirebaseController.getInstance();
        db = firebaseController.db;

    }

    public WeatherData getWeatherDataByLocation(String locationPath){
        mDocRef = db.document(locationPath);
        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()){
                    weatherData = documentSnapshot.toObject(WeatherData.class);
                    // Call some intent to move screen on success?
                }
                else
                {
                    Log.d("Firestore fetch error:", "No such path to document.");
                }
            }
        });
        return weatherData;
    }

    public void saveWeatherDataByLocation(String locationPath, WeatherData weatherData) {
        if (weatherData != null) {
            mDocRef = db.document(locationPath);
            mDocRef.set(weatherData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Weather data:", "Document has been saved!");
                            // Call some intent to move screen on success?
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Weather data:", "Failure: Document has not been saved!", e);
                }
            });
        }
    }

    public String weatherDataPathBuilder(String country, String city)
    {
        return BASE_PATH + country + "/" + city;
    }
}
