package com.mkjihu.audioedit;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.jakewharton.rxbinding2.view.RxView;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import io.reactivex.functions.Consumer;

public class AudioAdapder extends RecyclerView.Adapter<ViewHolder>{
	
	private ArrayList<AudioItem> audioItems;
	private MainActivity activity;
	private LayoutInflater mLayoutInflater;
	
	
	
	public AudioAdapder(MainActivity activity,ArrayList<AudioItem> audioItems) {
		this.audioItems = audioItems;
		this.activity = activity;
		mLayoutInflater = LayoutInflater.from(activity);
	}
	
	@Override
	public int getItemCount() {
		return audioItems.size();
	}

	@Override
	public void onBindViewHolder(ViewHolder arg0, int arg1) {
		ItemView indexItem = (ItemView)arg0;
		if(getItemCount()>0)  {
			indexItem.SetData(arg1);
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup arg0, int arg1) {
		//產生View
		return new ItemView(mLayoutInflater.inflate(R.layout.item_audio1,arg0, false));
	}
	
	public class ItemView extends ViewHolder
	{
		private LinearLayout ad_lay;
		private ImageView ad_image;
		private TextView ad_name,ad_artist,ad_duration;

		public ItemView(View itemView) {
			super(itemView);
			ad_lay = (LinearLayout)itemView.findViewById(R.id.ad_lay);
			ad_image = (ImageView)itemView.findViewById(R.id.ad_image);
			ad_name = (TextView)itemView.findViewById(R.id.ad_name);
			ad_artist = (TextView)itemView.findViewById(R.id.ad_artist);
			ad_duration = (TextView)itemView.findViewById(R.id.ad_duration);
		}
		
		public void SetData(final int arg1)
		{
			ad_name.setText(audioItems.get(arg1).getTitle());
			ad_artist.setText(audioItems.get(arg1).getArtist());
			ad_duration.setText(audioItems.get(arg1).getDuration());
			
			switch (audioItems.get(arg1).getType()) {
			case "mp3":
				ad_image.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.mp3));
				break;
			case "wav":
				ad_image.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.wav));
				break;
			case "m4a":
				ad_image.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.m4a));
				break;
			case "ogg":
				ad_image.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ogg));
				break;
			default:
				ad_image.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ftarn));
				break;
			}
			
			
	        RxView.clicks(ad_lay)
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe(new Consumer<Object>() {
                @Override
                public void accept(Object arg0) throws Exception {
                	LogU.i("测试",audioItems.get(arg1).getUrl());
                	activity.tosdg(audioItems.get(arg1).getUrl());
                }
            });
		}
		
	}
	
	
	
	
	
}
