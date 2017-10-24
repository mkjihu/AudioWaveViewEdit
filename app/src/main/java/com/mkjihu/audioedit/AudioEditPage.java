package com.mkjihu.audioedit;

import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.florescu.android.rangeseekbar.RangeSeekBar.OnRangeSeekBarChangeListener;

import com.androidquery.AQuery;
import com.jaygoo.widget.RangeSeekBar.OnRangeChangedListener;
import com.mkjihu.audioedit.obj.Player;
import com.mkjihu.audioedit.view.RangeSeekBar2;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;


public class AudioEditPage extends AppCompatActivity{

	
	public RangeSeekBar<Integer> seekbar1;
	public com.jaygoo.widget.RangeSeekBar seekbar2,seekbar3;
	public RelativeLayout lay1;
	public RangeSeekBar2 bar2;
	//private SeekBar mSeekBar;
	//private String path = "http://s2.music.catch.net.tw/Storage/questions/112817510139489.mp3";
	//private Player mPlayer;
	private AQuery aq;
	
	///----特殊
	int mexoi = 45000;//預設毫秒
	float pic=0;//每秒代表多少像素
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_edit_page);
		aq = new AQuery(this);
		fid();
		/*
		mPlayer = new Player(mSeekBar);
		aq.id(R.id.button1).clicked(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
		            @Override
		            public void run() {
		                mPlayer.playUrl(path);
		            }
		        }).start();
			}
		});
		*/
		
		
		
		lay1.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				Log.i("(╬ﾟдﾟ)", lay1.getWidth()+"_"+lay1.getMeasuredWidth());
				if (Build.VERSION.SDK_INT < 16) {
					lay1.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			    } else {
			    	lay1.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			    }
			}
		});
		
		
		seekbar1.setNotifyWhileDragging(true);
		seekbar1.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Integer>() {
			@Override
			public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
				Log.i("滑動1", minValue+"_"+maxValue);
				Log.i("滑動1", lay1.getWidth()+"_"+lay1.getMeasuredWidth());
			}
		});
		
		seekbar2.setValue(0, seekbar2.getMax());
		seekbar2.setOnRangeChangedListener(new OnRangeChangedListener() {
			@Override
			public void onRangeChanged(com.jaygoo.widget.RangeSeekBar view, float min, float max, boolean isFromUser) {
				Log.i("滑動2", min+"_"+max);
				//bar2.setValue(min, max);
				
			}
		});
		
		
		seekbar3.setValue(50);//-設置到中心點--計算毫秒代表
		{
			float tac = mexoi/1000;//換算成秒
			pic = 100/tac;//將100平分給每秒位元 = 每秒代表多少像素
			LogU.i("(╬ﾟдﾟ)",pic*15+"");
			bar2.setValue(50-(pic*15), 50+(pic*15));//-設置給區間-往前往後各15格
		}
		
		
		seekbar3.setOnRangeChangedListener(new OnRangeChangedListener() {
			@Override
			public void onRangeChanged(com.jaygoo.widget.RangeSeekBar view, float min, float max, boolean isFromUser) {
				try {
					bar2.setValue(min-(pic*15), min+(pic*15));//-設置給區間-往前往後各15格
					Log.i("滑動2", min-(pic*15)+"_"+min+(pic*15));
				} catch (Exception e) {
					if (min>50) {
						bar2.setValue(100-(pic*30), 100);//-設置給區間-往前往後各15格
					} else {
						bar2.setValue(0, (pic*30));//-設置給區間-往前往後各15格
					}
				}
			}
		});
	}

	private void fid() {
		seekbar1 = (RangeSeekBar<Integer>)findViewById(R.id.seekbar1);
		seekbar2 = (com.jaygoo.widget.RangeSeekBar)findViewById(R.id.seekbar2);
		bar2 = (RangeSeekBar2)findViewById(R.id.bar2);
		lay1 = (RelativeLayout)findViewById(R.id.lay1);
		//mSeekBar = (SeekBar)findViewById(R.id.seekBar3);
		
		seekbar3 = (com.jaygoo.widget.RangeSeekBar)findViewById(R.id.seekbar3);
		bar2.setValue(0,bar2.getMax());
	}

	



}
