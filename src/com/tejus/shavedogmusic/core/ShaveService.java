package com.tejus.shavedogmusic.core;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import com.tejus.shavedogmusic.core.Definitions;
import com.tejus.shavedogmusic.R;
import com.tejus.shavedogmusic.activity.PlayActivity;
import com.tejus.shavedogmusic.utils.Logger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class ShaveService extends Service {

    private final IBinder mBinder = new ShaveBinder();
    private NotificationManager mNM;
    private int NOTIFICATION = R.string.shave_service_started;
    private DatagramSocket mSearchSocket, mGenericSocket, mTestSocket;;
    WifiManager wifi;
    DhcpInfo dhcp;
    ShaveFinder mFinder;
    public static HashMap<String, String> peerMap = new HashMap<String, String>();
    public static HashMap<String, Integer> uploadPortMap = new HashMap<String, Integer>();
    public static HashMap<String, Integer> downloadPortMap = new HashMap<String, Integer>();
    public static ArrayList<Integer> alreadyAssignedPorts = new ArrayList<Integer>();
    public static HashMap<String, ArrayList<String>> peerMusicHistory = new HashMap<String, ArrayList<String>>();

    LocalMusicManager localMusicManager = new LocalMusicManager();

    public class ShaveBinder extends Binder {
        public ShaveService getService() {
            return ShaveService.this;
        }
    }

    @Override
    public IBinder onBind( Intent arg0 ) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Logger.d( "ShaveService created, biatch" );
        mNM = ( NotificationManager ) getSystemService( NOTIFICATION_SERVICE );
        showNotification();
        setUpNetworkStuff();
        // setup our request broadcast server:
        new RequestListener().execute( mSearchSocket );
        // // this's our generic listener:
        new RequestListener().execute( mGenericSocket );
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {
        Logger.d( "ShaveService Received start id " + startId + ": " + intent );
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mNM.cancel( NOTIFICATION );
        Toast.makeText( this, R.string.shave_service_stopped, Toast.LENGTH_SHORT ).show();
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
                    Logger.d( "Stuff received by Server = " + new String( packet.getData() ) );
                    publishProgress( packet );

                } catch ( IOException e ) {
                    Logger.d( "Server: Receive timed out.." );
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

    void setUpNetworkStuff() {
        initNetworkStuff();
        try {
            // temp. sockets used only here, that's why the ridiculous names:
            DatagramSocket socket1 = new DatagramSocket( Definitions.SEARCH_SERVER_PORT );
            socket1.setBroadcast( true );
            socket1.setSoTimeout( Definitions.SOCKET_TIMEOUT );
            mSearchSocket = socket1;

            DatagramSocket socket2 = new DatagramSocket( Definitions.GENERIC_SERVER_PORT );
            socket2.setBroadcast( true );
            socket2.setSoTimeout( Definitions.SOCKET_TIMEOUT );
            mGenericSocket = socket2;

            DatagramSocket socket3 = new DatagramSocket( Definitions.TEST_SERVER_PORT );
            socket3.setBroadcast( true );
            socket3.setSoTimeout( Definitions.SOCKET_TIMEOUT );
            mTestSocket = socket3;

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    void initNetworkStuff() {
        wifi = ( WifiManager ) this.getSystemService( Context.WIFI_SERVICE );
        if ( !wifi.isWifiEnabled() ) {
            Toast.makeText( this, R.string.no_wifi, Toast.LENGTH_LONG ).show();
        }
        dhcp = wifi.getDhcpInfo();
        getOurIp();
    }

    private InetAddress getOurIp() {
        Definitions.IP_ADDRESS_INT = dhcp.ipAddress;
        int ourIp = Definitions.IP_ADDRESS_INT;
        byte[] quads = new byte[ 4 ];
        try {
            for ( int k = 0; k < 4; k++ ) {
                quads[ k ] = ( byte ) ( ( ourIp >> k * 8 ) & 0xFF );
            }

            Definitions.IP_ADDRESS_INETADDRESS = InetAddress.getByAddress( quads );
            return InetAddress.getByAddress( quads );
        } catch ( UnknownHostException e ) {
            e.printStackTrace();
        }
        return null;

    }

    // Searches our subnet & our parent - subnet
    private class ShaveFinder extends AsyncTask<Void, DatagramPacket, Void> {
        @Override
        protected void onProgressUpdate( DatagramPacket... packet ) {
            dealWithReceivedPacket( packet[ 0 ] );
        }

        @Override
        protected Void doInBackground( Void... shavealicious ) {
            String ourIp = getOurIp().getHostAddress();
            String subnet = ( String ) ourIp.subSequence( 0, ourIp.lastIndexOf( "." ) );
            String parentSubnet = ( String ) ourIp.subSequence( 0, subnet.lastIndexOf( "." ) );
            // search our subnet first:
            for ( int i = 0; i < 256; i++ ) {
                try {
                    if ( isCancelled() ) {
                        break;
                    }
                    String destinationAddress = subnet + "." + String.valueOf( i );
                    String searchString = Definitions.DISCOVER_PING + ":" + getOurUserName() + ":" + getOurIp().toString().replace( "/", "" )
                            + Definitions.END_DELIM;

                    Logger.d( "sending DISCOVER to = " + destinationAddress );
                    DatagramPacket sendPacket = new DatagramPacket( searchString.getBytes(), searchString.getBytes().length,
                            InetAddress.getByName( destinationAddress ), Definitions.SEARCH_SERVER_PORT );

                    mTestSocket.send( sendPacket );
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }

            // search other subnets under our parent's subnet:
            /*
             * for ( int j = 0; j < 256; j++ ) { String parentAddress =
             * parentSubnet + "." + String.valueOf( j ); for ( int i = 0; i <
             * 256; i++ ) { try { if ( isCancelled() ) { break; } String
             * destinationAddress = parentAddress + "." + String.valueOf( i );
             * String searchString = Definitions.DISCOVER_PING + ":" +
             * getOurUserName() + ":" + getOurIp().toString().replace( "/", "" )
             * + Definitions.END_DELIM;
             * 
             * Logger.d( "sending DISCOVER to = " + destinationAddress );
             * DatagramPacket sendPacket = new DatagramPacket(
             * searchString.getBytes(), searchString.getBytes().length,
             * InetAddress.getByName( destinationAddress ),
             * Definitions.TEST_SERVER_PORT );
             * 
             * mTestSocket.send( sendPacket ); } catch ( Exception e ) {
             * e.printStackTrace(); } } if ( isCancelled() ) { Logger.d(
             * "TestSearchMethod: stopping search, since a friend replied.. " );
             * break; } }
             */

            return null;
        }
    }

    private String getOurUserName() {
        SharedPreferences settings = getSharedPreferences( Definitions.credsPrefFile, Context.MODE_PRIVATE );
        return settings.getString( Definitions.prefUserName, Definitions.defaultUserName );
    }

    public void testPopulateList() {
        mFinder = new ShaveFinder();
        mFinder.execute();
    }

    // Request processor:
    private void dealWithReceivedPacket( DatagramPacket packet ) {

        String words[] = new String[ Definitions.COMMAND_WORD_LENGTH + 1 ];
        int wordCounter = 0;
        String senderAddress, userName;
        String command = new String( packet.getData() );
        Logger.d( "command here = " + command );

        StringTokenizer strTok = new StringTokenizer( command, Definitions.COMMAND_DELIM );
        while ( strTok.hasMoreTokens() && wordCounter <= Definitions.COMMAND_WORD_LENGTH ) {
            words[ wordCounter ] = strTok.nextToken();
            Logger.d( "word here = " + words[ wordCounter ] );
            ++wordCounter;
        }
        for ( String word : words )
            Logger.d( "word = " + word );

        userName = words[ 1 ];
        senderAddress = words[ 2 ];

        if ( words[ 0 ].equals( Definitions.DISCOVER_PING ) ) {
            Logger.d( "DISCOVER_PING received...." );
            Logger.d( "cleanedup = " + cleanThisStringUp( words[ 2 ] ) );
            if ( cleanThisStringUp( words[ 2 ] ).equals( cleanThisStringUp( Definitions.IP_ADDRESS_INETADDRESS.getHostAddress() ) ) ) {
                Logger.d( "yep, it's ours" );
            } else {
                discoverPingReceived( new String[] {
                    words[ 1 ],
                    cleanThisStringUp( words[ 2 ] )
                } );
            }
        }

        if ( words[ 0 ].equals( Definitions.DISCOVER_ACK ) ) {
            Logger.d( "DISCOVER_ACK received...." );
            Logger.d( "cleanedup DISCOVER_ACK = " + cleanThisStringUp( words[ 2 ] ) );

            discoverAckReceived( new String[] {
                words[ 1 ],
                cleanThisStringUp( words[ 2 ] ),
                words[ 3 ]
            } );
        }

        if ( words[ 0 ].equals( Definitions.PLAY_REQUEST ) ) {
            Logger.d( "PLAY_REQUEST received from : " + userName );
            playRequestReceived( userName, senderAddress );

        }

    }

    private void discoverAckReceived( String[] senderDetails ) {
        String userName = senderDetails[ 1 ];
        String senderAddress = senderDetails[ 2 ];
        String downloadPort = senderDetails[ 0 ];
        Logger.d( "words in discoverAckReceived = " + senderDetails[ 0 ] + ", " + senderDetails[ 1 ] + ", " + senderDetails[ 2 ] );

        peerMap.put( userName, senderAddress );
        Logger.d( "ShaveService.discoverAckReceived: peerMap = " + peerMap.toString() );
        downloadPortMap.put( userName, Integer.parseInt( downloadPort ) );
        Toast toast = Toast.makeText( this, userName + " added!", Toast.LENGTH_SHORT );
        toast.show();
    }

    private void discoverPingReceived( String[] senderDetails ) {
        // add the requester to our peer - list and reply back
        String userName = senderDetails[ 0 ];
        String senderAddress = senderDetails[ 1 ];
        String uploadPort = getUnassignedPort();
        uploadPortMap.put( userName, Integer.parseInt( uploadPort ) );
        alreadyAssignedPorts.add( Integer.parseInt( uploadPort ) );
        peerMap.put( userName, senderAddress );
        Logger.d( "ShaveService.discoverPingReceived: peerMap = " + peerMap.toString() );
        sendMessage( senderAddress, Definitions.DISCOVER_ACK + ":" + uploadPort );

        Toast toast = Toast.makeText( this, userName + " added!", Toast.LENGTH_SHORT );
        toast.show();
    }

    private void playRequestReceived( String userName, String address ) {
        String songPath;
        try {
            songPath = getRecommendedSong( userName );
            long songSize = getFileSize( songPath );
            Logger.d( "ShaveService.playRequestReceived: uploading song: " + songPath + ", to: " + userName );
            uploadSong( userName, songPath, songSize );

        } catch ( Exception e ) {
            Toast.makeText( this, R.string.cannot_serve + userName, Toast.LENGTH_SHORT );
            e.printStackTrace();
        }

    }

    private void uploadSong( String userName, String songPath, long songSize ) {
        int destinationPort = uploadPortMap.get( userName );
        String destinationAddress = peerMap.get( userName );
        new Uploader( destinationAddress, destinationPort, songPath, songSize ).execute( getApplicationContext() );

    }

    private long getFileSize( String songPath ) {
        File tempFile = new File( songPath );
        if ( tempFile.exists() ) {
            return tempFile.length();
        } else {
            return -1;
        }

    }

    // TODO: improve exception handling.
    private String getRecommendedSong( String userName ) throws Exception {

        ArrayList<String> localMusicList = localMusicManager.getLatestLocalList();
        // 'peerMusicHistory' for this userName is empty, since we're serving
        // stuff up for the first time, return our first local song found:
        if ( peerMusicHistory.get( userName ).size() == 0 ) {
            if ( localMusicList.size() > 0 ) {
                return localMusicList.get( 0 );
            } else {
                throw new Exception( "ShaveService.getRecommendedSong: Cannot read local Music listing!" );
            }

        } else { // We have served userName before.
            return localMusicManager.requestSongForUser( userName, peerMusicHistory.get( userName ) );
        }

    }

    // return the last assigned port + '5' for now.
    private String getUnassignedPort() {
        String port;
        if ( alreadyAssignedPorts.size() == 0 ) {
            port = String.valueOf( Definitions.TRANSACTION_PORT_START );
        } else {
            port = String.valueOf( alreadyAssignedPorts.get( alreadyAssignedPorts.size() - 1 ) + 5 );
        }

        Logger.d( "ShaveService.getUnassignedPort: returning = " + port );
        return port;

    }

    String cleanThisStringUp( String string ) {
        return string.replace( "\\?", "" ).replace( "*", "" ).replace( "//", "" );
    }

    public void sendMessage( String destinationAddress, String message ) {

        String sendMessage = message + ":" + getOurUserName() + ":" + getOurIp().toString().replace( "/", "" ) + Definitions.END_DELIM;
        byte[] testArr = sendMessage.getBytes();

        Logger.d( "sendMessage = " + sendMessage + ", len = " + sendMessage.length() );
        Logger.d( "testarr = " + testArr.toString() + ", len = " + testArr.length );
        try {
            Logger.d( "destination address = " + InetAddress.getByName( destinationAddress ) );
            DatagramPacket sendPacket = new DatagramPacket( sendMessage.getBytes(), sendMessage.getBytes().length, InetAddress.getByName( destinationAddress ),
                    Definitions.GENERIC_SERVER_PORT );
            mGenericSocket.send( sendPacket );
        } catch ( Exception e ) {
            e.printStackTrace();
        }

    }

}
