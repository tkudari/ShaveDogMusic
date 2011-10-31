package com.tejus.shavedogmusic.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.tejus.shavedog.Definitions;
import com.tejus.shavedogmusic.R;
import com.tejus.shavedogmusic.activity.PlayActivity;
import com.tejus.shavedogmusic.utils.Logger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
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
    private DatagramSocket mBroadcastSocket;

    public class ShaveBinder extends Binder {
        public ShaveService getService() {
            return ShaveService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.d( "XXXX", "service created, biatch" );
        mNM = ( NotificationManager ) getSystemService( NOTIFICATION_SERVICE );
        showNotification();
        // setup our request broadcast server:
        new RequestListener().execute( mBroadcastSocket );
        // // this's our generic listener:
        // new RequestListener().execute( mGenericSocket );
    }

    private class RequestListener extends AsyncTask<DatagramSocket, DatagramPacket, Void> {
        @Override
        protected void onProgressUpdate( DatagramPacket... packet ) {
            dealWithReceivedPacket( packet[ 0 ] );
        }

        @Override
        protected Void doInBackground( DatagramSocket... requestSocket ) {
            byte[] buffer = new byte[ Definitions.COMMAND_BUFSIZE ];
            DatagramPacket packet;
            while ( true ) {
                try {
                    packet = new DatagramPacket( buffer, buffer.length );
                    Logger.d( "ShaveService.RequestListener listening on : " + requestSocket[ 0 ].getLocalPort() );
                    requestSocket[ 0 ].receive( packet );
                    Log.d( "XXXX", "Stuff received by Server = " + new String( packet.getData() ) );
                    publishProgress( packet );
                    Log.d( "XXXX", "done with publishProgress" );

                } catch ( IOException e ) {
                    Log.d( "XXXX", "Server: Receive timed out.." );
                }
            }
        }
    }

    private void showNotification() {
        CharSequence text = getText( R.string.shave_service_started );
        Notification notification = new Notification( R.drawable.iconshave, text, System.currentTimeMillis() );
        PendingIntent contentIntent = PendingIntent.getActivity( this, 0, new Intent( this, PlayActivity.class ), 0 );
        notification.setLatestEventInfo( this, getText( R.string.awesomeness ), text, contentIntent );
        mNM.notify( NOTIFICATION, notification );
    }
}
