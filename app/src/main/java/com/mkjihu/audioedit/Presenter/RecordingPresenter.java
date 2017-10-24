package com.mkjihu.audioedit.Presenter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscription;

import com.cokus.wavelibrary.view.WaveSurfaceView;
import com.mkjihu.audioedit.MyApplication;
import com.mkjihu.audioedit.RecordingPage;
import com.mkjihu.audioedit.obj.DialogBox;
import com.mkjihu.audioedit.obj.SdCardU;
import com.mkjihu.audioedit.utils.PcmToWav;
import com.mkjihu.audioedit.utils.SoundFile;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

public class RecordingPresenter {
	public CompositeDisposable disposable;
	public RecordingPage recordingPage;


	public AudioRecord audioRecord;
	public static final int FREQUENCY = 16000;//修改後數字越大採集樣本越大，滑桿跑動速度越快// 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
	public static final int CHANNELCONGIFIGURATION = AudioFormat.CHANNEL_IN_MONO;// 设置单声道声道
	public static final int AUDIOENCODING = AudioFormat.ENCODING_PCM_16BIT;// 音频数据格式：每个样本16位
    public final static int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;// 音频获取源
    public int recBufSize;// 录音最小buffer大小
	
   
    public ArrayList<String> filePathList;//暫存檔案路徑列表
	
    
	private FileOutputStream outputStream;
	private int readsize;
	public short[] buffer;
	public ArrayList<Short> buf;
    public boolean isRecording = false;// 录音线程控制标记
    
    private String mFileName = "testpcm";//中斷文件名
    public String fileName="";//最終文件名
    private String savePcmPath ;//保存pcm文件路径
	//private String saveWavPath;//保存wav文件路径
    
    public int Type = 0;
    		
    private ArrayList<byte[]> write_data = new ArrayList<byte[]>();//写入文件数据
    /*繪圖設定區*/
	public ArrayList<Short> inBuf = new ArrayList<Short>();//缓冲区数据
    private int line_off ;//上下边距的距离
    public int rateX = 100;//控制多少帧取一帧
    public int rateY = 1; //  Y轴缩小的比例 默认为1
    public int baseLine = 0;// Y轴基线
    public int marginRight=30;//波形图绘制距离右边的距离
    public int draw_time = 1000 / 200;//两次绘图间隔的时间
    public float divider = 0.2f;//为了节约绘画时间，每0.2个像素画一个数据
    long c_time;
    private Paint circlePaint;
	private Paint center;
	private Paint paintLine;
	private Paint mPaint;
	public WaveSurfaceView waveSfv;
	public RecordingPresenter(RecordingPage recordingPage,WaveSurfaceView waveSfv) {
		this.recordingPage = recordingPage;
		disposable = new CompositeDisposable();
		this.waveSfv = waveSfv;
		filePathList = new ArrayList<>();//初始化
	}
	
	//-輸入儲存文件名
	public void steTitle(String fileName)
	{
		this.fileName = fileName ;
	}
	
	
    /**已停止录音 清空畫面 合并所有挡案*/
    public void Stop(int Type) {
    	this.Type = Type;
    	switch (Type) {
		case 0:
			//暫停
	        isRecording = false;
			audioRecord.stop();
			break;
		case 1:
			//播放中停止
	        isRecording = false;
			audioRecord.stop();
			break;
		case 2:
			//已暫停中停止
			isRecording = false;
			audioRecord.stop();
			if (filePathList.size()>0) {
    			mergePCMFilesToWAVFile();//合并所有挡案
			}
			break;
		}
    }
    
   
    public void destroy() {
    	try {
    		disposable.dispose();
		} catch (Exception e) { }
    }
    
