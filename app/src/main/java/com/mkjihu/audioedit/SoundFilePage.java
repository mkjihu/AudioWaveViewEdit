package com.mkjihu.audioedit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.androidquery.AQuery;
import com.cokus.wavelibrary.view.WaveSurfaceView;
import com.cokus.wavelibrary.view.WaveformView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.PixelFormat;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jaygoo.widget.RangeSeekBar.OnRangeChangedListener;
import com.mkjihu.audioedit.Presenter.SoundFilePresenter;
import com.mkjihu.audioedit.obj.DialogBox;
import com.mkjihu.audioedit.utils.*;
import com.mkjihu.audioedit.view.RangeSeekBar2;



public class SoundFilePage extends AppCompatActivity {
	 
	
	public AQuery aq;
	public SoundFilePresenter presenter;
	public AudioTrack mAudioTrack;
	public Sound sound;
	public ProgressDialog  mProgressDialog,mProgressDialog2;
	public boolean mLoadingKeepGoing;
	private boolean mFinishActivity;
	//==============================
	public String va;
	private SamplePlayer mPlayer;
	
	private SeekBar seekBar;
	
	private boolean isJump = true;
	//========================================
	public WaveSurfaceView waveSfv;
	public WaveformView waveView;
	public SoundFile mSoundFile;
	
	public com.jaygoo.widget.RangeSeekBar seekbar2;
	public RangeSeekBar2 seekbar3;
	private float mMin=0,mMax=100;
	//===============================
	
