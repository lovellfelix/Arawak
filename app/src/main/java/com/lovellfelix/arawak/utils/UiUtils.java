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

    public static DateFormat DATE_SHORT_FORMAT = null;

    static {
        // getBestTimePattern() is only available in API 18 and up (Android 4.3 and better)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            DATE_SHORT_FORMAT = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(MainApplication.getContext().getResources().getConfiguration().locale, "d MMM"));
        } else {
            DATE_SHORT_FORMAT = android.text.format.DateFormat.getDateFormat(MainApplication.getContext());
        }
    }

    public static final DateFormat TIME_FORMAT = android.text.format.DateFormat.getTimeFormat(MainApplication.getContext());
    public static final int SIX_HOURS = 21600000; // six hours in milliseconds

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

    static public String easyreadDateTimeString(long timestamp) {

        String outString;

        Date date = new Date(timestamp);
        Calendar calTimestamp = Calendar.getInstance();
        calTimestamp.setTimeInMillis(timestamp);
        Calendar calCurrent = Calendar.getInstance();

        if (calCurrent.getTimeInMillis() - timestamp < SIX_HOURS || calCurrent.get(Calendar.DAY_OF_MONTH) == calTimestamp.get(Calendar.DAY_OF_MONTH)) {
            outString = TIME_FORMAT.format(date);
        } else {
            outString = DATE_SHORT_FORMAT.format(date) + ' ' + TIME_FORMAT.format(date);
        }

        return outString;

    }
}
