package com.yinyxn.music;

import java.io.Serializable;

/**
 * Created by yinyxn on 2015/12/23.
 */
public class Song implements Serializable{//实现序列化

    long id;
    String data;
    String title;
    String artist;
    String album;
    long duration;

    public Song() {
    }

    public Song(long id, String data, String title, String artist, String album, long duration) {
        this.id = id;
        this.data = data;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Song{" +
                "id=" + id +
                ", data='" + data + '\'' +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", duration=" + duration +
                '}';
    }
}
