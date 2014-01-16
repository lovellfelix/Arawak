package com.lovellfelix.arawak.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.TypedValue;

import com.lovellfelix.arawak.MainApplication;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import com.lovellfelix.arawak.R;
public class UiUtils {

    static public void setPreferenceTheme(Activity a) {
        if (!PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true)) {
            a.setTheme(R.style.Theme_Cnsdark);
        }
    }

    static public int dpToPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, MainApplication.getContext().getResources().getDisplayMetrics());
    }

    static public Bitmap getScaledBitmap(byte[] iconBytes, int sizeInDp) {
        if (iconBytes != null && iconBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
            if (bitmap != null && bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
                int bitmapSizeInDip = UiUtils.dpToPixel(sizeInDp);
                if (bitmap.getHeight() != bitmapSizeInDip) {
                    Bitmap tmp = bitmap;
                    bitmap = Bitmap.createScaledBitmap(tmp, bitmapSizeInDip, bitmapSizeInDip, false);
                    tmp.recycle();
                }

                return bitmap;
            }
        }

        return null;
    }
}
