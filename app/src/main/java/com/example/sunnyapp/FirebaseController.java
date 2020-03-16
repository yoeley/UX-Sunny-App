package com.example.sunnyapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

// Most prob will delete seems unnecessary
public class FirebaseController {

    private static final FirebaseController firebaseController = new FirebaseController();

    public FirebaseAuth mAuth;
    public FirebaseFirestore db;
    public FirebaseStorage storage;

    public static FirebaseController getInstance() {
        return firebaseController;
    }

    private FirebaseController() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }
}
