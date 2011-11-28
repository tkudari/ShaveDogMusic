package com.tejus.shavedogmusic.core;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;

public class Definitions {
    public static int IP_ADDRESS_INT;
    public static InetAddress IP_ADDRESS_INETADDRESS;
    public static int SEARCH_SERVER_PORT = 5555;
    public static int GENERIC_SERVER_PORT = 5565;
    public static int TEST_SERVER_PORT = 5595;
    public static final int UPLOAD_TRANSFER_PORT = 5575;
    public static final int DOWNLOAD_TRANSFER_PORT = 5585;

    public static final int COMMAND_BUFSIZE = 4000;
    public static final int DOWNLOAD_BUFFER_SIZE = 16384;
    public static String QUERY_LIST = "query_list";
    public static final String TAG = "XXXX";
    public static final boolean DASHWIRE = true;
    public static final int SOCKET_TIMEOUT = 1000000; // TODO: decide the
                                                      // correct value?

    public static String USERNAME;
    public static String credsPrefFile = "credsPrefFile";
    public static String prefUserName = "pUserName";
    public static String prefHomeDirectory = "pHomeDirectory";
    public static String defaultUserName = "defaultUserName";
    public static int MIN_USERNAME_LENGTH = 3;
    public static String END_DELIM = "*";
    public static String COMMAND_DELIM = ":*";
    public static int COMMAND_WORD_LENGTH = 4;
    public static String REPLY_ACCEPTED = "yes";
    public static final String MSG_TYPE = "msg_type";
    public static final String SENDER_ADDRESS = "sender_address";
    public static String REQUEST_LISTING = "show_files";
    public static String LISTING_REPLY = "listing_reply";
    public static String REQUEST_FILE = "request_file";
    public static String REQUEST_DIRECTORY = "request_directory";
    public static String HOME_DIRECTORY = "";

    public static final String DISCOVER_PING = "discover_ping";
    public static boolean ALLOW_COMPLEX_SEARCH = false;

    public static final String DISCOVER_ACK = "discover_ack";
    public static final String DISCOVER_ACK2 = "discover_ack2";
    public static final String PLAY_REQUEST = "play_request";
    public static final String PLAY_ACK = "play_ack";
    
    public static final int TRANSACTION_PORT_START = 10000;
    public static final int PACKET_DUPE_THRESHOLD_TIME = 1000;
    public static final boolean IS_IN_DEBUG_MODE = true;
    public static final String WIFI_STATE_CHANGE = "wifi_state_change";
    public static final String SERVED_ALL_SONGS = "served_all_songs";
    public static final String SENDER_UNAME = "sender_uname";
    public static final String START_SEARCH_PROGRESS = "com.tejus.shavedogmusic.start_progress";
    public static final String STOP_SEARCH_PROGRESS = "com.tejus.shavedogmusic.stop_progress";
    
    // Intent Names:
    public static String INTENT_SONG_PLAYING = "com.tejus.shavedogmusic.song_name";
    public static String INTENT_FRIEND_ACCEPTED = "com.tejus.shavedog.friend_accepted";

    // add more formats here:
    public static ArrayList<String> MUSIC_TYPES = new ArrayList<String>() {
        {
            add( ".mp3" );
        }
    };

    //

}
