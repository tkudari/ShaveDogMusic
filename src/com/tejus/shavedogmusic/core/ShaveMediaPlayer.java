package com.tejus.shavedogmusic.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

import com.tejus.shavedogmusic.core.Definitions;
import com.tejus.shavedogmusic.utils.Logger;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * MediaPlayer does not yet support streaming from external URLs so this class
 * provides a pseudo-streaming function by downloading the content incrementally
 * & playing as soon as we get enough audio in our temporary storage.
 */

public class ShaveMediaPlayer {

    private String lname = "ShaveMediaPlayer";
    private static final int INTIAL_KB_BUFFER = 96 * 10 / 8;// assume
                                                            // 96kbps*10secs/8bits
                                                            // per byte
    private TextView textStreamed;
    private ImageButton playButton;
    private ProgressBar progressBar;
    // Track for display by progressBar
    private long mediaLengthInKb, mediaLengthInSeconds;
    private int totalKbRead = 0;
    private ShaveService mShaveService;

    // Create Handler to call View updates on the main UI thread.
    private final Handler handler = new Handler();
    private MediaPlayer mediaPlayer;
    private File downloadingMediaFile;
    private boolean isInterrupted;
    private Context context;
    private int counter = 0;
    private String downloadAddress;
    private int downloadPort;

    public ShaveMediaPlayer( Context context, ShaveService serviceObject, TextView textStreamed, ImageButton playButton, Button streamButton,
            ProgressBar progressBar ) {
        this.context = context;
        this.textStreamed = textStreamed;
        this.playButton = playButton;
        this.progressBar = progressBar;
        this.mShaveService = serviceObject;
    }

    /**
     * Progressively download the media to a temporary location and update the
     * MediaPlayer as new content becomes available.
     */
    public void startStreaming( final String downloadAddress, final int downloadPort, final long mediaLengthInKb, long mediaLengthInSeconds ) throws IOException {
        this.mediaLengthInKb = mediaLengthInKb;
        this.mediaLengthInSeconds = mediaLengthInSeconds;

        Runnable r = new Runnable() {
            public void run() {
                try {
                    downloadAudioIncrement( downloadAddress, downloadPort, mediaLengthInKb );
                } catch ( IOException e ) {
                    Logger.e( "Unable to initialize the MediaPlayer for peerAddress = " + downloadAddress );
                    e.printStackTrace();
                    return;
                }
            }
        };
        new Thread( r ).start();
    }

