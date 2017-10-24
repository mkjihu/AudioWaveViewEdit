package com.mkjihu.audioedit;


import java.nio.ByteBuffer;

import com.androidquery.AQuery;
import com.cokus.wavelibrary.draw.WaveCanvas;
import com.cokus.wavelibrary.view.WaveSurfaceView;
import com.mkjihu.audioedit.Presenter.RecordingPresenter;
import com.mkjihu.audioedit.view.Chronometer;

import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

public class RecordingPage extends AppCompatActivity {

	public AQuery aq;
	
	public WaveSurfaceView waveSfv;
	public WaveCanvas waveCanvas;
	private ProgressDialog logdialogs;
	public RecordingPresenter presenter;
	public int RegType = 0;
	private String fileName = "MergePCM";//完成合併後的文件名;
	
	private EditText editText;
	public Chronometer chronometer1;
	public long escapeTime = 0;
	
	//https://github.com/adrielcafe/AndroidAudioConverter
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recording_page);
		aq = new AQuery(this);
		fid();
		presenter = new RecordingPresenter(this,waveSfv);
		aq.id(R.id.bt1).clicked(ls);
		aq.id(R.id.bt2).clicked(ls);
		aq.id(R.id.bt3).clicked(ls);
		aq.id(R.id.bt1).text("開始");
		RegType = 0;
	}
	
	public OnClickListener ls = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.bt1:
				waveSfv.setVisibility(View.VISIBLE);
				if (RegType==0) {//按下开始
					
					aq.id(R.id.bt1).text("暫停");
					presenter.StartReg();
					RegType = 1;
					chronometer1.setBase(SystemClock.elapsedRealtime() + escapeTime);
					chronometer1.start();
				}
				else {//按下暫停
					
					aq.id(R.id.bt1).text("開始");//-呼叫暫停
					presenter.Stop(0);//暫停
					RegType = 0;
					
					escapeTime = chronometer1.getBase() - SystemClock.elapsedRealtime();
					chronometer1.stop();
				}
				
				break;
			case R.id.bt2:
				aq.id(R.id.bt1).text("開始");
				presenter.Stop(0);//暫停
				presenter.clearFiles();
				presenter.clidview();
				RegType = 0;
				escapeTime = 0;
				
				
				chronometer1.stop();
				chronometer1.setBase(SystemClock.elapsedRealtime());
				
				
				break;
			case R.id.bt3:
				
				if (!editText.getText().toString().equals("")) {
					fileName = editText.getText().toString();
				}
				presenter.steTitle(fileName);
				if (RegType==1) {//播放狀態
					//presenter.Stop(1);
					Toast.makeText(RecordingPage.this, "請先暫停錄音", Toast.LENGTH_SHORT).show();
				}
				else{//已暫停狀態
					presenter.Stop(2);
				}
				
				break;
			}
		}
	};
	
	public void toSidie(final String Path)
	{
		
		Builder builder = new Builder(this);
        builder.setTitle("訊號轉換完成");
        builder.setCancelable(false);
        builder.setMessage("檔案路徑："+Path);
        //設定Negative按鈕資料
        builder.setNegativeButton("確定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) 
            { 
            	Intent intent = new Intent(RecordingPage.this, SoundFilePage.class);
        		intent.putExtra("va", Path);
        		startActivity(intent);
        		finish();
            }
        });
        builder.create().show();
	}
	

	@Override
	protected void onDestroy() {
		try {
			presenter.destroy();
		} catch (Exception e) { }
		super.onDestroy();
	}



	private void fid() {
		waveSfv = (WaveSurfaceView)findViewById(R.id.wavesfv);
		if(waveSfv != null) {
            waveSfv.setLine_off(42);
            //解决surfaceView黑色闪动效果
            waveSfv.setZOrderOnTop(true);
            waveSfv.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }
		logdialogs = new ProgressDialog(this);
		logdialogs.setCancelable(false);
		logdialogs.setInverseBackgroundForced(false);
		logdialogs.setCanceledOnTouchOutside(false);
		logdialogs.setMessage("訊號轉換中...");
		chronometer1 = (Chronometer)findViewById(R.id.chronometer1);
		editText = (EditText)findViewById(R.id.editText1);
	}
	   
	//--顯示加載
	public void showdia() {
		if(logdialogs!=null && !logdialogs.isShowing()) {
			logdialogs.show();
		}
	}			
	//此处关闭加載
	public void disdia() {
		if(logdialogs!=null && logdialogs.isShowing()) {
			logdialogs.dismiss();
		}
	}	

	//==================================================================================================================================================
	
	
	
    //轉換為短字節
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }
    
    //byte[] 轉換 short[]
    public static short[] shortMe(byte[] bytes) {
        short[] out = new short[bytes.length / 2]; // will drop last byte if odd number
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        for (int i = 0; i < out.length; i++) {
            out[i] = bb.getShort();
        }
        return out;
    }
}
