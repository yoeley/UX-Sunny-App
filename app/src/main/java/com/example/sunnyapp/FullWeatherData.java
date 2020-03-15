package com.example.sunnyapp;

import com.google.firebase.Timestamp;

import java.util.Date;

public class FullWeatherData {


    private String country;
    private String city;
    private SunriseSunset sunriseSunset;
    private Forecast forecast;

    public FullWeatherData(String country, String city, SunriseSunset sunriseSunset, Forecast forecast) {
        this.country = country;
        this.city = city;
        this.sunriseSunset = sunriseSunset;
        this.forecast = forecast;
    }

    public SunriseSunset getSunriseSunset() {
        return sunriseSunset;
    }

    public void setSunriseSunset(SunriseSunset sunriseSunset) {
        this.sunriseSunset = sunriseSunset;
    }

    public Forecast getForecast() {
        return forecast;
    }

    public void setForecast(Forecast forecast) {
        this.forecast = forecast;
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
