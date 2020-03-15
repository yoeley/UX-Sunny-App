package com.example.sunnyapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WeatherDataController {

    private static final WeatherDataController ourInstance = new WeatherDataController();

    private FirebaseController firebaseController;
    private FirebaseFirestore db;
    private DocumentReference mDocRef;
    private Forecast forecast = null;
    private SunriseSunset sunriseSunset = null;
    public CountDownLatch getForecastThread;
//    private CountDownLatch saveForecastThread;
    public CountDownLatch getSunTimeThread;
//    private CountDownLatch saveSuntimeThread;
    private final String BASE_PATH = "weather_by_loc/Countries/";
    private final String DATA_TYPE_PATH = "data_type/";
    private final String FORECAST_PATH = "forecast_data";
    private final String SUNTIME_PATH = "sun_time_data";
    private final int WAIT_TIME_IN_MILLISECONDS = 3000;

    public static WeatherDataController getInstance() {
        return ourInstance;
    }

    private WeatherDataController() {
        firebaseController = FirebaseController.getInstance();
        db = firebaseController.db;
    }

    public Forecast getForecastDataByLocation(String country, String city) {
        String locationPath = forecastDataPathBuilder(country, city);
        mDocRef = db.document(locationPath);
        getForecastThread = new CountDownLatch(1);
        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    forecast = documentSnapshot.toObject(Forecast.class);
                    Log.d("Forecast data:", "Document has been imported!");
                    getForecastThread.countDown();

                } else {
                    Log.d("Firestore fetch error:", "No such path to document.");
                    getForecastThread.countDown();
                }
            }
        });
        try {
            getForecastThread.await(WAIT_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(forecast != null) {
            return forecast;
        }
        else return null;
    }

    public void saveForecastDataByLocation(Forecast forecast, String country, String city) {
        if (forecast != null) {
            String locationPath = forecastDataPathBuilder(country, city);
            mDocRef = db.document(locationPath);
//            saveForecastThread = new CountDownLatch(1);
            mDocRef.set(forecast, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Forecast data:", "Document has been saved!");
//                            saveForecastThread.countDown();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Weather data:", "Failure: Document has not been saved!", e);
                }
            });
        }
        else {
            Log.d("Forecast data:", "Failure: Document has not been saved! - " +
                    "received null data.");
        }
//        try {
//            saveForecastThread.await(WAIT_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }


    public SunriseSunset getSunTimesDataByLocation(String country, String city) {
        String locationPath = sunTimeDataPathBuilder(country, city);
        mDocRef = db.document(locationPath);
        getSunTimeThread = new CountDownLatch(1);
        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    sunriseSunset = documentSnapshot.toObject(SunriseSunset.class);
                    Log.d("Weather data:", "Sunrise sunset Document has been imported!");
                    getSunTimeThread.countDown();
                } else {
                    Log.d("Firestore fetch error:", "No such path to document.");
                    getSunTimeThread.countDown();
                }
            }
        });

        try {
            getSunTimeThread.await(WAIT_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(sunriseSunset != null) {
            return sunriseSunset;
        }
        else return null;
    }

    public void saveSunTimesDataByLocation(SunriseSunset sunriseSunset, String country, String city) {
        if (sunriseSunset != null) {
            String locationPath = sunTimeDataPathBuilder(country, city);
//            saveSuntimeThread = new CountDownLatch(1);
            mDocRef = db.document(locationPath);
            mDocRef.set(sunriseSunset, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("SunTime data:", "Sunrise & set has been saved!");
//                            saveSuntimeThread.countDown();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("SunTime data:", "Failure: Document has not been saved!", e);
                }
            });
        }
//        try {
//            saveSuntimeThread.await(WAIT_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }


    public String forecastDataPathBuilder(String country, String city) {
        return BASE_PATH + country + "/" + city + "/" + DATA_TYPE_PATH + FORECAST_PATH;
    }

    public String sunTimeDataPathBuilder(String country, String city) {
        return BASE_PATH + country + "/" + city + "/" + DATA_TYPE_PATH + SUNTIME_PATH;
    }

}
