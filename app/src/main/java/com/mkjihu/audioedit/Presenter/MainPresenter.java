package com.mkjihu.audioedit.Presenter;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import org.reactivestreams.Subscription;

import com.mkjihu.audioedit.AudioAdapder;
import com.mkjihu.audioedit.AudioItem;
import com.mkjihu.audioedit.LogU;
import com.mkjihu.audioedit.MainActivity;

import android.database.Cursor;
import android.provider.MediaStore;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

public class MainPresenter {


	public CompositeDisposable disposable;
	public MainActivity mainActivity;
	public MainPresenter(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
		disposable = new CompositeDisposable();
	}
	
	public void GetAudio()
	{
		Disposable d = Flowable.just("")
				.subscribeOn(Schedulers.io())
				.doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription arg0) throws Exception {
                    	mainActivity.openRefresh();//显示加载-解析資料中
                    }
                })
				.subscribeOn(AndroidSchedulers.mainThread()) // 指定上面的doOnSubscribe跑主线程
				.map(new Function<String, AudioAdapder>() {
					@Override
					public AudioAdapder apply(String arg0) throws Exception {
						return new AudioAdapder(mainActivity, Audio());
					}
				})
				.unsubscribeOn(Schedulers.io())//允許取消訂閱
				.observeOn(AndroidSchedulers.mainThread())//--結果在主線程中顯示
                .subscribeWith(new DisposableSubscriber<AudioAdapder>() {
                    @Override  
                    public void onNext(AudioAdapder adapder) {
                    	mainActivity.adasp(adapder);
                    	mainActivity.offRefresh();
                    }
                    @Override
                    public void onError(Throwable e) {
                        LogU.i("完成GG", e.getMessage());
                        mainActivity.offRefresh();
                    }
                    @Override
                    public void onComplete() {}
                });
				
		disposable.add(d);	
	}
	
	//透過MediaStore的方式查询数据库 - ContentProvider
	private ArrayList<AudioItem> Audio() {
		/**
		 * query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
		Uri :这个Uri代表要查询单额数据库名称加上表的名称，一般直接从MediaStroe里获取的信息，
		例如我们要取得所有的歌曲信息，就必须利用MediaStore.Audio.Media.EXTERNAL_CONTENT_RUI这个Uri
		。专辑信息要利用MediaStore.Audio.Albums.EXTERNA_CONTENT_URI这个Uri来查询，其他的也类似。
		Prjs:代表要从表中选择的列，用一个StringS数组来表示。
		Selections:相当于SQL语句中的where子句，就是代表你的查询条件。
		selectArgs：这个参数是说你的Selections里有？这个符号是，这里可以以实际值代替这个问号。如果Selections这个没有？的话，那么这个String数组可以为null。
		Order:说明查询结果按什么来排序
		
		MediaStore.Audio.Media.INTERNAL_CONTENT_URI 是获取系统内置音乐的参数
		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI 是获取系统外置SD卡音乐的参数。
		*/
		
		Cursor cursor = mainActivity.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER); //-得到系统的所有音乐
		
		ArrayList<AudioItem> audioItems = new ArrayList<>();
		
		for (int i=0;i<cursor.getCount();i++){
			cursor.moveToNext();  
			long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)); // 音乐id 
			String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)); // 文件路径 
			String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)); // 歌手
			String title = cursor.getString((cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))); // 音乐标题 
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)); // 时长  
            long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)); // 文件大小
            
            
            File file = new File(url); 
            String title2 = file.getName();
            
            
            AudioItem item= new AudioItem();
            item.setId(id);
            item.setUrl(url);
            item.setTitle(toUtf8(title2));
            item.setDuration(formatTime(duration));
            if(artist==null){
            	item.setArtist("");
            } else{
            	item.setArtist(toUtf8(artist));
            }
            item.setSize(size);
            
            String type="";
            if (url.endsWith(".mp3")) {
            	type="mp3";
			}
            else if(url.endsWith(".wav")){
            	type="wav";
            }
            else if(url.endsWith(".m4a")){
            	type="m4a";
            }	
            else if(url.endsWith(".ogg")){
            	type="ogg";
            }
            else{
            	type="0";
            }
            LogU.i("名称", title2);
            LogU.i("时长", duration+"");
            LogU.i("路径", url);
            item.setType(type);
            audioItems.add(item);
		}
		return audioItems;
	}	
	
	public String toUtf8(String str) {
		try {
			return new String(str.getBytes("UTF-8"),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str;
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
	
	
}
