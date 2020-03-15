package com.example.sunnyapp;


import com.google.firebase.Timestamp;

import java.util.Date;

public class WeatherDataDTO {

    // Getting from DB
    private Timestamp timestamp;
    private int degreesCelsius;

    public WeatherDataDTO(){}

    public WeatherDataDTO(int degreesCelcius, Timestamp timestamp){
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
}
