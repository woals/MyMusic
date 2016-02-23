package com.yinyxn.music;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private static final String TAG = "MusicService";

    MediaPlayer mediaPlayer;

    //音乐列表
    ArrayList<Song> songList = new ArrayList<>();

    //当前播放曲目
    private int current;

    volatile boolean isInit = false;

    private Bitmap bitmap;

    private Song currentSong;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");


        bitmap = BitmapFactory.decodeResource(
                getResources(), R.drawable.sprite7_strip16
        );

//        int bh = bitmap.getHeight();
//        int bw = bitmap.getWidth();
//
//        int h = bh/2;
//        int w = bw/8;
//
//        bitmap = Bitmap.createBitmap(bitmap,bw,bh,w,h);


        mediaPlayer = new MediaPlayer();
        // 播放完成监听器
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                try {
                    // 播放完成之后，播放下一首
                    playNext();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });

        //使用子线程加载音乐列表
        new Thread() {
            @Override
            public void run() {
                super.run();

                //查询
                Cursor cursor = getContentResolver().query(
                        Media.EXTERNAL_CONTENT_URI,
                        new String[]{Media._ID, Media.DATA, Media.TITLE, Media.ARTIST, Media.ALBUM, Media.DURATION},
                        "is_music != ?",
                        new String[]{"0"},
                        null);
                while (cursor.moveToNext()) {//取数据
                    long id = cursor.getLong(0);
                    String data = cursor.getString(1);
                    String title = cursor.getString(2);
                    String artist = cursor.getString(3);
                    String album = cursor.getString(4);
                    long duration = cursor.getLong(5);

                    //加到音乐列表
                    songList.add(new Song(id, data, title, artist, album, duration));
                }
                cursor.close();

                //作用是：防止线程还没有读取完东西就发广播（保证初始化（加载音乐）完毕之后发广播）
                //在这儿是从服务中获得数据
                isInit = true;

                for (Song song : songList) {
//                    Log.d(TAG, song.toString());
                }

                //在这儿是从广播中获得数据
//                isInit = true;

                //发广播
                Intent intent = new Intent(App.LOAD_SONG_LIST);

                Bundle bundle = new Bundle();
                bundle.putSerializable(App.EXTRA_SONG_LIST, songList);
                intent.putExtras(bundle);

//                intent.putExtra(EXTRA_SONG_LIST,songList);

                LocalBroadcastManager.getInstance(getApplicationContext())
                        .sendBroadcast(intent);//通过意图发广播
            }
        }.start();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        //注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(App.PLAY);
        filter.addAction(App.PLAY_PREVIOUS);
        filter.addAction(App.PLAY_NEXT);

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        receiver,
                        filter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    //不绑定服务就得不到服务中东西
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return new LocalBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return true;//执行重新绑定
    }

    public class LocalBinder extends Binder {

        //获得服务引用
        public MusicService getService() {
            Log.d(TAG, "getService");
            return MusicService.this;
        }
    }
    //------------------------------------------------------------------------------------

    /**
     * 音乐列表是否初始化完成
     *
     * @return
     */
    public boolean isInit() {
        Log.d(TAG, "isInit");
        return isInit;
    }

    /**
     * 获得音乐列表
     * <p/>
     * 如果初始化完成(发广播)就读取这个
     *
     * @return
     */
    public ArrayList<Song> getSongList() {
        Log.d(TAG, "getSongList");
        return songList;
    }

    /**
     * 播放音乐
     *
     * @param position 曲目当前位置
     * @throws IOException
     */
    public void play(int position) throws IOException {

        current = position;

        // 重置，回到空闲状态
        mediaPlayer.reset();

        // 设置数据源
        mediaPlayer.setDataSource(songList.get(position).data);
        // 预处理 加载解码器相关数据
        mediaPlayer.prepare();
        // 开始播放
        mediaPlayer.start();

        Song song = songList.get(position);
        Intent intent = new Intent(App.PLAY_CURRENT);//广播名字
        intent.putExtra(App.EXTRA_SONG, song);//广播内容
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(intent);

        //重构
//        sendNotification(song);

        sendNotificationCompat(song);
    }

    private void sendNotificationCompat(Song song) {
        Notification not = new NotificationCompat.Builder(this)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setSmallIcon(R.drawable.mymusic)
                .setColor(Color.argb(100, 255, 0, 0))
                .setContentIntent(
                        PendingIntent.getActivity(
                                this,
                                1,
                                new Intent(getApplicationContext(), MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT))

//                .setLargeIcon(bitmap)
//                .setStyle(new NotificationCompat.BigPictureStyle()
//                        .bigPicture(bitmap))
//
//                .setStyle(new NotificationCompat.BigTextStyle()
//                        .setBigContentTitle("标题")
//                        .setSummaryText("摘要")
//                        .bigText("this is text that must be save"))


                                // 发广播  给服务
                                .addAction(android.R.drawable.ic_media_previous, "",
                                        PendingIntent.getBroadcast(this, 1, new Intent(App.PLAY_PREVIOUS), PendingIntent.FLAG_UPDATE_CURRENT))

                                .addAction(android.R.drawable.ic_media_pause, "",
                                        PendingIntent.getBroadcast(this, 1, new Intent(App.PLAY), PendingIntent.FLAG_UPDATE_CURRENT))

                                .addAction(android.R.drawable.ic_media_next, "",
                                        PendingIntent.getBroadcast(this, 1, new Intent(App.PLAY_NEXT), PendingIntent.FLAG_UPDATE_CURRENT))
                                .build();

        NotificationManagerCompat.from(getApplicationContext())
                .notify(1,not);
    }

    private void sendNotification(Song song) {
        //  发通知
        Notification nof = new Notification.Builder(getApplicationContext())
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setSmallIcon(android.R.drawable.ic_media_play)
//                .setOngoing(true)//是否正在执行中        通知不能被清掉
                .build();



        //切歌 有声音提示
//        nof.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS;

        // 从文件中找声音
//        nof.sound = Uri.fromFile();
//        nof.color
//        nof.ledARGB

        nof.flags = Notification.FLAG_FOREGROUND_SERVICE;

        // 点击做反应
        nof.contentIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,//请求码
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

//        NotificationManager nm = (NotificationManager) getApplicationContext()
//                .getSystemService(NOTIFICATION_SERVICE);
//
//        //发通知
//        nm.notify(1,nof);

        startForeground(1,nof);

//        stopForeground(true);
    }

    //播放暂停
    public void play() {

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    //播放下一首
    public void playNext() throws IOException {
        if (current == songList.size() - 1) {
            current = 0;
        } else {
            current++;
        }
        play(current);
    }

    //播放上一首
    public void playPrevious() throws IOException {
        if (current == 0) {
            current = songList.size() - 1;
        } else {
            current--;
        }
        play(current);
    }

    //获得当前播放进度（位置）
    public int getCurrentProgress(){
        return mediaPlayer.getCurrentPosition();
    }

    public Song getCurrentSong(){
        currentSong = songList.get(current);
        return currentSong;
    }

    //修改播放进度
    public int seekTo(int progress){
        mediaPlayer.seekTo(progress);

        return progress;
    }


    // 通知 的 广播接收器
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case App.PLAY:
                    play();
                    break;
                case App.PLAY_NEXT:
                    try {
                        playNext();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case App.PLAY_PREVIOUS:
                    try {
                        playPrevious();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };
}
