package com.tejus.shavedogmusic.core;




import com.tejus.shavedogmusic.R;
import com.tejus.shavedogmusic.activity.PlayActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ShaveService extends Service {

    @Override
    public IBinder onBind( Intent arg0 ) {
        return mBinder;
    }
    
    private final IBinder mBinder = new ShaveBinder();
    private NotificationManager mNM;
    private int NOTIFICATION = R.string.shave_service_started;

    
    public class ShaveBinder extends Binder {
        public ShaveService getService() {
            return ShaveService.this;
        }
    }
    
    @Override
    public void onCreate() {
        Log.d( "XXXX", "service created, biatch" );
        mNM = ( NotificationManager ) getSystemService( NOTIFICATION_SERVICE );
//        showNotification();
//        setUpNetworkStuff();
//        // setup our request broadcast server:
//        new RequestListener().execute( mBroadcastSocket );
//        // this's our generic listener:
//        new RequestListener().execute( mGenericSocket );
    }

    
    private void showNotification() {
        CharSequence text = getText( R.string.shave_service_started );
        Notification notification = new Notification( R.drawable.iconshave, text, System.currentTimeMillis() );
        PendingIntent contentIntent = PendingIntent.getActivity( this, 0, new Intent( this, PlayActivity.class ), 0 );
        notification.setLatestEventInfo( this, getText( R.string.this_the_string ), text, contentIntent );
        mNM.notify( NOTIFICATION, notification );
    }
}