	public void StartReg()
	{
		isRecording = true;
		createAudio();
		
		{
			line_off = ((WaveSurfaceView)waveSfv).getLine_off();
			baseLine = waveSfv.getHeight() / 2;
			//inBuf.clear();// 清除  
		}
		
		buffer = new short[recBufSize];
		audioRecord.startRecording();// 开始录制
		
		
		Disposable d =Flowable.just("1")
					.subscribeOn(Schedulers.io())
					.doOnSubscribe(new Consumer<Subscription>() {
			            @Override
			            public void accept(Subscription arg0) throws Exception {
			            	//显示加载-解析資料中
			            	
			            }
			        })
					.subscribeOn(AndroidSchedulers.mainThread()) // 指定上面的doOnSubscribe跑主线程
					.repeatWhen(new Function<Flowable<Object>, Flowable<Object>>() {
						@Override
						public Flowable<Object> apply(@NonNull Flowable<Object> observable) throws Exception {
							//LogU.i("內輪詢", "0秒後-重查");
							return observable.delay(0, TimeUnit.MILLISECONDS);
						}
					})
					.subscribeOn(Schedulers.io())
					.map(new Function<String, String>() {
						@Override
						public String apply(String arg) throws Exception {
							//Log.i("處理1", arg0+MyApplication.isInMainThread());
							try {
								// 从MIC保存数据到缓冲区  
			                    readsize = audioRecord.read(buffer, 0, recBufSize);
			                    
			                    //--畫畫用
			                    synchronized (inBuf) {
				                    for (int i = 0; i < readsize; i += rateX) {
				                    	inBuf.add(buffer[i]);
				                    }
			                    }
			                    
			                    //將數據從緩衝區寫入文件  存儲語音緩衝區
			                    if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
			                    	synchronized (write_data) {
			                    		byte  bys[] = new byte[readsize*2];
										//因为arm字节序问题，所以需要高低位交换
			                    		for (int i = 0; i < readsize; i++) {
			                    		byte ss[] =	getBytes(buffer[i]);
			                    			bys[i*2] =ss[0];
			                    			bys[i*2+1] = ss[1];
										}
			                    		write_data.add(bys);
			                        }
			        			}
				            } catch (Throwable t) {
				            	throw Exceptions.propagate(new RuntimeException("錄音錯誤1"));
				            }
							
							//計算圖形
							long time = new Date().getTime();
							
				            if(time - c_time >= draw_time){
				            	buf = new ArrayList<Short>();
				    			synchronized (inBuf) {
				    				if (inBuf.size() == 0)  {
				    					throw Exceptions.propagate(new RuntimeException("錄音錯誤2"));
				    				}
				    	            while(inBuf.size() > (waveSfv.getWidth()-marginRight) / divider){
				    	            	inBuf.remove(0);
				    	            }
				    	            buf = (ArrayList<Short>) inBuf.clone();// 保存  
				    			}
				    			c_time = new Date().getTime();
				    			return "1";
				            }
				            
							return "2";
						}
					})
					.observeOn(AndroidSchedulers.mainThread())//--結果在主線程中顯示
					.map(new Function<String, String>() {
						@Override
						public String apply(String ag) throws Exception {
							//Log.i("處理2", arg0+MyApplication.isInMainThread());
							//處理圖形
							if (ag.equals("1")) {
								SimpleDraw(buf, waveSfv.getHeight()/2,isRecording);// 把缓冲区数据画出来
							}
							return "1";
						}
					})
					.observeOn(Schedulers.io())
					.takeUntil(new Predicate<String>() {
						@Override
						public boolean test(@NonNull String s) throws Exception {
							if (isRecording) {
								return false;//-繼續輪巡
							} else {
								return true;
							}
						}
					})
					/** 如果我们在这里返回“false”的话，那这个结果会被过滤掉（filter）
			            * 过滤（Filtering） 表示 onNext() 不会被调用.
			            * 但是 onComplete() 仍然会被传递.
			         */
					.filter(new Predicate<String>() {
						@Override
						public boolean test(@NonNull String s) throws Exception {
							
							if (isRecording) {
								return false;//-不显示
							} else {
								return true;//显示到onNext
							}
						}
					})
					//.observeOn(Schedulers.newThread())//--多線程
					.map(new Function<String, String>() {
						@Override
						public String apply(String arg0) throws Exception {
							
							//所有录音存入list---建立寫入訊號字节的文件 将文件名后面加个数字,防止重名文件内容被覆盖
							savePcmPath = SdCardU.DATA_DIRECTORY + "/" + mFileName + new Date().getTime()+".pcm";
							filePathList.add(savePcmPath);
							
							File file2wav = null;
							file2wav = new File(savePcmPath);
							if (file2wav.exists()) {
								Log.i("GG", "存在就先刪除");
								file2wav.delete();//--存在就先刪除
							}
							try {
								outputStream = new FileOutputStream(file2wav);
							} catch (FileNotFoundException e1) {}
							
							while (write_data.size() > 0) {
			                	byte[] buffer = null;
			                    synchronized (write_data) {
			                    	if(write_data.size() > 0){
			                    		buffer = write_data.get(0);
			                        	write_data.remove(0);
			                    	}
			                    }
			                    try {
			                    	if(buffer != null){
			                    		outputStream.write(buffer);
			                    		outputStream.flush();
			                    	}
			    				} catch (IOException e) {
			    					e.printStackTrace();
			    				}
			                }
							return "1";
						}
					})
					.observeOn(AndroidSchedulers.mainThread())//--結果在主線程中顯示
					.unsubscribeOn(Schedulers.io())//允許取消訂閱  
					.subscribeWith(new DisposableSubscriber<String>() {
			            @Override
			            public void onComplete() {}
			            @Override
			            public void onError(Throwable e) {
			            	Log.i("錯誤", e.getMessage());
			            }
			            @Override
			            public void onNext(String arg0) {
			            	//Log.i("完成", "完成");
			            	if (Type==1 && filePathList.size()>0) {
			        			Log.i("播放中停止", filePathList.size()+"");
			        			mergePCMFilesToWAVFile();//合并所有挡案
							}
			            }
			        });
		
