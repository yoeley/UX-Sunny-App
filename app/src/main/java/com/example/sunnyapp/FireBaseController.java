package com.example.sunnyapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class FireBaseController {

    private static final FireBaseController firebaseController = new FireBaseController();

    public FirebaseAuth auth;
    public FirebaseFirestore dB;
    public FirebaseStorage storage;

    public static FireBaseController getInstance() {
        return firebaseController;
    }

    private FireBaseController() {
        auth = FirebaseAuth.getInstance();
        dB = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }
}
