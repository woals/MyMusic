package com.yinyxn.music;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

/**
 * Created by yinyxn on 2015/12/23.
 */
public class App extends Application {

    /**
     *  广播：播放当前歌曲
     */
    public static final String PLAY_CURRENT = "com.example.music.action.PLAY_CURRENT";

    /**
     * 当前的曲目
     */
    public static final String EXTRA_SONG = "song";

    public static final String LOAD_SONG_LIST = "com.example.music.action.LOAD_SONG_LIST";
    public static final String EXTRA_SONG_LIST = "song_list";

    private static final String TAG = "App";


    public static final String PLAY_PREVIOUS = "com.example.music.action.PLAY_PREVIOUS";
    public static final String PLAY_NEXT = "com.example.music.action.PLAY_NEXT";
    public static final String PLAY = "com.example.music.action.PLAY";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");

        //启动服务
        startService(new Intent(this, MusicService.class));
    }
}
