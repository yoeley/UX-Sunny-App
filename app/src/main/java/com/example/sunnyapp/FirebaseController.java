package com.example.sunnyapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


class FirebaseController {

    private static final FirebaseController firebaseController = new FirebaseController();

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    static FirebaseController getInstance() {
        return firebaseController;
    }

    private FirebaseController() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }
}
