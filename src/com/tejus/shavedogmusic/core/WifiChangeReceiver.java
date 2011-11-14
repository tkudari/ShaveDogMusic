package com.tejus.shavedogmusic.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WifiChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive( Context context, Intent i ) {
        Intent intent = new Intent( Definitions.WIFI_STATE_CHANGE).setClass(context, ShaveService.class);
        context.startService( intent );
    }

}
