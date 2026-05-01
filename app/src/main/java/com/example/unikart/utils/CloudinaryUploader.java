package com.example.unikart.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

public class CloudinaryUploader {

    private static final String TAG = "CloudinaryUploader";

    // ─── YOUR CLOUDINARY CREDENTIALS ─────────────────────────────────────────
    // 1. Sign up free at https://cloudinary.com
    // 2. Go to Dashboard — copy Cloud Name and API Key
    // 3. Go to Settings > Upload > Add upload preset
    //    Set "Signing Mode" to UNSIGNED, name it "unikart_products"
    // 4. Replace the values below
    private static final String CLOUD_NAME   = "djbd78zci";   // e.g. "dxyz123abc"
    private static final String UPLOAD_PRESET = "unikart_products"; // unsigned preset name
    // ─────────────────────────────────────────────────────────────────────────

    private static final String UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    /**
     * Uploads an image URI to Cloudinary using an unsigned upload preset.
     * Runs on a background thread, calls back on the main thread.
     */
    public static void upload(Context context, Uri imageUri, UploadCallback callback) {
        if (imageUri == null) {
            callback.onFailure("No image selected");
            return;
        }

        if (CLOUD_NAME.equals("YOUR_CLOUD_NAME")) {
            callback.onFailure("Cloudinary not configured. Set CLOUD_NAME in CloudinaryUploader.java");
            return;
        }

        executor.execute(() -> {
            try {
                // Read and compress image
                byte[] imageBytes = compressImage(context, imageUri);
                if (imageBytes == null) {
                    postFailure(callback, "Could not read image");
                    return;
                }

                Log.d(TAG, "Uploading " + imageBytes.length + " bytes to Cloudinary");

                // Build multipart request
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("upload_preset", UPLOAD_PRESET)
                        .addFormDataPart("file", "product.jpg",
                                RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                        .build();

                Request request = new Request.Builder()
                        .url(UPLOAD_URL)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Cloudinary response [" + response.code() + "]: " + body);

                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(body);
                        String secureUrl = json.getString("secure_url");
                        Log.d(TAG, "Upload success: " + secureUrl);
                        postSuccess(callback, secureUrl);
                    } else {
                        // Parse Cloudinary error message
                        String errorMsg = "Upload failed (" + response.code() + ")";
                        try {
                            JSONObject json = new JSONObject(body);
                            if (json.has("error")) {
                                errorMsg = json.getJSONObject("error").getString("message");
                            }
                        } catch (Exception ignored) {}
                        postFailure(callback, errorMsg);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Upload exception", e);
                postFailure(callback, e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        });
    }

    private static byte[] compressImage(Context context, Uri uri) {
        try {
            // First pass — get dimensions
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream s1 = context.getContentResolver().openInputStream(uri);
            if (s1 == null) return null;
            BitmapFactory.decodeStream(s1, null, opts);
            s1.close();

            // Sample size to keep within 1024x1024
            opts.inSampleSize = calculateSampleSize(opts, 1024, 1024);
            opts.inJustDecodeBounds = false;

            // Second pass — decode
            InputStream s2 = context.getContentResolver().openInputStream(uri);
            if (s2 == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(s2, null, opts);
            s2.close();

            if (bitmap == null) return null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            bitmap.recycle();
            return baos.toByteArray();

        } catch (Exception e) {
            Log.e(TAG, "compressImage failed", e);
            return null;
        }
    }

    private static int calculateSampleSize(BitmapFactory.Options opts, int maxW, int maxH) {
        int h = opts.outHeight, w = opts.outWidth, size = 1;
        if (h > maxH || w > maxW) {
            int hh = h / 2, hw = w / 2;
            while ((hh / size) >= maxH && (hw / size) >= maxW) size *= 2;
        }
        return size;
    }

    private static void postSuccess(UploadCallback cb, String url) {
        mainHandler.post(() -> cb.onSuccess(url));
    }

    private static void postFailure(UploadCallback cb, String error) {
        mainHandler.post(() -> cb.onFailure(error));
    }
}
