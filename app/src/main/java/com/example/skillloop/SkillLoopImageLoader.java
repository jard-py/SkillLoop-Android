package com.example.skillloop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;

/** Loads local profile photographs and authenticated Firebase Storage images without blocking UI. */
public final class SkillLoopImageLoader {
    private SkillLoopImageLoader() { }

    public static void load(Context context, String source, ImageView target, Runnable onLoaded) {
        if (source == null || source.trim().isEmpty() || target == null) return;
        String normalized = source.trim();
        target.setTag(normalized);
        if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("gs://")) {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) return;
                FirebaseStorage.getInstance().getReferenceFromUrl(normalized)
                        .getBytes(5L * 1024L * 1024L)
                        .addOnSuccessListener(bytes -> {
                            if (!normalized.equals(target.getTag())) return;
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            if (bitmap != null) {
                                target.setImageBitmap(bitmap);
                                if (onLoaded != null) onLoaded.run();
                            }
                        });
            } catch (Exception ignored) { }
            return;
        }
        try {
            Bitmap bitmap;
            if (normalized.startsWith("content://") || normalized.startsWith("file://")) {
                try (java.io.InputStream input = context.getContentResolver().openInputStream(Uri.parse(normalized))) {
                    bitmap = input == null ? null : BitmapFactory.decodeStream(input);
                }
            } else {
                File file = new File(normalized);
                bitmap = file.exists() ? BitmapFactory.decodeFile(normalized) : null;
            }
            if (bitmap != null && normalized.equals(target.getTag())) {
                target.setImageBitmap(bitmap);
                if (onLoaded != null) onLoaded.run();
            }
        } catch (Exception ignored) { }
    }
}
