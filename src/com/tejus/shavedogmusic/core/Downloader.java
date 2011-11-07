package com.tejus.shavedogmusic.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.tejus.shavedogmusic.core.Definitions;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class Downloader extends AsyncTask<Context, Integer, Boolean> {
    String filePath;
    Context context;
    int downloadPort;
    long fileSize;

    public Downloader( int downloadPort, String filePath, long fileSize ) {
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.downloadPort = downloadPort;
    }

    @Override
    protected void onProgressUpdate( Integer... progress ) {
        updateDownloadProgress( this.context, filePath, progress[ 0 ] );
    }

    @Override
    protected Boolean doInBackground( Context... context ) {
        ServerSocket serverSocket;
        Socket connection;
        this.context = context[ 0 ];
        serverSocket = null;
        try {
            serverSocket = new ServerSocket( Definitions.DOWNLOAD_TRANSFER_PORT );
            while ( true ) {
                Log.d( "XXXX", "Downloader - gonna start waiting on accept()" );
                connection = serverSocket.accept();
                InputStream iStream = connection.getInputStream();
                FileOutputStream oStream = new FileOutputStream( new File( DEFAULT_DOWNLOAD_LOC + "/" + getFileNameTrivial( filePath ) ) );
                Log.d( "XXXX", "Downloader - will start dloading to : " + DEFAULT_DOWNLOAD_LOC + "/" + getFileNameTrivial( filePath ) );
                byte[] readByte = new byte[ Definitions.DOWNLOAD_BUFFER_SIZE ];
                int size, previousProgress = 0;
                long count = 0;
                while ( ( size = iStream.read( readByte ) ) > 0 ) {
                    oStream.write( readByte, 0, size );
                    count += ( long ) size;
                    if ( fileSize > Definitions.DOWNLOAD_BUFFER_SIZE ) {
                        int progress = ( int ) ( ( count * 100 ) / fileSize );
                        if ( progress < 100 ) {
                            Log.d( "XXXX", "Downloader - download count = " + count + ", size here = " + size + ", progress = " + progress );
                            if ( previousProgress != progress ) {
                                previousProgress = progress;
                                publishProgress( progress );
                            }

                        }
                    } else {
                        Log.d( "XXXX", "Downloader - download count = " + count + ", progress = 100" );
                    }
                }
                if ( count < fileSize ) {
                    publishProgress( -1 );
                }
                publishProgress( 100 );
                Log.d( "XXXX", "Downloader - done dloading : " + filePath );
                iStream.close();
                oStream.close();
                serverSocket.close();
                return true;
            }
        } catch ( IOException e ) {
            e.printStackTrace();
            return false;
        }
    }
}
