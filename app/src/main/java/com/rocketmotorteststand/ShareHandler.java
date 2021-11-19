package com.rocketmotorteststand;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;


public class ShareHandler {

    public static Bitmap takeScreenshot(View rootView) {
        rootView.setDrawingCacheEnabled(true);
        return rootView.getDrawingCache();
    }


    public static void share(Bitmap bitmap, Context ctx){
        String pathofBmp=
                MediaStore.Images.Media.insertImage(ctx.getContentResolver(),
                        bitmap,"MotorTestStand", null);
        Uri uri = Uri.parse(pathofBmp);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Star App");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        ctx.startActivity(Intent.createChooser(shareIntent, "MotorTestStand has shared with you some info"));
    }
}
