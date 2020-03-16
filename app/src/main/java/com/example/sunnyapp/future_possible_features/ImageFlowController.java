package com.example.sunnyapp.future_possible_features;

import com.example.sunnyapp.FirebaseController;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ImageFlowController {

    private static final ImageFlowController ourInstance = new ImageFlowController();

    private String mName;
    private String mImageUrl;
    private FirebaseController firebaseController;
    protected FirebaseStorage storage;
    protected StorageReference storageReference;

    public static ImageFlowController getInstance() {
        return ourInstance;
    }

    public ImageFlowController() {
        firebaseController = FirebaseController.getInstance();
        storage = firebaseController.storage;
        storageReference = storage.getReference("uploads");
    }

//    public ImageFlowController(String name, String imageUrl) {
//        if (name.trim().equals("")) {
//            name = "No Name";
//        }
//        mName = name;
//        mImageUrl = imageUrl;
//    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        mImageUrl = imageUrl;
    }

//    public UploadImage uploadImage()
//    {
//        return new UploadImage(mEditTextFileName.getText().toString().trim(),
//                taskSnapshot.getDownloadUrl().toString());
//    }

}
