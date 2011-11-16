package com.tejus.shavedogmusic.activity;

import com.tejus.shavedogmusic.R;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tejus.shavedogmusic.core.Definitions;
import com.tejus.shavedogmusic.core.ShaveMediaPlayer;
import com.tejus.shavedogmusic.core.ShaveService;
import com.tejus.shavedogmusic.utils.Logger;

public class PlayActivity extends Activity {

    private Button streamButton, nextButton;

    private ImageButton playButton;

    private TextView textStreamed, songName, wifiState;

    private boolean isPlaying;

    private ShaveMediaPlayer audioStreamer;
    private BroadcastReceiver mShaveReceiver = new ServiceIntentReceiver();
    ServiceConnection mConnection;
    ShaveService mShaveService;
    Context mContext;
    Handler handler = new Handler();

    public void onCreate( Bundle icicle ) {

        super.onCreate( icicle );
        setContentView( R.layout.play );
        mContext = this;
        initControls();
        initReceiver();
        initShaveServiceStuff();
        setViewValues();

    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.play_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch ( item.getItemId() ) {

            case R.id.find_peers:
                mShaveService.testPopulateList();
                streamButton.setVisibility( View.VISIBLE );
                return true;

            case R.id.set_creds:
                startActivity( new Intent().setClass( mContext, CredentialsActivity.class ) );
                return true;

            case R.id.test_api:
                this.testApi();
                return true;

            case R.id.dump_maps:
                mShaveService.dumpMapsToLogs();
                return true;

            case R.id.quit:
                this.quit();
                return true;

            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private void testApi() {
        mShaveService.testApi();
    }

    private void initControls() {
        textStreamed = ( TextView ) findViewById( R.id.text_kb_streamed );
        wifiState = ( TextView ) findViewById( R.id.wifi_state );
        songName = ( TextView ) findViewById( R.id.song_name );
        streamButton = ( Button ) findViewById( R.id.button_stream );
        nextButton = ( Button ) findViewById( R.id.next );
        nextButton.setOnClickListener( new View.OnClickListener() {
            public void onClick( View view ) {
                audioStreamer.closePreviousDownloadSocket();
                // need to do this since we're using one download port per peer:
                startStreamingAudio();
            }
        } );
        streamButton.setOnClickListener( new View.OnClickListener() {
            public void onClick( View view ) {
                startStreamingAudio();
                streamButton.setVisibility( View.GONE );
                nextButton.setVisibility( View.VISIBLE );
            }
        } );

        playButton = ( ImageButton ) findViewById( R.id.button_play );
        playButton.setEnabled( false );
        playButton.setOnClickListener( new View.OnClickListener() {
            public void onClick( View view ) {
                if ( audioStreamer.getMediaPlayer().isPlaying() ) {
                    audioStreamer.getMediaPlayer().pause();
                    playButton.setImageResource( R.drawable.iconshave );
                } else {
                    audioStreamer.getMediaPlayer().start();
                    audioStreamer.startPlayProgressUpdater();
                    playButton.setImageResource( R.drawable.icon );
                }
                isPlaying = !isPlaying;
            }
        } );

    }

    private void startStreamingAudio() {
        try {
            final ProgressBar progressBar = ( ProgressBar ) findViewById( R.id.progress_bar );
            String peerToAsk = mShaveService.getNextPeer();
            Logger.d( "PlayActivity.startStreamingAudio: gonna request: " + peerToAsk );
            String downloadAddress = mShaveService.getPeerAddress( peerToAsk );
            int downloadPort = mShaveService.getDownloadPort( peerToAsk );
            if ( audioStreamer != null ) {
                audioStreamer.interrupt();
            }
            audioStreamer = new ShaveMediaPlayer( this, mShaveService, textStreamed, songName, playButton, streamButton, progressBar );
            audioStreamer.startStreaming( downloadAddress, downloadPort, 1677, 214 );
            // streamButton.setEnabled(false);
        } catch ( NullPointerException e ) {
            // prompt to reassociate with peers:
            Toast.makeText( this, getResources().getString( R.string.reassoc ), Toast.LENGTH_SHORT ).show();
            e.printStackTrace();
        } catch ( IndexOutOfBoundsException e ) {
            // prompt to reassociate with peers:
            Toast.makeText( this, getResources().getString( R.string.reassoc ), Toast.LENGTH_SHORT ).show();
            e.printStackTrace();
        } catch ( IOException e ) {
            Log.e( getClass().getName(), "Error starting to stream audio.", e );
        }

    }

    void initShaveServiceStuff() {
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected( ComponentName className ) {
                mShaveService = null;
                Toast.makeText( mContext, R.string.shave_service_disconnected, Toast.LENGTH_SHORT ).show();
            }

            @Override
            public void onServiceConnected( ComponentName name, IBinder service ) {
                mShaveService = ( ( ShaveService.ShaveBinder ) service ).getService();
            }
        };

        doBindService();
        startService( new Intent().setClass( mContext, ShaveService.class ) );
    }

    void doBindService() {
        bindService( new Intent( this, ShaveService.class ), mConnection, Context.BIND_AUTO_CREATE );
    }

    public class ServiceIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive( Context context, Intent intent ) {
            String action = intent.getAction();
            Logger.d( "PlayActivity.ServiceIntentReceiver: action = " + action );
            if ( action.equals( Definitions.INTENT_SONG_PLAYING ) ) {
                Logger.d( "PlayActivity.ServiceIntentReceiver: song name update received.." );
                updateSongName( intent );
            } else if ( action.equals( Definitions.WIFI_STATE_CHANGE ) ) {
                Logger.d( "PlayActivity.ServiceIntentReceiver: wifi state update received.." );
                // updating the view after 5s, since the intent received only
                // indicates a WIFI state change
                handler.postDelayed( new Runnable() {
                    @Override
                    public void run() {
                        setViewValues();
                    }
                }, 5000 );
            }

        }

    }

    private void updateSongName( Intent intent ) {
        String userName = ( intent.getStringExtra( "user_name" ) != null ) ? intent.getStringExtra( "user_name" ) : null;
        String songNameReceived = ( intent.getStringExtra( "song_name" ) != null ) ? intent.getStringExtra( "song_name" ) : null;
        Logger.d( "broadcast intent received, songName = " + songNameReceived );
        if ( songNameReceived != null && userName != null ) {
            songName.setText( getResources().getString( R.string.now_playing ) + songNameReceived + ", from: " + userName );
        }
    }

    void setViewValues() {
        ConnectivityManager connManager = ( ConnectivityManager ) getSystemService( CONNECTIVITY_SERVICE );
        NetworkInfo netInfo = connManager.getNetworkInfo( ConnectivityManager.TYPE_WIFI );

        if ( netInfo.isConnected() ) {
            Logger.d( "setViewValues: wifi is on" );
            wifiState.setText( getResources().getString( R.string.wifi_on ) );
        } else {
            Logger.d( "setViewValues: wifi is off" );
            wifiState.setText( getResources().getString( R.string.wifi_off ) );
        }
    }

    @Override
    protected void onStop() {
        unbindService( mConnection );
        super.onStop();
    }

    void quit() {
        Logger.d( "quit(): Killing myself.." );
        android.os.Process.killProcess( android.os.Process.myPid() );
    }

    void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( Definitions.INTENT_SONG_PLAYING );
        filter.addAction( Definitions.WIFI_STATE_CHANGE );
        registerReceiver( mShaveReceiver, filter );
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver( mShaveReceiver );
    }

    @Override
    protected void onResume() {
        super.onResume();
        initReceiver();
        setViewValues();
        initShaveServiceStuff();
    }

}
