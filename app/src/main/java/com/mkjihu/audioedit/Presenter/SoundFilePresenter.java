package com.mkjihu.audioedit.Presenter;

import java.io.File;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.reactivestreams.Subscription;

import com.mkjihu.audioedit.LogU;
import com.mkjihu.audioedit.MyApplication;
import com.mkjihu.audioedit.Sound;
import com.mkjihu.audioedit.SoundFilePage;
import com.mkjihu.audioedit.obj.DialogBox;
import com.mkjihu.audioedit.obj.SdCardU;
import com.mkjihu.audioedit.utils.SoundFile;
import com.mkjihu.audioedit.utils.SoundFile.InvalidInputException;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

public class SoundFilePresenter {
	public CompositeDisposable disposable;
	public SoundFilePage soundFilePage;
	
	protected MediaCodec codec;
	public ByteBuffer mDecodedBytes; //原始音頻數據
	public int mFileSize;
	private int mNumSamples;  //每個頻道的樣本數。
	private ShortBuffer mDecodedSamples;  // 共享緩衝區
	private int mAvgBitRate;  //平均比特率（kbps）
	private int mNumFrames;
	private ProgressListener mProgressListener = null;
    private int[] mFrameGains;
    private int[] mFrameLens;
    private int[] mFrameOffsets;
    private AudioTrack mAudioTrack;
    private short[]  mBuffer;
    private long mLoadingLastUpdateTime;
    
    public String outPath;
    
	public SoundFilePresenter(SoundFilePage soundFilePage) {
		this.soundFilePage= soundFilePage;
		disposable = new CompositeDisposable();
	}
	