		disposable.add(d);	
	}
	
	
	
	public byte[] getBytes(short s)
	{
        byte[] buf = new byte[2];
        for (int i = 0; i < buf.length; i++)
        {
            buf[i] = (byte) (s & 0x00ff);
            s >>= 8;
        }
        return buf;
    }  
	/** 
     * 绘制指定区域 
     *  
     * @param buf 缓冲区 
     * @param baseLine  Y轴基线 
     */  
	public void SimpleDraw(ArrayList<Short> buf, int baseLine,boolean isRecording) {
		if (!isRecording)
			return;
		rateY = (65535 /2/ (waveSfv.getHeight()-line_off));

    	for (int i = 0; i < buf.size(); i++) {
    		byte bus[] = getBytes(buf.get(i));
    		buf.set(i, (short)((0x0000 | bus[1]) << 8 | bus[0]));//高低位交换
		}
        Canvas canvas = waveSfv.getHolder().lockCanvas(  
                new Rect(0, 0, waveSfv.getWidth(), waveSfv.getHeight()));// 关键:获取画布  
        if(canvas==null)
        	return;
       // canvas.drawColor(Color.rgb(241, 241, 241));// 清除背景  
        canvas.drawARGB(255, 239, 239, 239);

        int start =(int) ((buf.size())* divider);
        float py = baseLine;
        float y;

        if(waveSfv.getWidth() - start <= marginRight){//如果超过预留的右边距距离
        	start = waveSfv.getWidth() -marginRight;//画的位置x坐标
        }

		canvas.drawCircle(start, line_off/4, line_off/4, circlePaint);// 上圆
        canvas.drawCircle(start, waveSfv.getHeight()-line_off/4, line_off/4, circlePaint);// 下圆
        canvas.drawLine(start, 0, start, waveSfv.getHeight(), circlePaint);//垂直的线
        int height = waveSfv.getHeight()-line_off;

        canvas.drawLine(0, line_off/2, waveSfv.getWidth(), line_off/2, paintLine);//最上面的那根线
		canvas.drawLine(0, height*0.5f+line_off/2, waveSfv.getWidth() ,height*0.5f+line_off/2, center);//中心线
        canvas.drawLine(0, waveSfv.getHeight()-line_off/2-1, waveSfv.getWidth(), waveSfv.getHeight()-line_off/2-1, paintLine);//最下面的那根线
//         canvas.drawLine(0, height*0.25f+20, sfv.getWidth(),height*0.25f+20, paintLine);//第二根线
//         canvas.drawLine(0, height*0.75f+20, sfv.getWidth(),height*0.75f+20, paintLine);//第3根线
        for (int i = 0; i < buf.size(); i++) {
			y =buf.get(i)/rateY + baseLine;// 调节缩小比例，调节基准线
            float x=(i) * divider;
            if(waveSfv.getWidth() - (i-1) * divider <= marginRight){
            	x = waveSfv.getWidth()-marginRight;
            }
			//画线的方式很多，你可以根据自己要求去画。这里只是为了简单
			canvas.drawLine(x, y,  x,waveSfv.getHeight()-y, mPaint);//中间出波形
        }
        waveSfv.getHolder().unlockCanvasAndPost(canvas);// 解锁画布，提交画好的图像  
    }
	private void createAudio() {
		// 获得缓冲区字节大小
		recBufSize = AudioRecord.getMinBufferSize(FREQUENCY,// 44100HZ  采样频率
                CHANNELCONGIFIGURATION, AUDIOENCODING);// 录音组件
		Log.i("緩衝字節大小",recBufSize+"");
		audioRecord = new AudioRecord(AUDIO_SOURCE,// 指定音频来源，这里为麦克风
	                FREQUENCY, // 44100HZ采样频率      16000HZ采样频率
	                CHANNELCONGIFIGURATION,// 录制通道
	                AUDIO_SOURCE,// 录制编码格式
	                recBufSize);// 录制缓冲区大小 //先修改
		SdCardU.createDirectory();
		init();
	}
	
	public WaveSurfaceView waveSfvGt(){
		return recordingPage.waveSfv;
	}
	
	public  void init(){
		circlePaint = new Paint();//画圆
		circlePaint.setColor(Color.rgb(246, 131, 126));//设置上圆的颜色
		center = new Paint();
		center.setColor(Color.rgb(39, 199, 175));// 画笔为color
		center.setStrokeWidth(1);// 设置画笔粗细
		center.setAntiAlias(true);
		center.setFilterBitmap(true);
		center.setStyle(Style.FILL);
		paintLine =new Paint();
		paintLine.setColor(Color.rgb(169, 169, 169));
		mPaint = new Paint();
		mPaint.setColor(Color.rgb(39, 199, 175));// 画笔为color
		mPaint.setStrokeWidth(1);// 设置画笔粗细
		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(true);
		mPaint.setStyle(Style.FILL);
	}
 
	//==============================================================================================
	/**
     * 将pcm合并成wav
     * @param filePaths
     */
    public void mergePCMFilesToWAVFile() {
    	
    	//saveWavPath = SdCardU.DATA_DIRECTORY + "/" + fileName +".wav";//最後合併檔案
    	clidview();
    	Disposable d = Flowable.just(filePathList)
			    		.subscribeOn(Schedulers.io())
			    		.doOnSubscribe(new Consumer<Subscription>() {
			    			@Override
			    			public void accept(Subscription arg0) throws Exception {
			    				//显示加载-解析資料中
			    				recordingPage.showdia();
			    			}
			    		})
			    		.subscribeOn(AndroidSchedulers.mainThread()) // 指定上面的doOnSubscribe跑主线程
			    		.map(new Function<List<String>, String>() {
			
							@Override
							public String apply(List<String> filePaths) throws Exception {
								//fileName = fileName + ".wav";
								String outPath = makeRingtoneFilename(fileName, ".wav");//getWavFileAbsolutePath(fileName);
								
								if (PcmToWav.mergePCMFilesToWAVFile(filePaths,outPath)) {
				                    //操作成功
				                    //wavToM4a();
									afterSavingRingtone(fileName, outPath);
									clearFiles();//--清除待合併文件
				                	return outPath;
				                } else {
				                    Log.i("合併大失敗", "合併大失敗");//操作失败
				                    throw new IllegalStateException("mergePCMFilesToWAVFile fail");
				                }
							}
						})
			    		.observeOn(AndroidSchedulers.mainThread())//--結果在主線程中顯示
			    		.unsubscribeOn(Schedulers.io())//允許取消訂閱 
						.subscribeWith(new DisposableSubscriber<String>() {
			                @Override
			                public void onComplete() {
			                	
			                }
			                @Override
			                public void onError(Throwable e) {
			                	Log.i("錯誤", e.getMessage());
			                	filePathList = new ArrayList<>();//录音合成   清空
			                	recordingPage.disdia();
			                	DialogBox.getAlertDialog1(recordingPage, "", e.getMessage());
			                }
			                @Override
			                public void onNext(final String arg0) {
			                	new Handler().postDelayed(new Runnable() {
									@Override
									public void run() {
										recordingPage.disdia();
										recordingPage.toSidie(arg0);
									}
								}, 1000);
			                }
			            });
    	disposable.add(d);	
    }

    //-創建存放合併後的檔案目錄
    public static String getWavFileAbsolutePath(String fileName) {
        if(fileName==null){
            throw new NullPointerException("fileName can't be null");
        }
        if(!isSdcardExit()){
            throw new IllegalStateException("sd card no found");
        }

        String mAudioWavPath = "";
        if (isSdcardExit()) {
            if (!fileName.endsWith(".wav")) {
                fileName = fileName + ".wav";
            }
            String fileBasePath = SdCardU.DATA_DIRECTORY + "/merge";
            File file = new File(fileBasePath);
            //创建目录
            if (!file.exists()) {
                file.mkdirs();
            }
            mAudioWavPath = fileBasePath +"/"+ fileName;
        }
        return mAudioWavPath;
    }
    
    /**
     * 判断是否有外部存储设备sdcard
     * @return true | false
     */
    public static boolean isSdcardExit() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return true;
        else
            return false;
    }
    public void clearFiles() {
    	clearFiles(filePathList);
    	filePathList = new ArrayList<>();//录音合成   清空
	}
    /**
     * 清除文件
     * @param filePathList
     */
    private static void clearFiles(List<String> filePathList) {
        for (int i = 0; i < filePathList.size(); i++) {
            File file = new File(filePathList.get(i));
            if (file.exists()) {
                file.delete();
            }
        }
    }
    //復原繪圖
    public void clidview()
    {
        line_off = ((WaveSurfaceView)waveSfv).getLine_off();
        baseLine = waveSfv.getHeight() / 2;
        inBuf.clear();// 清除
        buf = (ArrayList<Short>) inBuf.clone();// 图归零  
        SimpleDraw(buf, waveSfv.getHeight()/2 , true);// 

		Log.i("復原繪圖", "復原繪圖");
    }   
    
    
    
    
    
    private String makeRingtoneFilename(CharSequence title, String extension) {
        String subdir;
        String externalRootDir = Environment.getExternalStorageDirectory().getPath();
        if (!externalRootDir.endsWith("/")) {
            externalRootDir += "/";
        }
        subdir =  "/music/";
        
        String parentdir = SdCardU.DATA_DIRECTORY + subdir;

        // Create the parent directory
        File parentDirFile = new File(parentdir);
        parentDirFile.mkdirs();

        // If we can't write to that special path, try just writing
        // directly to the sdcard
        if (!parentDirFile.isDirectory()) {
            parentdir = externalRootDir;
        }

        // Turn the title into a filename
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
	private void afterSavingRingtone(CharSequence title, String outPath){
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
        //values.put(MediaStore.Audio.Media.DURATION, duration);// 沒有時長
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
        recordingPage.getContentResolver().insert(uri, values);
        
	}    
    
    
    
    
    
    
}
