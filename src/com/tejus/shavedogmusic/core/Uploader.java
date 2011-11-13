package com.tejus.shavedogmusic.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.tejus.shavedogmusic.core.Definitions;
import com.tejus.shavedogmusic.utils.Logger;

import android.content.Context;
import android.os.AsyncTask;

public class Uploader extends AsyncTask<Context, Integer, Boolean> {
    String filePath, destinationAddress;
    int destinationPort;
    long fileSize;

    public Uploader( String destinationAddress, int destinationPort, String filePath, long fileSize ) {
        this.filePath = filePath;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
        this.fileSize = fileSize;
        Logger.d( "Uploader: destination address = " + destinationAddress + ", destination port = " + destinationPort );
    }

    @Override
    protected Boolean doInBackground( Context... params ) {
        Socket socket = null;
        FileInputStream iStream = null;
        OutputStream oStream = null;

        try {
            socket = new Socket( destinationAddress, destinationPort );
            oStream = ( OutputStream ) socket.getOutputStream();
            iStream = new FileInputStream( new File( filePath ) );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        try {
            socket.sendUrgentData( 100 );
            byte[] readArray = new byte[ Definitions.DOWNLOAD_BUFFER_SIZE ];
            int size;
            long count = 0;
            while ( ( size = iStream.read( readArray ) ) > 0 ) {
                if ( isCancelled() ) {
                    break;
                }
                oStream.write( readArray, 0, size );
                ++count;
                if ( fileSize > Definitions.DOWNLOAD_BUFFER_SIZE ) {
                    Logger.d( "Uploader: upload progress percent = " + ( int ) ( ( count * Definitions.DOWNLOAD_BUFFER_SIZE * 100 ) / fileSize ) );
                } else {
                    Logger.d( "Uploader: upload count = " + count + ", progress = 100" );
                }
            }
            Logger.d( "Uploader: done uploading : " + filePath + " !!" );

            oStream.close();
            iStream.close();
            socket.close();
            return true;
        } catch ( Exception e ) {
            e.printStackTrace();
            return false;
        }
    }
}
