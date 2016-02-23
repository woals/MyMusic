package com.yinyxn.music;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by yinyxn on 2015/12/24.
 */
public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> data;
    private Context context;
    private LayoutInflater layoutInflater;

    public SongAdapter(Context context, ArrayList<Song> data) {
        this.context = context;
        this.data = data;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if(convertView == null){
            convertView = layoutInflater.inflate(R.layout.song_item,parent,false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else {
            //有holder 从Tag中取
            holder = (ViewHolder) convertView.getTag();
        }

        //绑定数据
        holder.bindData(data.get(position));


        return convertView;
    }
    static class ViewHolder {
        TextView title;
        TextView artist;
        TextView duration;

        public ViewHolder(View view) {
            title = (TextView) view.findViewById(R.id.textView_title);
            artist = (TextView) view.findViewById(R.id.textView_artist);
            duration = (TextView) view.findViewById(R.id.textView_duration);

        }

        public void bindData(Song song) {
            title.setText(song.title);
            artist.setText(song.artist);
            duration.setText(TimeUtil.formatDuration(song.duration));
        }
    }
}
