package com.example.sunnyapp;


import com.google.firebase.Timestamp;

import java.util.Date;

public class WeatherData {

    // Getting from DB
    private Timestamp timestamp;
    private int degreesCelsius;

    // Converted from timestamp to Date.
//    private Date date;

    public WeatherData(){}

    public WeatherData(int degreesCelcius, Timestamp timestamp){
        this.degreesCelsius = degreesCelcius;
        this.timestamp = timestamp;
    }


    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public int getDegreesCelsius() {
        return degreesCelsius;
    }

    public void setDegreesCelsius(int degrees_celcius) {
        this.degreesCelsius = degrees_celcius;
    }

    public Date getDate() {
        return timestamp.toDate();
    }
}
