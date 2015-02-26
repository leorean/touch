package com.example.touch;

import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class Utility {
    // debugging switch
    public static boolean DEBUG = true;

    // log
    public static void logging(String tag, String text) {
        Log.d(tag, "+++>>  " + text );
    }

    // toast for android
    public static void toastShort(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
    
    public static byte[] cutData(byte[] buffer, int bytes) {
        //Log.d(TAG, "cutData()");
        ByteArrayBuffer bab = new ByteArrayBuffer(bytes);

        bab.append(buffer, 0, bytes);
        return bab.toByteArray();
    }
}
