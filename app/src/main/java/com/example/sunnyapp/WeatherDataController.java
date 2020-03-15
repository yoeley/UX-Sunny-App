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

public class WeatherDataController {

    private static final WeatherDataController ourInstance = new WeatherDataController();

    private FirebaseController firebaseController;
    private FirebaseFirestore db;
    private DocumentReference mDocRef;
    private Forecast forecast = null;
    private SunriseSunset sunriseSunset = null;
    private final String BASE_PATH = "weather_by_loc/Countries/";
    private final String DATA_TYPE_PATH = "data_type/";
    private final String FORECAST_PATH = "forecast_data";
    private final String SUNTIME_PATH = "sun_time_data";

    public static WeatherDataController getInstance() {
        return ourInstance;
    }

    private WeatherDataController() {
        firebaseController = FirebaseController.getInstance();
        db = firebaseController.db;
    }

    public Forecast getForecastDataByLocation(Context context, String country, String city) {
        String locationPath = forecastDataPathBuilder(country, city);
        mDocRef = db.document(locationPath);
        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    forecast = documentSnapshot.toObject(Forecast.class);
                    // Call some intent to move screen on success?
                    Toast.makeText(context, "Forecast data has been imported",
                            Toast.LENGTH_LONG).show();
                    Log.d("Forecast data:", "Document has been imported!");
                    Toast.makeText(context, forecast.getCurrCondition()
                                    + " - Current condition",
                            Toast.LENGTH_LONG).show();
                } else {
                    Log.d("Firestore fetch error:", "No such path to document.");
                    Toast.makeText(context, "This path does not exist in the DB.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        if(forecast != null) {
            return forecast;
        }
        else return null;
    }

    public void saveForecastDataByLocation(Context context, Forecast forecast,
                                           String country, String city) {
        if (forecast != null) {
            String locationPath = forecastDataPathBuilder(country, city);
            mDocRef = db.document(locationPath);
            mDocRef.set(forecast, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Forecast data:", "Document has been saved!");
                            // Call some intent to move screen on success?
                            Toast.makeText(context, "Forecast data has been uploaded to DB",
                                    Toast.LENGTH_LONG).show();
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
            Toast.makeText(context, "Forecast data ERROR, is null, not uploaded to DB",
                    Toast.LENGTH_LONG).show();

        }
    }


    public SunriseSunset getSunTimesDataByLocation(Context context, String country, String city) {
        String locationPath = sunTimeDataPathBuilder(country, city);
        mDocRef = db.document(locationPath);
        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    sunriseSunset = documentSnapshot.toObject(SunriseSunset.class);
                    // Call some intent to move screen on success?
                    Toast.makeText(context, "Sunrise sunset data has been imported",
                            Toast.LENGTH_LONG).show();
                    Log.d("Weather data:", "Sunrise sunset Document has been imported!");
                } else {
                    Log.d("Firestore fetch error:", "No such path to document.");
                    Toast.makeText(context, "Sunrise sunset data has fetching failed ",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        if(sunriseSunset != null) {
            return sunriseSunset;
        }
        else return null;
    }

    public void saveSunTimesDataByLocation(Context context, SunriseSunset sunriseSunset,
                                           String country, String city) {
        if (sunriseSunset != null) {
            String locationPath = sunTimeDataPathBuilder(country, city);

            mDocRef = db.document(locationPath);
            mDocRef.set(sunriseSunset, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("SunTime data:", "Sunrise & set has been saved!");
                            // Call some intent to move screen on success?
                            Toast.makeText(context, "Sunrise & set has been uploaded to DB",
                                    Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("SunTime data:", "Failure: Document has not been saved!", e);
                    Toast.makeText(context, "Sunrise & set data has failed to upload to DB",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    public String forecastDataPathBuilder(String country, String city) {
        return BASE_PATH + country + "/" + city + "/" + DATA_TYPE_PATH + FORECAST_PATH;
    }

    public String sunTimeDataPathBuilder(String country, String city) {
        return BASE_PATH + country + "/" + city + "/" + DATA_TYPE_PATH + SUNTIME_PATH;
    }

}
