package com.example.sunnyapp;

import com.google.firebase.Timestamp;

import java.util.Date;

public class FullWeatherData {

    private Timestamp timestamp;
    private int degreesCelsius;
    private String country;
    private String city;

    private Date date;

    public FullWeatherData(){}

    public FullWeatherData(String country, String city, int degreesCelcius, Timestamp timestamp){
        this.degreesCelsius = degreesCelcius;
        this.timestamp = timestamp;
        this.country = country;
        this.city= city;

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

    public void setDate(Date date){
        this.date = date;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
