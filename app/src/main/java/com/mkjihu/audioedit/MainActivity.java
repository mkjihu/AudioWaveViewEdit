package com.mkjihu.audioedit;


import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;

import com.mkjihu.audioedit.Presenter.MainPresenter;
import com.mkjihu.audioedit.obj.DialogBox;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

	public SwipeRefreshLayout mSwipeRefreshLayout;
	public RecyclerView recycler_view;
	public ProgressDialog progressDialog;
	public MainPresenter presenter;
	
	final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
	
	public ImageView imageView1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		fid();
		presenter = new MainPresenter(this);
		progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(true);
		progressDialog.setInverseBackgroundForced(false);
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.setMessage("掃描中...");
		presenter.GetAudio();
		

		
		try {
	        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				//申请WRITE_EXTERNAL_STORAGE权限
				ActivityCompat.requestPermissions(this, new String[]{
						Manifest.permission.READ_EXTERNAL_STORAGE
						,Manifest.permission.WRITE_EXTERNAL_STORAGE
						, Manifest.permission.READ_PHONE_STATE
						, Manifest.permission.RECORD_AUDIO
						, Manifest.permission.MODIFY_AUDIO_SETTINGS
						, Manifest.permission.WRITE_CONTACTS}, REQUEST_CODE_ASK_PERMISSIONS);
	        }
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		
	}
	
	
	
  	
	//權限同意返回
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case REQUEST_CODE_ASK_PERMISSIONS:
				if(verifyPermissions(grantResults)){
					Log.i("!!!", "同意");
				}
				else {
					Log.i("!!!", "不同意");
					DialogBox.getAlertDialog2(this, "提示", "請至設定之應用程式，開啟權限!");
				}
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
	
	
	public boolean verifyPermissions(int[] grantResults) {
        // At least one result must be checked.
        if(grantResults.length < 1){
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

	public void tosdg(String aas) {
		Intent intent = new Intent(this, SoundFilePage.class);
		intent.putExtra("va", aas);
		startActivity(intent);
	}

	
	private void fid() 
	{
		imageView1 = (ImageView)findViewById(R.id.imageView1);
		recycler_view = (RecyclerView)findViewById(R.id.recycler_view);
		recycler_view.setLayoutManager(new LinearLayoutManager(this));
		//下面这行代码就是添加分隔线的方法
		recycler_view.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
		mSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.mSwipeRefreshLayout);
		//设置下拉出现小圆圈是否是缩放出现，出现的位置，最大的下拉位置
		mSwipeRefreshLayout.setProgressViewOffset(true, 0, 200);
		//设置下拉圆圈的大小，两个值 LARGE， DEFAULT
		//mSwipeRefreshLayout.setSize(SwipeRefreshLayout.LARGE);
		// 设置下拉圆圈上的颜色，蓝色、绿色、橙色、红色
		mSwipeRefreshLayout.setColorSchemeResources(
		    android.R.color.holo_blue_bright,
		    android.R.color.holo_green_light,
		    android.R.color.holo_orange_light,
		    android.R.color.holo_red_light);
		mSwipeRefreshLayout.setOnRefreshListener(this);	
		
		imageView1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, RecordingPage.class);
				startActivity(intent);
			}
		});
		
	}
	public void adasp(RecyclerView.Adapter<ViewHolder> adapter) {
		recycler_view.setAdapter(adapter);
	}
	
	public void offRefresh() {
		mSwipeRefreshLayout.setRefreshing(false);
		//iwillPaint.dissdig();
	}
	public void openRefresh() {
		mSwipeRefreshLayout.setRefreshing(true);
	}
	@Override
	public void onRefresh() {
		//下拉更新執行
		
		new Handler().postDelayed(new Runnable()  {
			@Override
			public void run()  {
				presenter.GetAudio();
			}
		}, 1000);
	
	}
	@Override
	protected void onDestroy() {
		presenter.disposable.dispose();
		super.onDestroy();
	}
	
	
}
