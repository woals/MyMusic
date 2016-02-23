package com.yinyxn.music;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //音乐服务
    private MusicService musicService;
    private ProgressThread progressThread;


    private ListView listView;
    private ProgressBar progressBar;
    private SongAdapter adapter;
    private TextView textViewTitle;
    private TextView textViewDuration;
    private TextView textViewPosition;
    private SeekBar seekBar;
    private Button button;
    private Button button_play;
    private Button button_next;
    private Button button_previous;


    //音乐列表
    private ArrayList songList;

    GestureDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        listView = (ListView) findViewById(R.id.listView);
//        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textViewTitle = (TextView) findViewById(R.id.textView_title);
        textViewDuration = (TextView) findViewById(R.id.textView_duration);
        textViewPosition = (TextView) findViewById(R.id.textView_position);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        button = (Button) findViewById(R.id.button);
        button_next = (Button) findViewById(R.id.button_next);
        button_play = (Button) findViewById(R.id.button_play);
        button_previous = (Button) findViewById(R.id.button_previous);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                musicService.mediaPlayer.start();
                musicService.play();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicService.play();
            }
        });

        detector = new GestureDetector(this,new GestureListener());
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //委托
        return detector.onTouchEvent(event);
    }
    boolean isSwitch = true;
    public void switchOver(View view) {
        if (isSwitch) {
            button.setText("按钮");
            seekBar.setEnabled(false);
            button_next.setEnabled(false);
            button_play.setEnabled(false);
            button_previous.setEnabled(false);
//            listView.setVisibility(View.GONE);
            isSwitch = false;
        } else {
            button.setText("手势");
            seekBar.setEnabled(true);
            button_next.setEnabled(true);
            button_play.setEnabled(true);
            button_previous.setEnabled(true);
//            listView.setVisibility(View.VISIBLE);
            isSwitch = true;
        }
    }

    class GestureListener implements GestureDetector.OnGestureListener{

        private static final String TAG = "GestureListener";

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown");
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(TAG, "onShowPress");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(TAG, "onSingleTapUp");
            return true;
        }

        //滑动路径
        @Override
        public boolean onScroll(
                MotionEvent e1,
                MotionEvent e2,
                float distanceX,
                float distanceY) {
            Log.d(TAG, "onScroll");
            return true;
        }

        //长按
        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress");
        }

        //移动方向（趋势）
        @Override
        public boolean onFling(
                MotionEvent e1,     //起点
                MotionEvent e2,
                float velocityX,    //偏移量
                float velocityY) {
            Log.d(TAG, "onFling");

            float sx = e1.getX();
            float sy = e1.getY();

            float ex = e2.getX();
            float ey = e2.getY();

            if(Math.abs(ex-sx)>Math.abs(ey-sy)){        //x的偏移量大于y的偏移量
                if(ex>sx){
                    try {
                        musicService.playNext();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else {
                    try {
                        musicService.playPrevious();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                if (ey > sy){
                    musicService.play();
                }else {
                    musicService.play();
                }
            }

            return true;
        }
    }

    //绑定服务
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");


        //收不到本地广播
//        registerReceiver()

        //动态注册本地广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(App.LOAD_SONG_LIST);
        filter.addAction(App.PLAY_CURRENT);
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        receiver,
                        filter);

        //绑定服务
        bindService(
                new Intent(this, MusicService.class),
                conn,//连接
                BIND_AUTO_CREATE//自动创建
        );


        detector = new GestureDetector(this,new GestureListener());
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressThread = new ProgressThread();
        progressThread.start();
        detector = new GestureDetector(this,new GestureListener());
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");
        progressThread.isRunning = false;


        //注销广播接收器
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(receiver);

        //解除绑定
        unbindService(conn);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
//        seekBar.setProgress(musicService.getCurrentProgress());
        Log.d(TAG, "onRestart");
    }

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "onServiceConnected");

            MusicService.LocalBinder localBinder = (MusicService.LocalBinder) binder;
            musicService = localBinder.getService();//绑定

            //绑定完成之后才可以调用服务
            if (musicService.isInit) {
                songList = musicService.getSongList();
                Log.d(TAG, "绑定后从服务获得数据： " + songList.size());

                //在 ListView 中显示
                showSongList();
            }
        }

        //断开连接
        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
        }
    };

    //广播接收器
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            switch (action) {
                case App.LOAD_SONG_LIST:
                    loadSongList(intent);
                    break;
                case App.PLAY_CURRENT:
                    playCurrent(intent);
                    break;
            }
        }

        private void playCurrent(Intent intent) {
            Song song = (Song) intent.getSerializableExtra(App.EXTRA_SONG);

            textViewTitle.setText(song.title);
            textViewDuration.setText(TimeUtil.formatDuration(song.duration));
            textViewPosition.setText(TimeUtil.formatDuration(0));
            seekBar.setMax((int) song.duration);

        }

        private void loadSongList(Intent intent) {
            if (songList == null) {
                Bundle bundle = intent.getExtras();
                songList = (ArrayList<Song>) bundle.get(App.EXTRA_SONG_LIST);
                Log.d(TAG, "从广播获得数据： " + songList.size());
                showSongList();
            }
        }
    };

    //显示音乐列表
    private void showSongList() {

        adapter = new SongAdapter(this, songList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    musicService.play(position);
//                    textViewTitle.setText(songList.get(position).title);
//                    textViewDuration.setText(TimeUtil.formatDuration(songList.get(position).duration));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

//        new MusicLoaderTask().execute();

    }


    //加载处理
    class MusicLoaderTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    public void play(View v) {
        musicService.play();
    }

    public void playPrevious(View v) {
        try {
            musicService.playPrevious();
//            textViewTitle.setText(songList.get(musicService.playPrevious()).title);
//            textViewDuration.setText(TimeUtil.formatDuration(songList.get(musicService.playNext()).duration));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playNext(View v) {
        try {
            musicService.playNext();
//            textViewTitle.setText(songList.get(musicService.playNext()).title);
//            textViewDuration.setText(TimeUtil.formatDuration(songList.get(musicService.playNext()).duration));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ProgressThread extends Thread {
        volatile boolean isRunning = true;

        @Override
        public void run() {
            super.run();
            while (isRunning) {
                //保证服务绑定之后再执行
                if (musicService != null) {
                    //读取服务中的播放进度
                    final int progress = musicService.getCurrentProgress();


                    //从子线程更新 UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setProgress(progress);
                            seekBar.setMax((int) musicService.getCurrentSong().duration);
                            textViewPosition.setText(TimeUtil.formatDuration(progress));
                            textViewDuration.setText(TimeUtil.formatDuration(musicService.getCurrentSong().duration));

                            textViewTitle.setText(musicService.getCurrentSong().title);
                        }
                    });
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