	public EditText toed1,toed2;
	public TextView itemtx;
	public Button catere;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sound_file_page);
		aq= new AQuery(this);
		presenter = new SoundFilePresenter(this);	
		fid();
		
		va = getIntent().getExtras().getString("va").replaceFirst("file://", "").replaceAll("%20", " ");
		mPlayer = null;
		presenter.ReadFile2(va);
		
		
		aq.id(R.id.button1).clicked(aod);
		aq.id(R.id.button2).clicked(aod);
		aq.id(R.id.button3).clicked(aod);
		aq.id(R.id.catere).clicked(aod);
	}
    
	
	
	public OnClickListener aod = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button1:
				//presenter.ReadFile();
				break;
			case R.id.button2:
				start();
				break;
			case R.id.button3:
				//onPlay(0);
				onPlay2(0);
				break;
			case R.id.catere:
				Catg();
				break;
			}
		}

		
	};
	private void Catg() {
		//File file = new File(va); 
        //String title2 = file.getName();
        //LogU.i("title2",title2);
        
        SongMetadataReader metadataReader = new SongMetadataReader(SoundFilePage.this, va);
        LogU.i("title2",metadataReader.mTitle);
        
        double  kdk = (mPlayEndMsec * (mMin/100));//--算出seekbar對區間起始的應百分比
        
        int frames = waveView.millisecsToPixels((int)kdk);//-取得該相素位置
        
        double startTime = waveView.pixelsToSeconds(frames);
        int startFrame = waveView.secondsToFrames(startTime);//秒到帧
        LogU.i("1",kdk+"");
        LogU.i("frames",frames+"");
        LogU.i("startTime",startTime+"");
        LogU.i("startFrame",startFrame+"");
        //frames = frames/2;
        double  kdk2 = (mPlayEndMsec * (mMax/100));//--算出seekbar對區間起始的應百分比
        int frames2 = waveView.millisecsToPixels((int)kdk2);//-取得該相素位置
        
        double endTime = waveView.pixelsToSeconds(frames2);
        int endFrame = waveView.secondsToFrames(endTime);
        //frames2 = frames2/2;
        LogU.i("2",kdk2+"");
        LogU.i("frames2",frames2+"");
        LogU.i("endTime",endTime+"");
        LogU.i("endFrame",endFrame+"");
        int duration = (int)((kdk2 - kdk)/1000 +0.5);
        LogU.i("duration",duration+"");
        presenter.CutReadFile(metadataReader.mTitle+"_2",startFrame,endFrame,duration, mSoundFile);
	}
	
	
	
	//-解析完成
	public void Loading(SoundFile mSoundFile)
	{
		 mPlayer = new SamplePlayer(mSoundFile);
		 mProgressDialog.setMax(mSoundFile.getFileSizeBytes());//取得最大長度
		 this.mSoundFile = mSoundFile;
		 mLoadingKeepGoing = true;
		 oiruyh();
		 
		 //--设置seek
	     mPlayEndMsec = waveView.pixelsToMillisecsTotal();
		 Log.i("设置seek", "设置seek"+mPlayEndMsec);
		 seekBar.setMax(mPlayEndMsec);
		 toed1.setText(formatTime(0));
		 toed2.setText(formatTime(mPlayEndMsec));
		 
	}
	
	
	private int mPlayStartMsec;
	private int mPlayEndMsec;
	public Handler updateTime= new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			try { 
				updateDisplay();
			} catch (Exception e) {}
		}
	};
	
	public Timer timer = new Timer();  
	public MyTimerTask timerTask = new MyTimerTask();  
	class MyTimerTask extends TimerTask {  
	    @Override  
	    public void run() {//这里不能处理UI操作  
	    	Message message = updateTime.obtainMessage(0);   
			updateTime.sendMessage(message);
	    }  
	}  

    
    /**播放音频，@param startPosition 开始播放的时间*/
    private synchronized void onPlay2(int startPosition) {
        if (mPlayer == null)
            return;
        
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
            Log.i("停止一次", "停止一次");
            timer.cancel();
            timer.purge();
            timer = new Timer();
        }
        
        mPlayEndMsec = waveView.pixelsToMillisecsTotal();
        seekBar.setMax(mPlayEndMsec);
        
        int kdk = (int)(mPlayEndMsec * (mMin/100));//--算出seekbar對區間起始的應百分比
        Log.i("淦", mMin+"應百分比"+kdk);
        int frames = waveView.millisecsToPixels(kdk);//-取得該相素位置
        
        mPlayStartMsec = waveView.pixelsToMillisecs(frames);
        Log.i("淦", mPlayStartMsec+"");
        
        mPlayer.setOnCompletionListener(new SamplePlayer.OnCompletionListener() {
            @Override
            public void onCompletion() {
            	handlePause();
                waveView.setPlayback(-1);
                updateDisplay();
                //updateTime.removeMessages(UPDATE_WAV);//将handler对应message queue里的消息清空
                Toast.makeText(getApplicationContext(),"播放完成",Toast.LENGTH_LONG).show();
            }
        });
        mPlayer.seekTo(mPlayStartMsec);
        mPlayer.start();
        
        
        timer.schedule(new MyTimerTask(), 10, 5); // 从现在起过0.01秒以后，每隔0.005秒执行一次。  
        
    } 
    
   
    
    private synchronized void handlePause(){
    	 if (mPlayer != null && mPlayer.isPlaying()) {//播放中
         	 //Log.i("播放中", "播放中");
             mPlayer.pause();
             timer.cancel();
             timer.purge();
             timer = new Timer();
             //updateTime.removeMessages(UPDATE_WAV);
         }
    }
    /**更新upd
     * ateview 中的播放进度*/
    private void updateDisplay() {
            int now = mPlayer.getCurrentPosition();// nullpointer
            //LogU.i("當前PINT", now+""+MyApplication.isInMainThread());
            if (isJump) {
				seekBar.setProgress(now);
				itemtx.setText(formatTime(now));
			}
           
            int frames = waveView.millisecsToPixels(now);//-取得該相素位置
            waveView.setPlayback(frames);//通过这个更新当前播放的位置
            
            int kdk = (int)(mPlayEndMsec * (mMax/100));//--算出seekbar對區間起始的應百分比
            //int frames2 = waveView.millisecsToPixels(kdk);//-取得該相素位置
            //int frames3= waveView.pixelsToMillisecs(frames2);
            
            
            if (now >= kdk) {
            	waveView.setPlayFinish(1);
                if (mPlayer != null && mPlayer.isPlaying()) {
                	handlePause();
                    mPlayer.seekTo(mPlayStartMsec);
                    itemtx.setText(formatTime(mPlayStartMsec));
                    LogU.i("當前PINT", now+""+MyApplication.isInMainThread());
                    LogU.i("OK", "播玩了"+now);
                    LogU.i("OK", "播玩了"+kdk);
                }
            }else{
                waveView.setPlayFinish(0);
            }
            /*
            if (now >= mPlayEndMsec ) {
                waveView.setPlayFinish(1);
                if (mPlayer != null && mPlayer.isPlaying()) {
                    mPlayer.pause();
                    //updateTime.removeMessages(UPDATE_WAV);
                    timer.cancel();
                    timer.purge();
                    timer = new Timer();
                    mPlayer.seekTo(0);
                    //seekBar.setProgress(0);
                    LogU.i("當前PINT", now+""+MyApplication.isInMainThread());
                    LogU.i("OK", "播玩了");
                }
            }else{
                waveView.setPlayFinish(0);
            }
            */
            waveView.invalidate();//刷新视图
    }
	

	public void oiruyh() {
		 if (mLoadingKeepGoing) {
             Runnable runnable = new Runnable() {
                 public void run() {
                     finishOpeningSoundFile();
                     waveSfv.setVisibility(View.INVISIBLE);
                     waveView.setVisibility(View.VISIBLE);
                 }
             };
             this.runOnUiThread(runnable);
         }
	}
	
	float mDensity;
    /**waveview载入波形完成*/
    private void finishOpeningSoundFile() {
        waveView.setSoundFile(mSoundFile);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;
        waveView.recomputeHeights(mDensity);
    }
	
    
    
    
    
	private void fid() {
		waveSfv = (WaveSurfaceView)findViewById(R.id.wavesfv);
		waveView = (WaveformView)findViewById(R.id.waveview);
		seekBar = (SeekBar)findViewById(R.id.sakker);
		seekbar2 = (com.jaygoo.widget.RangeSeekBar)findViewById(R.id.seekbar2);
		seekbar3 = (RangeSeekBar2)findViewById(R.id.bar2);
		toed1 = (EditText)findViewById(R.id.toed1);
		toed2 = (EditText)findViewById(R.id.toed2);
		itemtx = (TextView)findViewById(R.id.itemtx);
		toed1.setKeyListener(null);
		toed2.setKeyListener(null);
		
        if(waveSfv != null) {
            waveSfv.setLine_off(42);
            //解决surfaceView黑色闪动效果
            waveSfv.setZOrderOnTop(true);
            waveSfv.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }
        waveView.setLine_offset(42);
        mLoadingKeepGoing = true;
        mFinishActivity = false;
        mProgressDialog  = DialogBox.Prate(this,"");
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
        	 public void onCancel(DialogInterface dialog) {
                 mLoadingKeepGoing = false;
                 mFinishActivity = true;
             }
        });
        mProgressDialog2 = DialogBox.Prate2(this,"音頻編碼重組");
        /* */
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            	//Log.i("操作控制", "摸上時觸發");
            	isJump = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            	//Log.i("操作控制", "放開時觸發"+seekBar.getProgress());
           		isJump = true;
           		//onPlay(seekBar.getProgress());
           		if (mPlayer == null)
                    return;
                
                if (mPlayer != null && mPlayer.isPlaying()) {
                    mPlayer.pause();
                    Log.i("停止一次", "停止一次");
                    timer.cancel();
                    timer.purge();
                    timer = new Timer();
                }
                mPlayer.seekTo(seekBar.getProgress());
                mPlayer.start();
                
                timer.schedule(new MyTimerTask(), 10, 5); // 从现在起过0.01秒以后，每隔0.005秒执行一次。  
           		
            }
        });
       
        seekbar3.setValue(0, seekbar3.getMax());
        seekbar2.setValue(0, seekbar2.getMax());
		seekbar2.setOnRangeChangedListener(new OnRangeChangedListener() {
			@Override
			public void onRangeChanged(com.jaygoo.widget.RangeSeekBar view, float min, float max, boolean isFromUser) {
				//Log.i("滑動2", min+"_"+max);
				seekbar3.setValue(min, max);
				seekbar2.setLeftProgressDescription((int)min+"%");
                seekbar2.setRightProgressDescription((int)max+"%");
                mMin = min;
                mMax = max;
                int kdk = (int)(mPlayEndMsec * (mMin/100));//--算出seekbar對區間起始的應百分比
                toed1.setText(formatTime(kdk));
                
                int kdk2 = (int)(mPlayEndMsec * (mMax/100));//--算出seekbar對區間起始的應百分比
       		 	toed2.setText(formatTime(kdk2));
			}
		});
	}
	
	
	@Override
    protected void onDestroy() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying() || mPlayer.isPaused()) {
                mPlayer.stop();
            }
            mPlayer.release();
            timer.cancel();
            timer.purge();
            //mPlayer = null;
        }
        try {
        	mAudioTrack.pause();  // pause() stops the playback immediately.
            mAudioTrack.stop();   // Unblock mAudioTrack.write() to avoid deadlocks.
		} catch (Exception e) {
			// TODO: handle exception
		}
       
        super.onDestroy();
    }	   
	/**
	 * 格式化时间，将毫秒转换为分:秒格式
	 * @param time
	 * @return
	 */
	public String formatTime(long time) {
		// TODO Auto-generated method stub
		String min = time / (1000 * 60) + "";
		String sec = time % (1000 * 60) + "";
		if (min.length() < 2) {
			min = "0" + time / (1000 * 60) + "";
		} else {
			min = time / (1000 * 60) + "";
		}
		if (sec.length() == 4) {
			sec = "0" + (time % (1000 * 60)) + "";
		} else if (sec.length() == 3) {
			sec = "00" + (time % (1000 * 60)) + "";
		} else if (sec.length() == 2) {
			sec = "000" + (time % (1000 * 60)) + "";
		} else if (sec.length() == 1) {
			sec = "0000" + (time % (1000 * 60)) + "";
		}
		return min + ":" + sec.trim().substring(0, 2);
	}
	//////////////////////////////////////////////////////測試區////////////////////////////////////////////////////////////////////
	private Thread mPlayThread;
	private boolean mKeepPlaying;
	private int mPlaybackStart; //起始偏移量
	private short[]  mBuffer;
	
	public void start() {
		Log.i("sound.getmNumSamples()", sound.getmNumSamples()+"");
        mAudioTrack.setNotificationMarkerPosition(sound.getmNumSamples() - 1 - 0);
        
        mAudioTrack.setPlaybackPositionUpdateListener(
                new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onPeriodicNotification(AudioTrack track) {
            	LogU.i("Aud1", "1");
            }

            @Override
            public void onMarkerReached(AudioTrack track) {
            	LogU.i("Aud2", "2");
            }
        });
        
        mKeepPlaying = true;
        mAudioTrack.flush();
        mAudioTrack.play();
        mPlayThread = new Thread () {
            public void run() {
                int position = mPlaybackStart * sound.getmChannels();
                sound.getmSamples().position(position);
                int limit = sound.getmNumSamples() * sound.getmChannels();
                Log.i("寫入", "寫入"+limit);
                while (sound.getmSamples().position() < limit && mKeepPlaying) {
                    int numSamplesLeft = limit - sound.getmSamples().position();
                    if(numSamplesLeft >= mBuffer.length) {
                    	sound.getmSamples().get(mBuffer);
                    } else {
                        for(int i=numSamplesLeft; i<mBuffer.length; i++) {
                            mBuffer[i] = 0;
                        }
                        sound.getmSamples().get(mBuffer, 0, numSamplesLeft);
                    }
                    //使用以ByteBuffer為參數的寫入方法
                    Log.i("寫入", "寫入");
                    mAudioTrack.write(mBuffer, 0, mBuffer.length);
                }
            }
        };
        mPlayThread.start();
    }
	
	public void a(AudioTrack mAudioTrack)
	{
		this.mAudioTrack = mAudioTrack;
	}
	public void b(Sound sound,short[]  mBuffer)
	{
		this.sound = sound;
		this.mBuffer = mBuffer;
	}
	
}