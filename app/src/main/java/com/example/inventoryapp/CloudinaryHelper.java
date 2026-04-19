package com.example.inventoryapp;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class to upload images to Cloudinary using unsigned upload presets.
 *
 * ⚠️  IMPORTANT: Replace CLOUD_NAME and UPLOAD_PRESET with your own values
 *     from your Cloudinary dashboard.
 *
 *     - Cloud Name:    Dashboard → Product Environment Credentials
 *     - Upload Preset: Settings (⚙️) → Upload → Upload presets
 */
public class CloudinaryHelper {

    private static final String TAG = "CloudinaryHelper";

    // ============================================================
    // ⬇️  REPLACE THESE WITH YOUR OWN CLOUDINARY CREDENTIALS  ⬇️
    // ============================================================
    private static final String CLOUD_NAME    = "dki7ttfhi";
    private static final String UPLOAD_PRESET = "my_image_preset";
    // ============================================================

    private static final String UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Callback interface for upload results.
     */
    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String errorMessage);
    }

    /**
     * Uploads an image from a content URI to Cloudinary.
     * Runs on a background thread; callbacks are delivered on the main thread.
     *
     * @param context  Android context (for ContentResolver)
     * @param imageUri URI of the image to upload (from gallery/camera)
     * @param callback Callback to receive the secure URL or error
     */
    public static void uploadImage(Context context, Uri imageUri, UploadCallback callback) {
        executor.execute(() -> {
            try {
                // Read image bytes from the content URI
                byte[] imageBytes = getBytes(context, imageUri);
                if (imageBytes == null) {
                    postFailure(callback, "Failed to read image data");
                    return;
                }

                // Build multipart form data
                String boundary = "----CloudinaryBoundary" + System.currentTimeMillis();
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                URL url = new URL(UPLOAD_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);

                DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());

                // Add upload_preset field
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"" + lineEnd);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(UPLOAD_PRESET + lineEnd);

                // Add file field
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"" + lineEnd);
                outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
                outputStream.writeBytes(lineEnd);
                outputStream.write(imageBytes);
                outputStream.writeBytes(lineEnd);

                // End boundary
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                outputStream.flush();
                outputStream.close();

                // Read response
                int responseCode = conn.getResponseCode();
                InputStream responseStream;
                if (responseCode >= 200 && responseCode < 300) {
                    responseStream = conn.getInputStream();
                } else {
                    responseStream = conn.getErrorStream();
                }

                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int length;
                while ((length = responseStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String responseBody = result.toString("UTF-8");
                responseStream.close();
                conn.disconnect();

                if (responseCode >= 200 && responseCode < 300) {
                    // Parse the JSON response to get the secure URL
                    JSONObject json = new JSONObject(responseBody);
                    String secureUrl = json.getString("secure_url");
                    Log.d(TAG, "Upload success: " + secureUrl);
                    postSuccess(callback, secureUrl);
                } else {
                    Log.e(TAG, "Upload failed: " + responseCode + " - " + responseBody);
                    postFailure(callback, "Upload failed (HTTP " + responseCode + ")");
                }

            } catch (Exception e) {
                Log.e(TAG, "Upload exception", e);
                postFailure(callback, "Upload error: " + e.getMessage());
            }
        });
    }

    /**
     * Read all bytes from a content URI.
     */
    private static byte[] getBytes(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;

            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Error reading URI bytes", e);
            return null;
        }
    }

    private static void postSuccess(UploadCallback callback, String url) {
        mainHandler.post(() -> callback.onSuccess(url));
    }

    private static void postFailure(UploadCallback callback, String error) {
        mainHandler.post(() -> callback.onFailure(error));
    }
}
