package com.example.sunnyapp;

/**
 * the info a location where the user is/was
 */
public class LocationInfo {

    private String locationKey = null;
    private String country = null;
    private String city = null;

    public LocationInfo() {}

    public LocationInfo(String locationKey, String country, String city) {
        this.locationKey = locationKey;
        this.country = country;
        this.city = city;
    }

    public String getLocationKey() {
        return locationKey;
    }

    public void setLocationKey(String locationKey) {
        this.locationKey = locationKey;
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