    /**
     * Download the url stream to a temporary location and then call the
     * setDataSource for that local file
     */
    public void downloadAudioIncrement( String destinationAddress, int downloadPort, long fileSize ) throws IOException {
        String message = Definitions.PLAY_REQUEST;
        // Ask the peer to start uploading:
        mShaveService.sendMessage( destinationAddress, message );

        // Wait for the upload, download the song:
        ServerSocket serverSocket;
        Socket connection;
        serverSocket = null;
        try {
            serverSocket = new ServerSocket( downloadPort );
            Logger.d( "ShaveMediaPlayer.downloadAudioIncrement: waiting to hear back from - " + destinationAddress + ", at port: " + downloadPort );
            connection = serverSocket.accept();
            InputStream iStream = connection.getInputStream();
            if ( iStream == null ) {
                Logger.e( "Unable to create InputStream for peer at:" + destinationAddress );
            }
            downloadingMediaFile = new File( context.getCacheDir(), "downloadingMedia_" + ( counter++ ) + ".mp3" );
            FileOutputStream oStream = new FileOutputStream( downloadingMediaFile );
            byte buf[] = new byte[ 16384 ];
            int totalBytesRead = 0, incrementalBytesRead = 0;
            do {
                int numread = iStream.read( buf );
                if ( numread <= 0 )
                    break;
                oStream.write( buf, 0, numread );
                totalBytesRead += numread;
                incrementalBytesRead += numread;
                totalKbRead = totalBytesRead / 1000;

                testMediaBuffer();
                fireDataLoadUpdate();
            } while ( validateNotInterrupted() );

            iStream.close();
            if ( validateNotInterrupted() ) {
                fireDataFullyLoaded();
            }

        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    // void socketDownloader( String address, int downloadPort, long fileSize )
    // {
    //
    // ServerSocket serverSocket;
    // Socket connection;
    // serverSocket = null;
    // try {
    // serverSocket = new ServerSocket( downloadPort );
    // while ( true ) {
    // Logger.d( "Downloader - gonna start waiting on accept()" );
    // connection = serverSocket.accept();
    // InputStream iStream = connection.getInputStream();
    // FileOutputStream oStream = new FileOutputStream( new File(
    // context.getCacheDir(), "downloadingMedia_" + ( counter++ ) + ".dat" ) );
    // Logger.d( "Downloader - will start dloading to : " +
    // context.getCacheDir().getName() + "downloadingMedia_" + ( counter ) +
    // ".dat" );
    // byte[] readByte = new byte[ Definitions.DOWNLOAD_BUFFER_SIZE ];
    // int size, previousProgress = 0;
    // long count = 0;
    // while ( ( size = iStream.read( readByte ) ) > 0 ) {
    // oStream.write( readByte, 0, size );
    // count += ( long ) size;
    // if ( fileSize > Definitions.DOWNLOAD_BUFFER_SIZE ) {
    // int progress = ( int ) ( ( count * 100 ) / fileSize );
    // if ( progress < 100 ) {
    // Logger.d( "Downloader - download count = " + count + ", size here = " +
    // size + ", progress = " + progress );
    // if ( previousProgress != progress ) {
    // previousProgress = progress;
    // }
    //
    // }
    // } else {
    // Log.d( "XXXX", "Downloader - download count = " + count +
    // ", progress = 100" );
    // }
    // }
    //
    // Logger.d( "Downloader - done dloading : " + filePath );
    // iStream.close();
    // oStream.close();
    // serverSocket.close();
    // return true;
    // }
    // } catch ( IOException e ) {
    // e.printStackTrace();
    // return false;
    // }
    //
    // }

    private boolean validateNotInterrupted() {
        if ( isInterrupted ) {
            if ( mediaPlayer != null ) {
                mediaPlayer.pause();
                // mediaPlayer.release();
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether we need to transfer buffered data to the MediaPlayer.
     * Interacting with MediaPlayer on non-main UI thread can causes crashes to
     * so perform this using a Handler.
     */
    private void testMediaBuffer() {
        Runnable updater = new Runnable() {
            public void run() {
                if ( mediaPlayer == null ) {
                    // Only create the MediaPlayer once we have the minimum
                    // buffered data
                    if ( totalKbRead >= INTIAL_KB_BUFFER ) {
                        try {
                            Logger.d( "testMediaBuffer: startingMediaPlayer" );
                            startMediaPlayer();
                        } catch ( Exception e ) {
                            Log.e( getClass().getName(), "Error copying buffered conent.", e );
                        }
                    }
                } else if ( mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition() <= 1000 ) {
                    // NOTE: The media player has stopped at the end so transfer
                    // any existing buffered data
                    // We test for < 1second of data because the media player
                    // can stop when there is still
                    // a few milliseconds of data left to play
                    Logger.d( " gonna transferBufferToMediaPlayer.." );
                    transferBufferToMediaPlayer();
                }

                if ( mediaPlayer != null ) {
                    Logger.d( "testMediaBuffer : mediaPlayer.getDuration() = " + mediaPlayer.getDuration() + ", mediaPlayer.getCurrentPosition() = "
                            + mediaPlayer.getCurrentPosition() );
                }
            }
        };
        handler.post( updater );
    }

    private void startMediaPlayer() {
        try {
            File bufferedFile = new File( context.getCacheDir(), "playingMedia" + ( counter - 1 ) + ".mp3" );
            moveFile( downloadingMediaFile, bufferedFile );

            Log.e( "Player", bufferedFile.length() + "" );
            Log.e( "Player", bufferedFile.getAbsolutePath() );
            mediaPlayer = new MediaPlayer();
            FileInputStream fIs = new FileInputStream( bufferedFile );
            FileDescriptor fD = fIs.getFD();
            mediaPlayer.setDataSource( fD );

            mediaPlayer.setAudioStreamType( AudioManager.STREAM_MUSIC );
            mediaPlayer.prepare();
            fireDataPreloadComplete();

        } catch ( IOException e ) {
            Log.e( getClass().getName(), "Error initializing the MediaPlaer.", e );
            return;
        }
    }

    /**
     * Transfer buffered data to the MediaPlayer. Interacting with MediaPlayer
     * on non-main UI thread can causes crashes to so perform this using a
     * Handler.
     */
    private void transferBufferToMediaPlayer() {
        try {
            // First determine if we need to restart the player after
            // transferring data...e.g. perhaps the user pressed pause
            boolean wasPlaying = mediaPlayer.isPlaying();
            int curPosition = mediaPlayer.getCurrentPosition();
            if ( !wasPlaying ) {
                mediaPlayer.pause();
            }

            downloadingMediaFile = new File( context.getCacheDir(), "downloadingMedia_" + ( counter - 1 ) + ".mp3" );
            Logger.d( lname + ".transferBufferToMediaPlayer: downloadingMediaFile size here = " + downloadingMediaFile.length() );
            File bufferedFile = new File( context.getCacheDir(), "playingMedia" + ( counter - 1 ) + ".mp3" );
            moveFile( downloadingMediaFile, bufferedFile );

            // FileUtils.copyFile(downloadingMediaFile,bufferedFile);
            FileInputStream fIs = new FileInputStream( bufferedFile );
            FileDescriptor fD = fIs.getFD();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource( fD );
            // mediaPlayer.setAudioStreamType(AudioSystem.STREAM_MUSIC);
            mediaPlayer.prepare();
            mediaPlayer.seekTo( curPosition );

            // Restart if at end of prior buffered content or mediaPlayer was
            // previously playing.
            // NOTE: We test for < 1second of data because the media player can
            // stop when there is still
            // a few milliseconds of data left to play
            boolean atEndOfFile = mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition() <= 1000;
            if ( wasPlaying || atEndOfFile ) {
                mediaPlayer.start();
            }
        } catch ( Exception e ) {
            Log.e( getClass().getName(), "Error updating to newly loaded content.", e );
        }
    }

    private void fireDataLoadUpdate() {
        Runnable updater = new Runnable() {
            public void run() {
                textStreamed.setText( ( CharSequence ) ( totalKbRead + " Kb read" ) );
                float loadProgress = ( ( float ) totalKbRead / ( float ) mediaLengthInKb );
                progressBar.setSecondaryProgress( ( int ) ( loadProgress * 100 ) );
            }
        };
        handler.post( updater );
    }

    /**
     * We have preloaded enough content and started the MediaPlayer so update
     * the buttons & progress meters.
     */
    private void fireDataPreloadComplete() {
        Runnable updater = new Runnable() {
            public void run() {
                mediaPlayer.start();
                startPlayProgressUpdater();
                playButton.setEnabled( true );
                // streamButton.setEnabled(false);
            }
        };
        handler.post( updater );
    }

    private void fireDataFullyLoaded() {
        Runnable updater = new Runnable() {
            public void run() {
                transferBufferToMediaPlayer();
                textStreamed.setText( ( CharSequence ) ( "Audio full loaded: " + totalKbRead + " Kb read" ) );
            }
        };
        handler.post( updater );
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void startPlayProgressUpdater() {
        float progress = ( ( ( float ) mediaPlayer.getCurrentPosition() / 1000 ) / ( float ) mediaLengthInSeconds );
        progressBar.setProgress( ( int ) ( progress * 100 ) );

        if ( mediaPlayer.isPlaying() ) {
            Runnable notification = new Runnable() {
                public void run() {
                    startPlayProgressUpdater();
                }
            };
            handler.postDelayed( notification, 1000 );
        }
    }

    public void interrupt() {
        playButton.setEnabled( false );
        isInterrupted = true;
        validateNotInterrupted();
    }

    public void moveFile( File oldLocation, File newLocation ) throws IOException {

        if ( oldLocation.exists() ) {
            BufferedInputStream reader = new BufferedInputStream( new FileInputStream( oldLocation ) );
            BufferedOutputStream writer = new BufferedOutputStream( new FileOutputStream( newLocation, false ) );
            try {
                byte[] buff = new byte[ 8192 ];
                int numChars;
                while ( ( numChars = reader.read( buff, 0, buff.length ) ) != -1 ) {
                    writer.write( buff, 0, numChars );
                }
            } catch ( IOException ex ) {
                throw new IOException( "IOException when transferring " + oldLocation.getPath() + " to " + newLocation.getPath() );
            } finally {
                try {
                    if ( reader != null ) {
                        writer.close();
                        reader.close();
                    }
                } catch ( IOException ex ) {
                    Log.e( getClass().getName(), "Error closing files when transferring " + oldLocation.getPath() + " to " + newLocation.getPath() );
                }
            }
        } else {
            throw new IOException( "Old location does not exist when transferring " + oldLocation.getPath() + " to " + newLocation.getPath() );
        }
    }
}
