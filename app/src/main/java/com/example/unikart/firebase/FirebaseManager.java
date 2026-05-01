package com.example.unikart.firebase;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    // Explicit bucket URL — avoids SDK picking wrong default on new Firebase projects
    private static final String STORAGE_BUCKET = "gs://unikart-38d0e.firebasestorage.app";

    private static FirebaseManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseStorage tempStorage;
        try {
            tempStorage = FirebaseStorage.getInstance(STORAGE_BUCKET);
            Log.d(TAG, "Storage initialized with explicit bucket: " + STORAGE_BUCKET);
        } catch (Exception e) {
            Log.w(TAG, "Explicit bucket failed, falling back to default: " + e.getMessage());
            tempStorage = FirebaseStorage.getInstance();
        }
        storage = tempStorage;
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseAuth getAuth() { return auth; }
    public FirebaseFirestore getFirestore() { return firestore; }
    public FirebaseStorage getStorage() { return storage; }

    public StorageReference getStorageReference() {
        return storage.getReference();
    }

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}
