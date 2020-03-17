package com.example.sunnyapp;

import android.content.Context;
import android.util.Log;

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

    private FirebaseFirestore db;
    private DocumentReference docRef;
    private Forecast forecast = null;
    private SunriseSunset sunriseSunset = null;
    private CountDownLatch getForecastThread;
    private CountDownLatch getSunTimeThread;

    private final String BASE_PATH = "weather_by_loc/Countries/";
    private final String DATA_TYPE_PATH = "data_type/";
    private final String FORECAST_PATH = "forecast_data";
    private final String SUN_TIME_PATH = "sun_time_data";
    private final int WAIT_TIME_IN_MILLISECONDS = 3000;

    /**
     * Singleton of the WeatherDataController instance
     * @return The WeatherDataController instance
     */
    static WeatherDataController getInstance() {
        return ourInstance;
    }

    /**
     * Constructor for the WeatherDataController instance (a singleton)
     */
    private WeatherDataController() {
        FireBaseController firebaseController = FireBaseController.getInstance();
        db = firebaseController.dB;
    }

    /**
     * Retrieves the forecast from the FireStore data base for the given location if exists.
     * @param context - The current app context.
     * @param country - The country's data we want to get - part of the path for the "query".
     * @param city - The city's data we want to get.
     * @return A Forecast object that it's fields hold the data received from the database.
     */
    Forecast getForecastDataByLocation(Context context, String country, String city) {
        String locationPath = forecastDataPathBuilder(country, city);
        docRef = db.document(locationPath);
        getForecastThread = new CountDownLatch(1);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    forecast = documentSnapshot.toObject(Forecast.class);
                    Log.d("Forecast data:", "Document has been imported!");
                    getForecastThread.countDown();

                } else {
                    Log.d("FireStore fetch error:", "No such path to document.");
                    getForecastThread.countDown();
                }
            }
        });
        // Await timeout for call (thread wise).
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

    /**
     * Uploads the given data to the FireStore data base in the given path (by location).
     * @param context - The current app context.
     * @param forecast - The forecast object, that its data & fields we want to upload.
     * @param country - The country's data we want to get - part of the path for the "query".
     * @param city - The city's data we want to get.
     */
    public void saveForecastDataByLocation(Context context, Forecast forecast, String country,
                                           String city) {
        if (forecast != null) {
            String locationPath = forecastDataPathBuilder(country, city);
            docRef = db.document(locationPath);
            docRef.set(forecast, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Forecast data:", "Document has been saved!");
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
    }


    /**
     * Retrieves the sun rise & sun set from the FireStore data base for the given location if exists.
     * @param context - The current app context.
     * @param country - The country's data we want to get - part of the path for the "query".
     * @param city - The city's data we want to get.
     * @return A SunriseSunset object that it's fields hold the data received from the database.
     */
    public SunriseSunset getSunTimesDataByLocation(Context context, String country, String city) {
        String locationPath = sunTimeDataPathBuilder(country, city);
        docRef = db.document(locationPath);
        getSunTimeThread = new CountDownLatch(1);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
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

    /**
     * Uploads the given data to the FireStore data base in the given path (by location).
     * @param context - The current app context.
     * @param sunriseSunset - The SunriseSunset object, that its data & fields we want to upload.
     * @param country - The country's data we want to get - part of the path for the "query".
     * @param city - The city's data we want to get.
     */
    public void saveSunTimesDataByLocation(Context context, SunriseSunset sunriseSunset, String country, String city) {
        if (sunriseSunset != null) {
            String locationPath = sunTimeDataPathBuilder(country, city);
            docRef = db.document(locationPath);
            docRef.set(sunriseSunset, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("SunTime data:", "Sunrise & set has been saved!");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("SunTime data:", "Failure: Document has not been saved!", e);
                }
            });
        }
    }


    /**
     *
     * @param country  - The given country
     * @param city - The given city
     * @return A String that is a data base constructed path for storing the data for the Forecast
     * object.
     */
    public String forecastDataPathBuilder(String country, String city) {
        return BASE_PATH + country + "/" + city + "/" + DATA_TYPE_PATH + FORECAST_PATH;
    }

    /**
     *
     * @param country  - The given country
     * @param city - The given city
     * @return A String that is a data base constructed path for storing the data for the
     * SunriseSunset object.
     */
    public String sunTimeDataPathBuilder(String country, String city) {
        return BASE_PATH + country + "/" + city + "/" + DATA_TYPE_PATH + SUN_TIME_PATH;
    }

}
