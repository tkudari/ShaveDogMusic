package com.tejus.shavedogmusic.core;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.tejus.shavedogmusic.utils.Logger;

import android.os.AsyncTask;
import android.os.Environment;

/*
 * Here's where we deal with everything related to our local music list.
 * For now, everything's only persisted in memory, since ShaveDog has to refresh / establish sessions with peers if the service stops for any reason.
 * Can be persisted to disk too, if required. Tags, etc related to music can be dealt with.
 */

public class LocalMusicManager {
    // storing this value here, coz it could be usefiul for later
    long lastRefreshTime;
    public ArrayList<String> musicList = new ArrayList<String>();
    public String DEFAULT_MUSIC_DIRECTORY = Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_MUSIC;

    public ArrayList<String> getLatestLocalList() {

        try {
            lastRefreshTime = new LocalMusicListUpdater().execute().get();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return musicList;
    }

    /*
     * Returns the time the list was done refreshing:
     */

    private class LocalMusicListUpdater extends AsyncTask<Void, Void, Long> {

        @Override
        protected Long doInBackground( Void... shaveADogNow ) {
            File listOfFiles = new File( DEFAULT_MUSIC_DIRECTORY );
            
            List<String> songList = Arrays.asList( listOfFiles.list( new SupportedExtensions() ) );
            for (String song : songList) {
                Logger.d( "LocalMusicListUpdater: adding song = " + song );
                musicList.add( song );
            }
            return System.currentTimeMillis();

        }

    }

    public class SupportedExtensions implements FilenameFilter {
        ArrayList<String> extensions = Definitions.MUSIC_TYPES;

        public boolean accept( File dir, String name ) {
            Iterator<String> iterator = extensions.iterator();
            while ( iterator.hasNext() ) {
                return name.endsWith( iterator.next() );
            }
            return false;
        }
    }

    public long getLastRefreshTime() {
        return lastRefreshTime;
    }

    /*
     * We serve up the next song for userName here. For now, it's just the first
     * song not served to him yet. So we're not using his user name yet.
     */

    public String requestSongForUser( String userName, ArrayList<String> history ) {
        if ( musicList.size() == 0 ) {
            getLatestLocalList();
        }
        Iterator<String> iterator = musicList.iterator();
        while ( iterator.hasNext() ) {
            String localItem = iterator.next().toString();
            // return the first song not in userName's history:
            if ( !history.contains( localItem ) ) {
                return localItem;
            }
        }
        // if all our songs have been served before, return our first song:
        return musicList.get( 0 );

    }

}
