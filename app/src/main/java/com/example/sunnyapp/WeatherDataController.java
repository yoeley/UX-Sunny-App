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
    private WeatherDataDTO weatherDataDTO = null;
    private SunriseSunsetDTO sunriseSunsetDTO = null;
    private final String BASE_PATH = "weather_by_loc/Countries/";
    private final String DATA_TYPE_PATH = "data_type/";
    private final String WEATHER_PATH = "weather_data";
    private final String SUNRISE_SUNSET_PATH = "sun_time_data";

    public static WeatherDataController getInstance() {
        return ourInstance;
    }

    private WeatherDataController() {
        firebaseController = FirebaseController.getInstance();
        db = firebaseController.db;
    }

    public FullWeatherData getWeatherDataByLocation(Context context, String country, String city) {
        String locationPath = weatherDataPathBuilder(country, city);
        mDocRef = db.document(locationPath);
        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    weatherDataDTO = documentSnapshot.toObject(WeatherDataDTO.class);
                    // Call some intent to move screen on success?
                    Toast.makeText(context, "Weather data has been imported",
                            Toast.LENGTH_LONG).show();
                    Log.d("Weather data:", "Document has been imported!");
                    Toast.makeText(context, String.valueOf(weatherDataDTO.getDegreesCelsius())
                                    + "Celsius",
                            Toast.LENGTH_LONG).show();
                } else {
                    Log.d("Firestore fetch error:", "No such path to document.");
                }
            }
        });
        if(weatherDataDTO != null) {
            return new FullWeatherData(country, city, weatherDataDTO.getDegreesCelsius(),
                    weatherDataDTO.getTimestamp());
        }
        else return null;
    }

    public void saveWeatherDataByLocation(Context context, FullWeatherData fullWeatherData) {
        if (fullWeatherData != null) {
            String locationPath = weatherDataPathBuilder(fullWeatherData.getCountry(),
                    fullWeatherData.getCity());
            mDocRef = db.document(locationPath);
            mDocRef.set(getWeatherDTO(fullWeatherData), SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Weather data:", "Document has been saved!");
                            // Call some intent to move screen on success?
                            Toast.makeText(context, "Weather data has been uploaded to DB",
                                    Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Weather data:", "Failure: Document has not been saved!", e);
                }
            });
        }
    }


    public SunriseSunset getSunTimesDataByLocation(Context context, String country, String city) {
        String locationPath = weatherDataPathBuilder(country, city);
        mDocRef = db.document(locationPath);
        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    sunriseSunsetDTO = documentSnapshot.toObject(SunriseSunsetDTO.class);
                    // Call some intent to move screen on success?
                    Toast.makeText(context, "Sunrise sunset Weather data has been imported",
                            Toast.LENGTH_LONG).show();
                    Log.d("Weather data:", "Sunrise sunset Document has been imported!");
                } else {
                    Log.d("Firestore fetch error:", "No such path to document.");
                }
            }
        });
        if(sunriseSunsetDTO != null) {
            return SunriseSunsetGenerator.buildSunTime(sunriseSunsetDTO);
        }
        else return null;
    }

    public void saveSunTimesDataByLocation(Context context, SunriseSunset sunriseSunset,
                                           String country, String city) {
        if (sunriseSunset != null) {
            String locationPath = weatherDataPathBuilder(country, city);

            mDocRef = db.document(locationPath);
            mDocRef.set(getSunTimeDTO(sunriseSunset), SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Weather data:", "Sunrise & set has been saved!");
                            // Call some intent to move screen on success?
                            Toast.makeText(context, "Sunrise & set has been uploaded to DB",
                                    Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Weather data:", "Failure: Document has not been saved!", e);
                }
            });
        }
    }


    public String weatherDataPathBuilder(String country, String city) {
        return BASE_PATH + country + "/" + city + "/" + DATA_TYPE_PATH + WEATHER_PATH;
    }

    public String sunTimeDataPathBuilder(String country, String city) {
        return BASE_PATH + country + "/" + city + "/" + DATA_TYPE_PATH + SUNRISE_SUNSET_PATH;
    }


    private WeatherDataDTO getWeatherDTO(FullWeatherData fullWeatherData) {
        return new WeatherDataDTO(fullWeatherData.getDegreesCelsius(),
                fullWeatherData.getTimestamp());
    }

    private SunriseSunsetDTO getSunTimeDTO(SunriseSunset sunriseSunset) {
        return new SunriseSunsetDTO
                (sunriseSunset.getLatitude(),
                        sunriseSunset.getLongitude(),
                        sunriseSunset.getLocationKey(),
                        sunriseSunset.getDate(),
                        sunriseSunset.getMonth(),
                        sunriseSunset.getYear(),
                        sunriseSunset.getSunrise(),
                        sunriseSunset.getSunset());

    }

}
