package com.example.sunnyapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;


public class FireBaseController {

    private static final FireBaseController firebaseController = new FireBaseController();

    public FirebaseAuth auth;
    public FirebaseFirestore dB;
    public FirebaseStorage storage;

    /**
     * Singleton of the FireBase controller instance
     * @return The FireBase Controller instance
     */
    public static FireBaseController getInstance() {
        return firebaseController;
    }

    /**
     * Constructor of the FireBase controller
     */
    private FireBaseController() {
        auth = FirebaseAuth.getInstance();
        dB = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }
}