	/**
		AudioManager：这个主要是用来管理Audio系统的
		AudioTrack：这个主要是用来播放声音的
		AudioRecord：这个主要是用来录音的 
	
		AudioTrack播放声音时不能直接把wav文件传递给AudioTrack进行播放
		必须传递buffer，通过write函数把需要播放的缓冲区buffer传递给AudioTrack，然后才能播放。
	*/
	
	
	public void ReadFile2(String mFilename) {
		Disposable d = Flowable.just(mFilename)
				.subscribeOn(Schedulers.io())
				.doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription arg0) throws Exception {
                    	//显示加载-解析資料中
                    	soundFilePage.mProgressDialog.show();
                    }
                })
				.subscribeOn(AndroidSchedulers.mainThread()) // 指定上面的doOnSubscribe跑主线程
				.map(new Function<String, SoundFile>() {
					@Override
					public SoundFile apply(String arg0) throws Exception {
						
						SoundFile.ProgressListener listener =
					            new SoundFile.ProgressListener() {
					                public boolean reportProgress(double fractionComplete) {
										//LogU.i("線程狀態", MyApplication.isInMainThread()+" "+fractionComplete);
					                	soundFilePage.runOnUiThread(new aeoe(fractionComplete));
					                	return soundFilePage.mLoadingKeepGoing;
					                }
					            };
						SoundFile mSoundFile = SoundFile.create(new File(arg0).getAbsolutePath(), listener);
									
						return mSoundFile;
					}
				})
				.unsubscribeOn(Schedulers.io())//允許取消訂閱
				.observeOn(AndroidSchedulers.mainThread())//--結果在主線程中顯示
                .subscribeWith(new DisposableSubscriber<SoundFile>() {
                    @Override  
                    public void onNext(SoundFile soundFile) {
                    	soundFilePage.Loading(soundFile);
                    	LogU.i("OK", "大OK");
                    }
                    @Override
                    public void onError(Throwable e) {
                    }
                    @Override
                    public void onComplete() {
                    	soundFilePage.mProgressDialog.dismiss();
                    }
                });
				
		disposable.add(d);	
		
	}
	public class aeoe implements Runnable{
		double fractionComplete;
		public aeoe(double fractionComplete) {
			this.fractionComplete= fractionComplete;
		}
		@Override
		public void run() {
			//LogU.i("線程狀態2", MyApplication.isInMainThread()+" "+fractionComplete);
			long now = getCurrentTime();
            if (now - mLoadingLastUpdateTime > 100) {
                soundFilePage.mProgressDialog.setProgress((int) (soundFilePage.mProgressDialog.getMax() * fractionComplete));
                mLoadingLastUpdateTime = now;
            }
		}
		
	}
	
	/**裁剪區段*/
	public void CutReadFile(final String title,final int startFrame,final int endFrame,final int duration,SoundFile mSoundFile) {
		
		Log.i("起始結束", startFrame+"__"+endFrame);
		
		Disposable d = Flowable.just(mSoundFile)
				.subscribeOn(Schedulers.io())
				.doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription arg0) throws Exception {
                    	//显示加载-解析資料中
                    	soundFilePage.mProgressDialog2.show();
                    }
                })
				.subscribeOn(AndroidSchedulers.mainThread()) // 指定上面的doOnSubscribe跑主线程
				.map(new Function<SoundFile, SoundFile>() {
					@Override
					public SoundFile apply(SoundFile mSoundFile) throws Exception {
						
						outPath = makeRingtoneFilename(title, ".m4a");	
						if (outPath == null) {
		                    throw Exceptions.propagate(new RuntimeException("解析錯誤1"));
		                }
						File outFile = new File(outPath);
						Boolean fallbackToWAV = false;
						try {
		                    // Write the new file
		                    mSoundFile.WriteFile(outFile,  startFrame, endFrame - startFrame);
		                } catch (Exception e) {
		                    // log the error and try to create a .wav file instead
		                    if (outFile.exists()) {
		                        outFile.delete();
		                    }
		                    StringWriter writer = new StringWriter();
		                    e.printStackTrace(new PrintWriter(writer));
		                    Log.e("Ringdroid", "Error: Failed to create " + outPath);
		                    Log.e("Ringdroid", writer.toString());
		                    fallbackToWAV = true;
		                }
						
						//如果创建.m4a文件失败，请尝试创建一个.wav文件。
						if (fallbackToWAV) {
							Log.i("创建.m4a文件失败", "创建.m4a文件失败");
							outPath = makeRingtoneFilename(title, ".wav");
							if (outPath == null) {
								throw Exceptions.propagate(new RuntimeException("解析錯誤2"));
		                    }
							outFile = new File(outPath);
							try {
		                        // create the .wav file
		                        mSoundFile.WriteWAVFile(outFile, startFrame, endFrame - startFrame);
		                    } catch (Exception e) {
		                    	//创建.wav文件也失败。 停止进度对话框，显示
		                    	
		                    	if (outFile.exists()) {
		                    		outFile.delete();
		                    	}
		                    	if (e.getMessage() != null && e.getMessage().equals("No space left on device")) {
		                    		throw Exceptions.propagate(new RuntimeException("設備上沒有剩餘空間")); 
		                        } else {
		                        	throw Exceptions.propagate(new RuntimeException("解析錯誤3")); 
		                        }
		                    	
		                    }
							
						}
						//加載新文件
						try {
							 final SoundFile.ProgressListener listener =
				                        new SoundFile.ProgressListener() {
				                            public boolean reportProgress(double frac) {
				                                // Do nothing - we're not going to try to
				                                // estimate when reloading a saved sound
				                                // since it's usually fast, but hard to
				                                // estimate anyway.
				                                return true;  // Keep going
				                            }
				                        };
				              SoundFile  mSoundFile2 = SoundFile.create(outPath, listener); 
				              
				              
				              afterSavingRingtone(title, outPath, duration);
				              
				              return mSoundFile2;
						} catch (Exception e) {
							throw Exceptions.propagate(new RuntimeException("解析錯誤4")); 
						}
						
						
						
					}
				})
				.unsubscribeOn(Schedulers.io())//允許取消訂閱
				.observeOn(AndroidSchedulers.mainThread())//--結果在主線程中顯示
                .subscribeWith(new DisposableSubscriber<SoundFile>() {
                    @Override  
                    public void onNext(final SoundFile soundFile) {
                    	
                    	LogU.i("OK", "大OK");
                    	soundFilePage.mProgressDialog2.setTitle("重新加載中..");
                    	
                    	new Handler().postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                            	soundFilePage.Loading(soundFile);
                            	soundFilePage.mProgressDialog2.dismiss();
                            	
                            	DialogBox.getAlertDialog1(soundFilePage, "裁剪完成", "檔案路徑："+outPath);
                            }
                        }, 3000);
                    	
                    }
                    @Override
                    public void onError(Throwable e) {
                    	LogU.i("錯誤", "大OK");
                    	DialogBox.getAlertDialog1(soundFilePage, "錯誤", e.getMessage());
                    	soundFilePage.mProgressDialog2.dismiss();
                    }
                    @Override
                    public void onComplete() {
                    }
                });
				
		disposable.add(d);	
	}
	
	
	
	
	private String makeRingtoneFilename(CharSequence title, String extension) {
	        String subdir;
	        String externalRootDir = Environment.getExternalStorageDirectory().getPath();
	        if (!externalRootDir.endsWith("/")) {
	            externalRootDir += "/";
	        }
	        /*
	        switch(mNewFileKind) {
	        default:
	        case FileSaveDialog.FILE_KIND_MUSIC:
	            // TODO(nfaralli): can directly use Environment.getExternalStoragePublicDirectory(
	            // Environment.DIRECTORY_MUSIC).getPath() instead
	            subdir = "media/audio/music/";
	            break;
	        case FileSaveDialog.FILE_KIND_ALARM:
	            subdir = "media/audio/alarms/";
	            break;
	        case FileSaveDialog.FILE_KIND_NOTIFICATION:
	            subdir = "media/audio/notifications/";
	            break;
	        case FileSaveDialog.FILE_KIND_RINGTONE:
	            subdir = "media/audio/ringtones/";
	            break;
	        }
	        */
	        //subdir = "media/audio/music/";
	        subdir =  "/music/";
	        
	        String parentdir = SdCardU.DATA_DIRECTORY + subdir;

	        // Create the parent directory
	        File parentDirFile = new File(parentdir);
	        parentDirFile.mkdirs();

	        //如果我們無法寫入特殊路徑，請嘗試寫入
	        //直接到sdcard
	        if (!parentDirFile.isDirectory()) {
	            parentdir = externalRootDir;
	        }

	        //將標題轉成文件名
	        String filename = "";
	        for (int i = 0; i < title.length(); i++) {
	            if (Character.isLetterOrDigit(title.charAt(i))) {
	                filename += title.charAt(i);
	            }
	        }

	        // Try to make the filename unique
	        String path = null;
	        for (int i = 0; i < 100; i++) {
	            String testPath;
	            if (i > 0)
	                testPath = parentdir + filename + i + extension;
	            else
	                testPath = parentdir + filename + extension;

	            try {
	                RandomAccessFile f = new RandomAccessFile(new File(testPath), "r");
	                f.close();
	            } catch (Exception e) {
	                // Good, the file didn't exist
	                path = testPath;
	                break;
	            }
	        }

	        return path;
	    }
	
	
	//--幫裁切片段加上資訊
	private void afterSavingRingtone(CharSequence title, String outPath, int duration){
		File outFile = new File(outPath);
        long fileSize = outFile.length();
        if (fileSize <= 512) {
            outFile.delete();
            throw Exceptions.propagate(new RuntimeException("裁剪片段過短，無法存成文件")); 
        }
        //創建數據庫記錄，指向現有的文件路徑
        String mimeType;
        if (outPath.endsWith(".m4a")) {
            mimeType = "audio/mp4a-latm";
        } else if (outPath.endsWith(".wav")) {
            mimeType = "audio/wav";
        } else {
            // This should never happen.
            mimeType = "audio/mpeg";
        }
        String artist = "" + "text";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, outPath);
        values.put(MediaStore.MediaColumns.TITLE, title.toString());
        values.put(MediaStore.MediaColumns.SIZE, fileSize);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

        values.put(MediaStore.Audio.Media.ARTIST, artist);
        values.put(MediaStore.Audio.Media.DURATION, duration);
        /*
        values.put(MediaStore.Audio.Media.IS_RINGTONE,mNewFileKind == FileSaveDialog.FILE_KIND_RINGTONE);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION,mNewFileKind == FileSaveDialog.FILE_KIND_NOTIFICATION);
        values.put(MediaStore.Audio.Media.IS_ALARM, mNewFileKind == FileSaveDialog.FILE_KIND_ALARM);
        values.put(MediaStore.Audio.Media.IS_MUSIC,mNewFileKind == FileSaveDialog.FILE_KIND_MUSIC);
		*/
        values.put(MediaStore.Audio.Media.IS_RINGTONE,false);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION,false);
        values.put(MediaStore.Audio.Media.IS_ALARM, false);
        values.put(MediaStore.Audio.Media.IS_MUSIC,true); 
        
        //插入數據庫
        Uri uri = MediaStore.Audio.Media.getContentUriForPath(outPath);
        soundFilePage.getContentResolver().insert(uri, values);
        
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	///================================================================================================
	//---轉PCM
	public void ReadFile() {
		Disposable d = Flowable.just("")
				.subscribeOn(Schedulers.io())
				.doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription arg0) throws Exception {
                    	//显示加载-解析資料中
                    	soundFilePage.mProgressDialog.show();
                    }
                })
				.subscribeOn(AndroidSchedulers.mainThread()) // 指定上面的doOnSubscribe跑主线程
				.map(new Function<String, Sound>() {
					@Override
					public Sound apply(String arg0) throws Exception {
						AssetFileDescriptor afd = soundFilePage.getAssets().openFd("aees.mp3");
						MediaExtractor extractor = new MediaExtractor();
						extractor.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
					     //获取多媒体文件信息
				        MediaFormat format = extractor.getTrackFormat(0);
				        //媒体类型
				        String mime = format.getString(MediaFormat.KEY_MIME);
				        // 检查是否为音频文件
				        if (!mime.startsWith("audio/")) {
				        	LogU.i("MP3RadioStreamPlayer", "不是音频文件!");
				        	throw Exceptions.propagate(new RuntimeException("解析錯誤"));
				        }
				        // 声道个数：单声道或双声道
				        int channelConfiguration = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
				        //时长1
				        //int bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
				        //时长2
				        long  duration = format.getLong(MediaFormat.KEY_DURATION);
				        // System.out.println("歌曲总时间秒:"+duration/1000000);
				        //-采样率    http://www.jianshu.com/p/90c235fc8d48
				        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
				        int expectedNumSamples = (int)((format.getLong(MediaFormat.KEY_DURATION) / 1000000.f) * sampleRate + 0.5f);//總樣本數
				        LogU.i("歌曲信息","媒体类型:" + mime + " 采样率:" + sampleRate + " 声道个数:" +channelConfiguration + " 總樣本數:" + expectedNumSamples);
					
				        
				        mProgressListener = new ProgressListener() {
							@Override
							public boolean reportProgress(double fractionComplete) {
								soundFilePage.runOnUiThread(new aeoe(fractionComplete));
								return soundFilePage.mLoadingKeepGoing;
							}
						};
				        
				        
				        
				        //========================================================
				        //mp3->pcm
				        
				        int i;
				        int mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
				        int mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
				        mFileSize = (int)afd.getLength();
				        //-设置进度条
				        soundFilePage.mProgressDialog.setMax(mFileSize);
				        
				        
			            //http://www.cnblogs.com/Sharley/p/5964490.html
				        int numTracks = extractor.getTrackCount();
				        for (i=0; i<numTracks; i++) {//遍历媒体轨道 此处我们传入的是音频文件，所以也就只有一条轨道
				            format = extractor.getTrackFormat(i);//获取音频轨道
				            if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
				                extractor.selectTrack(i);//选择此音频轨道
				                break;
				            }
				        }
				        if (i == numTracks) {
				        	throw Exceptions.propagate(new RuntimeException("解析錯誤2"));
				        }
				        
				        mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
				        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
				        // Expected total number of samples per channel.

				        MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));// 实例化一个指定类型的解码器,提供数据输出
				        codec.configure(format, null, null, 0);//启动MediaCodec ，等待传入数据
				        codec.start();

				        int decodedSamplesSize = 0;  //包含解碼樣本的輸出緩衝區的大小。
				        byte[] decodedSamples = null;//解碼樣本
				        ByteBuffer[] inputBuffers = codec.getInputBuffers();// 用来存放目标文件的数据 MediaCodec在此ByteBuffer[]中获取输入数据
				        ByteBuffer[] outputBuffers = codec.getOutputBuffers();//MediaCodec将解码后的数据放到此ByteBuffer[]中 我们可以直接在这里面得到PCM数据
				        int sample_size;
				        //-以下是解码的实现
				        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
				        long presentation_time;//演示時間
				        int tot_size_read = 0;
				        boolean done_reading = false;
				        
				        //將解碼的樣本緩衝區的大小設置為1MB（在44.1kHz的立體聲流為〜6秒）。
			            //對於較長的流，緩衝區大小將在以後增加，計算粗略
			            //估計存儲所有樣本所需的總大小，以便調整緩衝區的大小
			            //只有一次。
				        mDecodedBytes = ByteBuffer.allocate(1<<20);
				        Boolean firstSampleData = true;//第一個樣本數據
				        while (true) {
				        	//從文件讀取數據並將其提供給解碼器輸入緩衝區。
				            //-通过调用dequeueInputBuffer(long) 把buffer的所有权从codec编解码器交换给对象
				            int inputBufferIndex = codec.dequeueInputBuffer(100);//long kTimeOutUs = 10000; 如果 timeoutUs < 0 则一直等待，如果 timeoutUs > 0则等待对应时间.
				            if (!done_reading && inputBufferIndex >= 0) {
				                sample_size = extractor.readSampleData(inputBuffers[inputBufferIndex], 0);
				                if (firstSampleData
				                        && format.getString(MediaFormat.KEY_MIME).equals("audio/mp4a-latm")
				                        && sample_size == 2) {
				                    // 提供AAC流的前兩個字節，否則MediaCodec將會 崩潰 這兩個字節不包含音樂數據，但不包含基本信息（例如頻道配置和採樣頻率）                  
				                    extractor.advance();//MediaExtractor移动到下一取样处
				                    tot_size_read += sample_size;
				                } else if (sample_size < 0) {//小于0 代表所有数据已读取完成
				                    codec.queueInputBuffer(inputBufferIndex, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				                    done_reading = true;
				                } else {
				                    presentation_time = extractor.getSampleTime();
				                    codec.queueInputBuffer(inputBufferIndex, 0, sample_size, presentation_time, 0);//通知MediaDecode解码刚刚传入的数据
				                    extractor.advance();
				                    tot_size_read += sample_size;
				                    if (mProgressListener != null) {
				                        if (!mProgressListener.reportProgress((float)(tot_size_read) / mFileSize)) {
				                            // We are asked to stop reading the file. Returning immediately. The
				                            // SoundFile object is invalid and should NOT be used afterward!
				                            extractor.release();
				                            extractor = null;
				                            codec.stop();
				                            codec.release();
				                            codec = null;
				                        }
				                    }
				                }
				                firstSampleData = false;
				            }

				            //從解碼器輸出緩衝區獲取解碼流 解码数据为PCM 
				            int outputBufferIndex = codec.dequeueOutputBuffer(info, 100);
				            if (outputBufferIndex >= 0 && info.size > 0) {
				                if (decodedSamplesSize < info.size) {
				                    decodedSamplesSize = info.size;
				                    decodedSamples = new byte[decodedSamplesSize];//解碼樣本
				                }
				                outputBuffers[outputBufferIndex].get(decodedSamples, 0, info.size);//将Buffer内的数据取出到字节数组中
				                outputBuffers[outputBufferIndex].clear();//数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
					            //檢查緩衝區是否足夠大。 如果它太小，請調整大小
				                if (mDecodedBytes.remaining() < info.size) {
				                	//對總體大小進行粗略估計，分配20％以上  確保分配比初始大小至少5MB。
				                	int position = mDecodedBytes.position();
				                    int newSize = (int)((position * (1.0 * mFileSize / tot_size_read)) * 1.2);
				                    if (newSize - position < info.size + 5 * (1<<20)) {
				                        newSize = position + info.size + 5 * (1<<20);
				                    }
				                    ByteBuffer newDecodedBytes = null;
				                    //嘗試分配內存。 如果是OOM，運行垃圾回收器
				                    int retry = 10;
				                    while(retry > 0) {
				                        try {
				                            newDecodedBytes = ByteBuffer.allocate(newSize);
				                            break;
				                        } catch (OutOfMemoryError oome) {
				                            retry--;
				                        }
				                    }
				                    if (retry == 0) {//無法分配內存...停止讀取更多數據
				                        break;
				                    }
				                    //ByteBuffer newDecodedBytes = ByteBuffer.allocate(newSize);
				                    mDecodedBytes.rewind();
				                    newDecodedBytes.put(mDecodedBytes);
				                    mDecodedBytes = newDecodedBytes;
				                    mDecodedBytes.position(position);
				                }
				                mDecodedBytes.put(decodedSamples, 0, info.size);//PCM数据填充给inputBuffer
				                codec.releaseOutputBuffer(outputBufferIndex, false);//此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
				                
				            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				                outputBuffers = codec.getOutputBuffers();
				            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				                // Subsequent data will conform to new format.
				                // We could check that codec.getOutputFormat(), which is the new output format,
				                // is what we expect.
				            }
				            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
				                    || (mDecodedBytes.position() / (2 * mChannels)) >= expectedNumSamples) {
				                // We got all the decoded data from the decoder. Stop here.
				                // Theoretically dequeueOutputBuffer(info, ...) should have set info.flags to
				                // MediaCodec.BUFFER_FLAG_END_OF_STREAM. However some phones (e.g. Samsung S3)
				                // won't do that for some files (e.g. with mono AAC files), in which case subsequent
				                // calls to dequeueOutputBuffer may result in the application crashing, without
				                // even an exception being thrown... Hence the second check.
				                // (for mono AAC files, the S3 will actually double each sample, as if the stream
				                // was stereo. The resulting stream is half what it's supposed to be and with a much
				                // lower pitch.)
				                break;
				            }
				        }
				        mNumSamples = mDecodedBytes.position() / (mChannels * 2);  // One sample = 2 bytes.
				        mDecodedBytes.rewind();
				        mDecodedBytes.order(ByteOrder.LITTLE_ENDIAN);
				        mDecodedSamples = mDecodedBytes.asShortBuffer();
				        mAvgBitRate = (int)((mFileSize * 8) * ((float)mSampleRate / mNumSamples) / 1000);

				        extractor.release();
				        extractor = null;
				        codec.stop();
				        codec.release();
				        codec = null;

				        // Temporary hack to make it work with the old version.
				        mNumFrames = mNumSamples / getSamplesPerFrame();
				        if (mNumSamples % getSamplesPerFrame() != 0){
				            mNumFrames++;
				        }
				        mFrameGains = new int[mNumFrames];
				        mFrameLens = new int[mNumFrames];
				        mFrameOffsets = new int[mNumFrames];
				        int j;
				        int gain, value;
				        int frameLens = (int)((1000 * mAvgBitRate / 8) *
				                ((float)getSamplesPerFrame() / mSampleRate));
				        for (i=0; i<mNumFrames; i++){
				            gain = -1;
				            for(j=0; j<getSamplesPerFrame(); j++) {
				                value = 0;
				                for (int k=0; k<mChannels; k++) {
				                    if (mDecodedSamples.remaining() > 0) {
				                        value += Math.abs(mDecodedSamples.get());
				                    }
				                }
				                value /= mChannels;
				                if (gain < value) {
				                    gain = value;
				                }
				            }
				            mFrameGains[i] = (int)Math.sqrt(gain);  // here gain = sqrt(max value of 1st channel)...
				            mFrameLens[i] = frameLens;  // totally not accurate...
				            mFrameOffsets[i] = (int)(i * (1000 * mAvgBitRate / 8) *  //  = i * frameLens
				                    ((float)getSamplesPerFrame() / mSampleRate));
				        }
				        mDecodedSamples.rewind();
			            //=======================匯出資料=========================
			            Sound sound = new Sound();
			            
			            if (mDecodedSamples != null) {
			                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
			                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
			                    // Hack for Nougat where asReadOnlyBuffer fails to respect byte ordering.
			                    // See https://code.google.com/p/android/issues/detail?id=223824
			                	sound.setmSamples(mDecodedSamples);
			                } else {
			                	sound.setmSamples(mDecodedSamples.asReadOnlyBuffer());
			                }
			            }
			            sound.setmSampleRate(mSampleRate);
			            sound.setmNumSamples(mNumSamples);
			            sound.setmChannels(mChannels);
			            
			            Log.i("1", sampleRate+"");
			            Log.i("2", mNumSamples+"");
			            Log.i("3", channelConfiguration+"");
						return sound;
					}
				})
				.map(new Function<Sound, Sound>() {
					@Override
					public Sound apply(Sound arg0) throws Exception {
						int bufferSize = AudioTrack.getMinBufferSize(
								arg0.getmSampleRate(),
								arg0.getmChannels() == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
				                AudioFormat.ENCODING_PCM_16BIT);
						//-確保緩衝區大小可以包含至少1秒的音頻（16位採樣）
						if (bufferSize < arg0.getmChannels() * arg0.getmSampleRate() * 2) {
				            bufferSize = arg0.getmChannels() * arg0.getmSampleRate() * 2;
				        }
						mBuffer = new short[bufferSize/2]; // bufferSize is in Bytes.
						
						mAudioTrack = new AudioTrack(
				                AudioManager.STREAM_MUSIC,
				                arg0.getmSampleRate(),
				                arg0.getmChannels() == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
				                AudioFormat.ENCODING_PCM_16BIT,
				                mBuffer.length * 2,
				                AudioTrack.MODE_STREAM);
						//-檢查播放所有給定的數據，並通知用戶是否設置了聽眾。
						mAudioTrack.setNotificationMarkerPosition(mNumSamples - 1);  // 將標記設置為結束
						mAudioTrack.setPlaybackPositionUpdateListener(
				                new AudioTrack.OnPlaybackPositionUpdateListener() {
				            @Override
				            public void onPeriodicNotification(AudioTrack track) {
				            	LogU.i("1q", "1");
				            }
				            @Override
				            public void onMarkerReached(AudioTrack track) {
				            	LogU.i("播放完成", "播放完成");
				            }
				        });    
						
						return arg0;
					}
				})
				.unsubscribeOn(Schedulers.io())//允許取消訂閱
				.observeOn(AndroidSchedulers.mainThread())//--結果在主線程中顯示
                .subscribeWith(new DisposableSubscriber<Sound>() {
                    @Override  
                    public void onNext(Sound sound) {
                    	soundFilePage.a(mAudioTrack);
                    	soundFilePage.b(sound,mBuffer);
                    	LogU.i("OK", "大OK");
                    }
                    @Override
                    public void onError(Throwable e) {
                    }
                    @Override
                    public void onComplete() {
                    	soundFilePage.mProgressDialog.dismiss();
                    }
                });
				
		disposable.add(d);	
	}
	
	
	//進度偵聽器界面
    public interface ProgressListener {
        /**
         * Will be called by the SoundFile class periodically
         * with values between 0.0 and 1.0.  Return true to continue
         * loading the file or recording the audio, and false to cancel or stop recording.
         */
        boolean reportProgress(double fractionComplete);
    }
    public int getSamplesPerFrame() {
        return 1024;  // just a fixed value here...
    }

	
	
    private long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

}

