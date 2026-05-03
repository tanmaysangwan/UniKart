package com.example.unikart.firebase;

import android.util.Log;

import com.example.unikart.models.User;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.FCMTokenManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {

    private static final String TAG = "AuthRepository";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public interface AuthCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }

    public AuthRepository() {
        FirebaseManager manager = FirebaseManager.getInstance();
        this.auth = manager.getAuth();
        this.firestore = manager.getFirestore();
    }

    /**
     * Returns null if config is valid, or an error string if the google-services.json is a placeholder.
     */
    private String checkFirebaseConfig() {
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            FirebaseOptions options = app.getOptions();
            String apiKey = options.getApiKey();
            if (apiKey == null || apiKey.contains("Dummy") || apiKey.contains("Replace") || apiKey.length() < 30) {
                return "Firebase API key is a placeholder. Replace app/google-services.json with the real file from Firebase Console.";
            }
            String projectId = options.getProjectId();
            if (projectId == null || projectId.equals("unikart-project")) {
                return "Firebase project ID is a placeholder. Replace app/google-services.json with the real file from Firebase Console.";
            }
        } catch (Exception e) {
            return "Firebase not initialized: " + e.getMessage();
        }
        return null;
    }

    public void registerUser(String name, String email, String studentId, String password, AuthCallback callback) {
        String configError = checkFirebaseConfig();
        if (configError != null) {
            Log.e(TAG, "Config error: " + configError);
            callback.onFailure(configError);
            return;
        }

        if (!Constants.isValidUniversityEmail(email)) {
            callback.onFailure("Please use a valid university email address");
            return;
        }

        Log.d(TAG, "Attempting registration for: " + email);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        Log.d(TAG, "User created: " + firebaseUser.getUid());
                        // Send verification email — don't block registration on this
                        firebaseUser.sendEmailVerification()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Verification email sent");
                                    } else {
                                        Log.w(TAG, "Verification email failed (non-critical)");
                                    }
                                    createUserDocument(firebaseUser.getUid(), name, email, studentId, callback);
                                });
                    } else {
                        callback.onFailure("User creation returned null — try again");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Registration failed", e);
                    callback.onFailure(parseAuthError(e));
                });
    }

    private void createUserDocument(String uid, String name, String email, String studentId, AuthCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("studentId", studentId);
        userData.put("rating", 0.0);
        userData.put("reviewCount", 0);
        userData.put("createdAt", System.currentTimeMillis());

        firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created for: " + uid);
                    // Set FCM token for new user
                    FCMTokenManager.refreshAndSaveToken(uid);
                    callback.onSuccess("Account created! A verification email has been sent.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore write failed", e);
                    // Auth succeeded — still let user in, Firestore can be retried
                    // Set FCM token anyway
                    FCMTokenManager.refreshAndSaveToken(uid);
                    callback.onSuccess("Account created! (Profile save failed — will retry on next login)");
                });
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        String configError = checkFirebaseConfig();
        if (configError != null) {
            Log.e(TAG, "Config error: " + configError);
            callback.onFailure(configError);
            return;
        }

        if (!Constants.isValidUniversityEmail(email)) {
            callback.onFailure("Please use a valid university email address");
            return;
        }

        Log.d(TAG, "Attempting login for: " + email);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Login successful");
                    // Save/refresh FCM token so this device receives push notifications
                    FirebaseUser loggedInUser = authResult.getUser();
                    if (loggedInUser != null) {
                        FCMTokenManager.refreshAndSaveToken(loggedInUser.getUid());
                    }
                    callback.onSuccess("Login successful");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Login failed", e);
                    callback.onFailure(parseAuthError(e));
                });
    }

    public void getUserData(String uid, UserCallback callback) {
        firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            // Document exists but couldn't deserialize — build from raw fields
                            User fallback = new User();
                            fallback.setUid(uid);
                            fallback.setName(documentSnapshot.getString("name") != null ? documentSnapshot.getString("name") : "Student");
                            fallback.setEmail(documentSnapshot.getString("email") != null ? documentSnapshot.getString("email") : "");
                            fallback.setStudentId(documentSnapshot.getString("studentId") != null ? documentSnapshot.getString("studentId") : "");
                            callback.onSuccess(fallback);
                        }
                    } else {
                        // No Firestore doc — build minimal user from Auth
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            User fallback = new User();
                            fallback.setUid(firebaseUser.getUid());
                            fallback.setEmail(firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");
                            fallback.setName(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Student");
                            fallback.setStudentId("");
                            callback.onSuccess(fallback);
                        } else {
                            callback.onFailure("User data not found");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getUserData failed", e);
                    // Firestore failed — still let user in with Auth data
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser != null) {
                        User fallback = new User();
                        fallback.setUid(firebaseUser.getUid());
                        fallback.setEmail(firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");
                        fallback.setName("Student");
                        fallback.setStudentId("");
                        callback.onSuccess(fallback);
                    } else {
                        callback.onFailure("Failed to fetch user data: " + e.getMessage());
                    }
                });
    }

    private String parseAuthError(Exception e) {
        String raw = e.getMessage();
        if (raw == null) return "Unknown error";

        // Surface the raw message always — also map common codes
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            Log.e(TAG, "FirebaseAuthException code: " + code);
            switch (code) {
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Email already registered. Try logging in.";
                case "ERROR_WEAK_PASSWORD":
                    return "Password must be at least 6 characters.";
                case "ERROR_INVALID_EMAIL":
                    return "Invalid email address.";
                case "ERROR_USER_NOT_FOUND":
                    return "No account found with this email.";
                case "ERROR_WRONG_PASSWORD":
                    return "Incorrect password.";
                case "ERROR_USER_DISABLED":
                    return "This account has been disabled.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Too many attempts. Try again later.";
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return "Email/Password sign-in is not enabled in Firebase Console.\nGo to Authentication > Sign-in method > Email/Password > Enable.";
                case "ERROR_INVALID_API_KEY":
                    return "Invalid API key. Replace app/google-services.json with the real file from Firebase Console.";
                default:
                    return "Auth error [" + code + "]: " + raw;
            }
        }

        // Fallback: return raw message so nothing is hidden
        if (raw.contains("API key not valid") || raw.contains("INVALID_API_KEY")) {
            return "Invalid API key — google-services.json is a placeholder.\nReplace it with the real file from Firebase Console.";
        }
        if (raw.contains("CONFIGURATION_NOT_FOUND") || raw.contains("PROJECT_NOT_FOUND")) {
            return "Firebase project not found. Check google-services.json project_id.";
        }
        if (raw.contains("EMAIL_NOT_FOUND")) {
            return "No account found with this email.";
        }
        if (raw.contains("INVALID_PASSWORD")) {
            return "Incorrect password.";
        }
        if (raw.contains("EMAIL_EXISTS")) {
            return "Email already registered. Try logging in.";
        }
        if (raw.contains("OPERATION_NOT_ALLOWED")) {
            return "Email/Password sign-in is not enabled.\nFirebase Console > Authentication > Sign-in method > Email/Password > Enable.";
        }

        return raw; // Always show the real error
    }

    public void logout() {
        // Clear FCM token so this device stops receiving notifications after logout
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid != null) {
            FCMTokenManager.clearToken(uid);
        }
        auth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}
