package com.example.sunnyapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.location.LocationListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Calendar;

public class LoadWeatherActivity extends AppCompatActivity {

    private int PERMISSION_ID = 44;
    private FusedLocationProviderClient fusedLocationClient;
    private WeatherLoader weatherLoader;
    private TextView logo;
    private Location location;
    private LoadWeatherActivity loadWeatherActivity;
    private ExternalGetLastLocationHandler externalGetLastLocationHandler;
    private LocationListenerGPS locationListenerGPS;
    private Boolean needToLoadWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_weather);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        logo = findViewById(R.id.logo);
        weatherLoader = WeatherLoader.getInstance();
        locationListenerGPS = new LocationListenerGPS();
        needToLoadWeather = true;
        loadWeatherActivity = this;

        getLastLocation();
    }

    public void externalGetLastLocation() {
        getLastLocation();
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                location = task.getResult();
                                setGetLastLocationAgain();
                                if (location == null) {
                                    requestNewLocationData();
                                } else {
                                    checkIsOutdoors();
                                    if (needToLoadWeather) {
                                        needToLoadWeather = false;
                                        loadWeather();
                                    }
                                }
                            }
                        }
                );
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }

    private void checkIsOutdoors() {
        // if last fix on location happend more then 5 seconds ago, user is inside.
        // using LocationManager for this one instead of fusedLocationClient because
        // fusedLocationClient seems to give the time of a fixed location from any source,
        // while LocationManager gives the reqired time of the fix - that of the GPS fix
        LocationManager locationManager = (LocationManager) getBaseContext().getSystemService(LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 0, locationListenerGPS);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                Location currLocation = null;
                try {
                    currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }

                if (Calendar.getInstance().getTime().getTime() - currLocation.getTime() > 5000) {
                    uploadIsOutdoors(false);
                } else {
                    uploadIsOutdoors(true);
                }
                locationManager.removeUpdates(locationListenerGPS);
            }
        }, 10000);
    }

    private void loadWeather() {
        weatherLoader.setLocation(location);
        weatherLoader.setLoadWeatherActivity(loadWeatherActivity);
        weatherLoader.loadWeather();
    }

    /**
     * this method will upload to the firebase server information about if the user is outside or
     * not. it's not yet implemented
     *
     * @param isOutside
     */
    private void uploadIsOutdoors(Boolean isOutside) {
//        // this code was used, and may still be used, for testing the checkIsOutdoors method
//        if (isOutside) {
//            logo.setText("Outside");
//        } else {
//            logo.setText("Inside");
//        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location location = locationResult.getLastLocation();
            System.out.println(location.getLatitude() + "" + location.getLongitude() + "");
        }
    };

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

//    @Override
//    public void onResume(){
//        super.onResume();
//        if (checkPermissions()) {
//            getLastLocation();
//        }
//    }

    private void setGetLastLocationAgain() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                getLastLocation();
            }
        }, 600000);
    }

    public void goToDisplayWeatherActivity() {
        Intent displayWeatherActivity = new Intent(getBaseContext(), DisplayWeatherActivity.class);
        startActivity(displayWeatherActivity);
    }

    public class LocationListenerGPS implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    }
}
