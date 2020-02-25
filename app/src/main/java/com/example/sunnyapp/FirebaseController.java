package com.example.sunnyapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

// Most prob will delete seems unnecessary
class FirebaseController {

    private static final FirebaseController firebaseController = new FirebaseController();

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseStorage storage;

    static FirebaseController getInstance() {
        return firebaseController;
    }

    private FirebaseController() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }
}
