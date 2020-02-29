package com.example.sunnyapp;

import java.io.Serializable;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Forecast implements Serializable {

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("locationKey")
    private String locationKey;

    @JsonProperty("dateTime")
    private String dateTime;

    @JsonProperty("currCondition")
    private Double currCondition;

    @JsonProperty("foreCast")
    private ArrayList<Double> foreCast;

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationKey() {
        return locationKey;
    }

    public void setLocationKey(String locationKey) {
        this.locationKey = locationKey;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public Double getCurrCondition() {
        return currCondition;
    }

    public void setCurrCondition(Double currCondition) {
        this.currCondition = currCondition;
    }

    public ArrayList<Double> getForeCast() {
        return foreCast;
    }

    public void setForeCast(ArrayList<Double> foreCast) {
        this.foreCast = foreCast;
    }
}
