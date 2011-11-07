package com.tejus.shavedogmusic.core;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Environment;

public class LatestLocalMusicList {
    long lastRefreshTime;
    ArrayList<String> musicList = new ArrayList<String>();
    String DEFAULT_MUSIC_DIRECTORY = Environment.DIRECTORY_MUSIC;

    long refreshLocalList() {
        new LocalMusicListUpdater().execute();
        return lastRefreshTime;

    }

    private class LocalMusicListUpdater extends AsyncTask<Void, Void, Long> {

        @Override
        protected Long doInBackground( Void... shaveADogNow ) {
            File listOfFiles = new File( DEFAULT_MUSIC_DIRECTORY );

            return null;

        }

    }
    
    public class SupportedExtensions implements FilenameFilter { 
    ArrayList<String> extensions = Definitions.MUSIC_TYPES; 
    
    public boolean accept(File dir, String name) { 
    return name.endsWith(); 
    } 
    }

}
