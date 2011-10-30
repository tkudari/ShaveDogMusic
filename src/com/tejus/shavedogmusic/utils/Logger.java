package com.tejus.shavedogmusic.utils;

import android.util.Log;

public class Logger {

   
    private static final String TAG = "xxxShaveDogMusic";
   
    static boolean isInDebugMode() {
        return Definitions.IS_IN_DEBUG_MODE;
    }

    // in decreasing order of priority:

    public static void e( String message ) {
        Log.e( TAG, message );        
    }

    public static void w( String message ) {
        Log.w( TAG, message );
    }

    public static void i( String message ) {
        Log.i( TAG, message );
    }

    public static void d( String message ) {
        // if the app's not in debug mode, we don't show Log statements that are
        // at 'debug' level or lower
        if ( isInDebugMode() )
            Log.d( TAG, message );
    }

    public static void v( String message ) {
        Log.v( TAG, message );
    }

}
