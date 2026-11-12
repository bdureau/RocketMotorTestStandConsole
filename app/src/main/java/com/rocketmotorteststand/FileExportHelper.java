package com.rocketmotorteststand;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @description: Saves and shares the files produced by the application (CSV, RASP .eng, zip,
 * screenshots) in a way that works on every supported Android version:
 * - saved files go to Download/RocketMotorTestStand, through MediaStore on Android 10+ (direct
 *   writes to the public Download folder are not allowed any more) and through the File API
 *   on older versions (WRITE_EXTERNAL_STORAGE permission);
 * - shared files are written to the private cache and exposed with a FileProvider URI, and
 *   the receiving application is granted read access to it.
 * @author: boris.dureau@neuf.fr
 **/
public final class FileExportHelper {
    private static final String TAG = "FileExportHelper";

    /** Sub folder of the Download folder where the files are saved. */
    public static final String SUB_DIR = "RocketMotorTestStand";
    /** Sub folder of the cache used for the shared files (declared in res/xml/file_path.xml). */
    private static final String SHARE_DIR = "share";

    private FileExportHelper() {
        // Utility class: no instances
    }

    /**
     * Replaces the characters that are not allowed in file names (spaces, slashes, colons...)
     * with an underscore.
     */
    public static String sanitizeFileName(String name) {
        return name.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    /**
     * Returns the MIME type matching the file extension.
     */
    public static String mimeTypeFor(String fileName) {
        String lower = fileName.toLowerCase(java.util.Locale.US);
        if (lower.endsWith(".csv"))
            return "text/csv";
        if (lower.endsWith(".zip"))
            return "application/zip";
        if (lower.endsWith(".png"))
            return "image/png";
        return "text/plain"; // .eng and other text files
    }

    /**
     * Saves a file in Download/RocketMotorTestStand.
     *
     * @return the path shown to the user, or null if the file could not be written
     */
    public static String saveToDownloads(Context ctx, String fileName, byte[] data) {
        String mimeType = mimeTypeFor(fileName);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = ctx.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/" + SUB_DIR);
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null)
                    return null;
                try (OutputStream out = resolver.openOutputStream(uri)) {
                    if (out == null) {
                        resolver.delete(uri, null, null);
                        return null;
                    }
                    out.write(data);
                } catch (IOException e) {
                    resolver.delete(uri, null, null);
                    throw e;
                }
                values.clear();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), SUB_DIR);
                if (!dir.exists() && !dir.mkdirs())
                    return null;
                try (FileOutputStream out = new FileOutputStream(new File(dir, fileName))) {
                    out.write(data);
                }
            }
            return Environment.DIRECTORY_DOWNLOADS + "/" + SUB_DIR + "/" + fileName;
        } catch (IOException | SecurityException | IllegalArgumentException | IllegalStateException e) {
            Log.e(TAG, "Cannot save " + fileName, e);
            return null;
        }
    }

    /**
     * Writes a file in the private cache so that it can be shared.
     *
     * @return the FileProvider URI of the file, or null on error
     */
    public static Uri saveForSharing(Context ctx, String fileName, byte[] data) {
        try {
            File dir = new File(ctx.getCacheDir(), SHARE_DIR);
            if (!dir.exists() && !dir.mkdirs())
                return null;
            File file = new File(dir, fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(data);
            }
            return FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".provider", file);
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "Cannot prepare " + fileName + " for sharing", e);
            return null;
        }
    }

    /**
     * Opens the Android share sheet for a file. The receiving application is granted read
     * access to the URI (without it, it could not open the file).
     */
    public static void share(Context ctx, Uri uri, String mimeType, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setClipData(ClipData.newRawUri("", uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, "Share File");
        if (!(ctx instanceof android.app.Activity))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, "No App Available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Builds a zip archive in memory from file names and contents.
     */
    public static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
