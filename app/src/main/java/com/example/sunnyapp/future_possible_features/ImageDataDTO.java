package com.example.sunnyapp.future_possible_features;

import com.google.firebase.Timestamp;

import java.util.Date;

public class ImageDataDTO {

    // Getting from DB
    private Timestamp dateUploaded;
    private String imageUrl;
    private String name;

    // Converted from timestamp to Date.
//    private Date date;

    public ImageDataDTO(){}

    public ImageDataDTO(Timestamp dateUploaded, String imageUrl, String name){
        this.dateUploaded = dateUploaded;
        this.imageUrl = imageUrl;
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public Timestamp getTimestamp() {
//        return dateUploaded;
//    }

    public void setTimestamp(Timestamp timestamp) {
        this.dateUploaded = timestamp;
    }

    public Date getDate() {
        return dateUploaded.toDate();
    }
}
