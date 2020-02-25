package com.example.sunnyapp;

import android.content.ContentResolver;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.google.firebase.storage.StorageReference;

public class UploadImage extends ImageFlowController {

    public UploadImage(){
    // Required empty constructor..
    }

    public UploadImage(String name, String imageUrl) {
        if (name.trim().equals("")) {
            name = "No Name";
        }
    }
}
