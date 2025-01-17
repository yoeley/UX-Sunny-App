package com.example.sunnyapp;

import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Calendar;

/**
 * The first activity of the app.
 * this is a loading screen with no interaction with the user.
 */
public class MainActivity extends AppCompatActivity {

    private final int PERMISSION_ID = 44;
    private FusedLocationProviderClient fusedLocationClient;
    private WeatherLoader weatherLoader;
    private Location location;
    private MainActivity mainActivity;
    private LocationListenerGPS locationListenerGPS;
    private Boolean isFirstDisplay;

    /**
     * starts the app up upon it's creation
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        weatherLoader = WeatherLoader.getInstance();
        locationListenerGPS = new LocationListenerGPS();
        mainActivity = this;
        isFirstDisplay = true;

        getLastLocation();
    }

    /**
     * gets the last known location of the user. usually it's the current location
     */
    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                loadWeatherForLocation(task);
                            }
                        }
                );
            } else {
                promptTurnOnLocation();
            }
        } else {
            requestPermissions();
        }
    }

    /**
     * loads the weather forecast for a given location
     * @param task
     */
    private void loadWeatherForLocation(@NonNull Task<Location> task) {
        location = task.getResult();
        setGetLastLocationAgain();
        if (location == null) {
            requestNewLocationData();
        } else {
            checkIsOutdoors();
            loadWeather();
        }
    }

    /**
     * prompts the user to turn on their location
     */
    private void promptTurnOnLocation() {
        Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    /**
     * checks whether the user is indoors or outdoors.
     * this will enable us to collect statistics about the times  when people like to go outside,
     * and over time achieve the best prediction of when is a good time to go out side
     */
    @SuppressLint("MissingPermission")
    private void checkIsOutdoors() {
        // if last fix on location happened more then 7 seconds ago, user is inside.
        // using LocationManager for this one instead of fusedLocationClient because
        // fusedLocationClient seems to give the time of a fixed location from any source,
        // while LocationManager gives the required time of the fix - that of the GPS fix
        LocationManager locationManager = (LocationManager) getBaseContext().getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 0, locationListenerGPS);


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                getUpdatedGPSLocation(locationManager);
            }
        }, 10000);
    }

    /**
     * try to get a GPS fix, and update weather the user is outdoor or indoors accordingly
     * (got GPS fix = user is outdoors)
     * @param locationManager
     */
    @SuppressLint("MissingPermission")
    private void getUpdatedGPSLocation(LocationManager locationManager) {
        Location currLocation = null;
        currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (Calendar.getInstance().getTime().getTime() - currLocation.getTime() > 7000) {
            uploadIsOutdoors(false);
        } else {
            uploadIsOutdoors(true);
        }
        locationManager.removeUpdates(locationListenerGPS);
    }

    /**
     * downloads weather data for the up
     */
    private void loadWeather() {
        weatherLoader.setLocation(location);
        weatherLoader.setMainActivity(mainActivity);
        weatherLoader.loadWeather(this);
    }

    /**
     * this method will upload to the firebase server information about whether the user is outdoors
     * or indoors. it's not yet implemented     *
     * @param isOutside
     */
    private void uploadIsOutdoors(Boolean isOutside) {}

    /**
     * requests a location fix
     */
    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(0);
        locationRequest.setFastestInterval(0);
        locationRequest.setNumUpdates(1);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.requestLocationUpdates(
                locationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        /**
         * updates location on result
         * @param locationResult
         */
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location location = locationResult.getLastLocation();
            System.out.println(location.getLatitude() + "" + location.getLongitude() + "");
        }
    };

    /**
     * checks for location-access permissions
     * @return
     */
    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    /**
     * requests location-access permissions
     * @return
     */
    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    /**
     * checks whether location is enabled or not
     * @return
     */
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    /**
     * reacts to granted permissions with a new request for location
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    /**
     * sets the app to update in 10 minutes.
     * this way the data stay updated,
     * and our servers get updated data about the percentage of users who are outdoors
     */
    private void setGetLastLocationAgain() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                isFirstDisplay = false;
                getLastLocation();
            }
        }, 600000);
    }

    /**
     * go to display screen, when the data has been collected
     */
    public void goToDisplayWeatherActivity() {
        Intent displayWeatherActivity = new Intent(getBaseContext(), DisplayWeatherActivity.class);
        displayWeatherActivity.putExtra("isFirstDisplay", isFirstDisplay);
        startActivity(displayWeatherActivity);
    }

    /**
     * listens for GPS location fixes
     */
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
