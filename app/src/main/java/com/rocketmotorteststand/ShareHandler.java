package com.rocketmotorteststand;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @description: Shares a screenshot of the current screen with another application (mail,
 * messaging...). The screenshot is drawn into a bitmap, written as a PNG in the private cache
 * and exposed through the FileProvider with a read permission granted to the receiver.
 * @author: boris.dureau@neuf.fr
 **/
public class ShareHandler {
    private static final String TAG = "ShareHandler";

    /**
     * Takes a screenshot of the given view and opens the share sheet.
     * Fixes: the receiving app was not granted read access to the image, the file name
     * contained ':' characters (not allowed on shared storage) and a PNG was saved with a
     * .jpeg extension. getDrawingCache() (deprecated) is replaced by drawing on a Canvas.
     */
    public static void takeScreenShot(View view, Context ctx) {
        if (view.getWidth() <= 0 || view.getHeight() <= 0)
            return;
        try {
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, png);
            bitmap.recycle();

            String date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.US).format(new Date());
            Uri uri = FileExportHelper.saveForSharing(ctx, "TestStandCurve-" + date + ".png", png.toByteArray());
            if (uri == null) {
                Toast.makeText(ctx, R.string.export_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            FileExportHelper.share(ctx, uri, "image/png", "MotorTestStand has shared with you some info");
        } catch (OutOfMemoryError | IllegalArgumentException e) {
            Log.e(TAG, "Cannot take the screenshot", e);
            Toast.makeText(ctx, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shares an image file already written in the share cache folder.
     */
    public static void shareScreenShot(Uri imageUri, Context ctx) {
        FileExportHelper.share(ctx, imageUri, "image/png", "MotorTestStand has shared with you some info");
    }
}
