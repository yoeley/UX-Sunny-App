package com.example.sunnyapp;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class WeatherDataController {

    private static final WeatherDataController ourInstance = new WeatherDataController();

    private FirebaseController firebaseController;
    private FirebaseFirestore db;
    private DocumentReference mDocRef;

    public static WeatherDataController getInstance() {
        return ourInstance;
    }

    private WeatherDataController() {
        firebaseController = FirebaseController.getInstance();
        db = firebaseController.db;

    }

    public String getJerusalemWeatherData(){
        mDocRef = db.document("weather_by_loc/Countries");
        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()){
                    String dateAndTime = documentSnapshot.getString("date_and_time");
                    String degrees_celcius = documentSnapshot.getString("degrees_celcius");

                    // TODO do a weatherData object to save info and use Map<String, weatherData> my data = documentSnapshot.getData();
                    // Or a to object function, look at tutorial - Better use to object.
                }
            }
        });
        return "s";
    }
}
