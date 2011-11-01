package com.tejus.shavedogmusic.activity;

import com.tejus.shavedogmusic.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;

import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
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

import com.tejus.shavedogmusic.core.ShaveMediaPlayer;
import com.tejus.shavedogmusic.core.ShaveService;
import com.tejus.shavedogmusic.utils.Logger;

public class PlayActivity extends Activity {

    private Button streamButton;

    private ImageButton playButton;

    private TextView textStreamed;

    private boolean isPlaying;

    private ShaveMediaPlayer audioStreamer;
    private BroadcastReceiver mShaveReceiver = new ServiceIntentReceiver();
    ServiceConnection mConnection;
    ShaveService mShaveService;
    Context mContext;

    public void onCreate( Bundle icicle ) {

        super.onCreate( icicle );
        setContentView( R.layout.play );
        mContext = this;
        initControls();
        initShaveServiceStuff();
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
                return true;

            case R.id.set_creds:
                startActivity( new Intent().setClass( mContext, CredentialsActivity.class ) );
                return true;

            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private void initControls() {
        textStreamed = ( TextView ) findViewById( R.id.text_kb_streamed );
        streamButton = ( Button ) findViewById( R.id.button_stream );
        streamButton.setOnClickListener( new View.OnClickListener() {
            public void onClick( View view ) {
                startStreamingAudio();
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
            if ( audioStreamer != null ) {
                audioStreamer.interrupt();
            }
            audioStreamer = new ShaveMediaPlayer( this, textStreamed, playButton, streamButton, progressBar );
            audioStreamer.startStreaming( "http://www.pocketjourney.com/downloads/pj/tutorials/audio.mp3", 1677, 214 );
            // streamButton.setEnabled(false);
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
            Logger.d( "broadcast intent received, username = " + intent.getStringExtra( "user_name" ) + ", address = " + intent.getStringExtra( "address" ) );
        }
    }

    @Override
    protected void onStop() {
        unbindService( mConnection );
        super.onStop();
    }

}
