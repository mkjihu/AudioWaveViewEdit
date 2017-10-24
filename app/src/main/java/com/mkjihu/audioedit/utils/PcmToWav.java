package com.mkjihu.audioedit.utils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.common.io.ByteStreams;

import android.util.Log;

/**
 * Created by HXL on 16/8/11.
 * 将pcm文件转化为wav文件
 */
public class PcmToWav {
	
	
    /**
     * 合并多个pcm文件为一个wav文件
     *該方法是修改的 46字頭 wav檔
     **/
    public static boolean mergePCMFilesToWAVFile2(List<String> filePathList,String destinationPath) {
        File[] file = new File[filePathList.size()];
        byte buffer[] = null;

        int TOTAL_SIZE = 0;
        int fileNum = filePathList.size();

        for (int i = 0; i < fileNum; i++) {
            file[i] = new File(filePathList.get(i));
            TOTAL_SIZE += file[i].length();
        }
        int  Channels = 1;//声道数
        
        //先删除目标文件
        File destfile = new File(destinationPath);
        if (destfile.exists())
            destfile.delete();

        //合成所有的pcm文件的数据，写到目标文件
        try {
        	//將樣本寫入文件，一次1024 * 2 * 2  個數。
            buffer = new byte[1024 * Channels * 2]; // 所有文件的長度，總大小  每個樣本都用短號碼編碼
            InputStream inStream = null;
            OutputStream ouStream = null;
            
            TOTAL_SIZE = (TOTAL_SIZE/2)-23;//用此方法的  樣本總數 = 等於所有檔案的文件的長度/2 - 23
            
            ouStream = new FileOutputStream(destinationPath);//new BufferedOutputStream(new FileOutputStream(destinationPath));
            ouStream.write(WAVHeader.getWAVHeader(16000, Channels, TOTAL_SIZE));//寫入wav格式字頭

            /**/
            for (int j = 0; j < fileNum; j++) {
                inStream = new BufferedInputStream(new FileInputStream(file[j]));
                
                //--1
                byte[] bytes = ByteStreams.toByteArray(inStream);
                ouStream.write(bytes);
                inStream.close();
            }
            
            ouStream.close();
        } catch (FileNotFoundException e) {
            Log.i("PcmToWav2", e.getMessage());
            return false;
        } catch (IOException ioe) {
            Log.i("PcmToWav3", ioe.getMessage());
            return false;
        }
        //clearFiles(filePathList);//--清除待合併文件
        Log.i("PcmToWav", "mergePCMFilesToWAVFile  成功!" + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(new Date()));
        return true;
    }
	
	
    /**
     * 合并多个pcm文件为一个wav文件
     *
     * @param filePathList    pcm文件路径集合
     * @param destinationPath 目标wav文件路径
     * @return true|false
     */
    public static boolean mergePCMFilesToWAVFile(List<String> filePathList,String destinationPath) {
        File[] file = new File[filePathList.size()];
        byte buffer[] = null;

        int TOTAL_SIZE = 0;
        int fileNum = filePathList.size();

        for (int i = 0; i < fileNum; i++) {
            file[i] = new File(filePathList.get(i));
            TOTAL_SIZE += file[i].length();
        }

        // 填入参数，比特率等等。
        WaveHeader header = new WaveHeader();
        // 长度字段 = 内容的大小（TOTAL_SIZE) +
        // 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = TOTAL_SIZE + (44 - 8);
        header.FmtHdrLeth = 16;
        header.BitsPerSample = 16;
        header.Channels = 1;
        header.FormatTag = 0x0001;
        header.SamplesPerSec = 16000;
        header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
        header.DataHdrLeth = TOTAL_SIZE;

        byte[] h = null;
        try {
            h = header.getHeader();
        } catch (IOException e1) {
            Log.i("PcmToWav1", e1.getMessage());
            return false;
        }

        /**/
        if (h.length != 44) // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
        {  
        	Log.i("e04GG", "e04GG");
        	return false;
        }
        //先删除目标文件
        File destfile = new File(destinationPath);
        if (destfile.exists())
            destfile.delete();

        //合成所有的pcm文件的数据，写到目标文件
        try {
            buffer = new byte[1024 * 4]; // Length of All Files, Total Size
            InputStream inStream = null;
            OutputStream ouStream = null;

            ouStream = new BufferedOutputStream(new FileOutputStream(destinationPath));
            ouStream.write(h, 0, h.length);
            for (int j = 0; j < fileNum; j++) {
                inStream = new BufferedInputStream(new FileInputStream(file[j]));
                int size = inStream.read(buffer);
                while (size != -1) {
                    ouStream.write(buffer);
                    size = inStream.read(buffer);
                }
                inStream.close();
            }
            ouStream.close();
        } catch (FileNotFoundException e) {
            Log.i("PcmToWav2", e.getMessage());
            return false;
        } catch (IOException ioe) {
            Log.i("PcmToWav3", ioe.getMessage());
            return false;
        }
        //clearFiles(filePathList);//--清除待合併文件
        Log.i("PcmToWav", "mergePCMFilesToWAVFile  success!" + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(new Date()));
        return true;
    }

    /**
     * 将一个pcm文件转化为wav文件
     * @param pcmPath         pcm文件路径
     * @param destinationPath 目标文件路径(wav)
     * @param deletePcmFile   是否删除源文件
     * @return
     */
    public static boolean makePCMFileToWAVFile(String pcmPath, String destinationPath, boolean deletePcmFile) {
        byte buffer[] = null;
        int TOTAL_SIZE = 0;
        File file = new File(pcmPath);
        if (!file.exists()) {
            return false;
        }
        TOTAL_SIZE = (int) file.length();
        // 填入参数，比特率等等。这里用的是16位单声道 8000 hz
        WaveHeader header = new WaveHeader();
        // 长度字段 = 内容的大小（TOTAL_SIZE) +
        // 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = TOTAL_SIZE + (44 - 8);
        header.FmtHdrLeth = 16;
        header.BitsPerSample = 16;
        header.Channels = 1;
        header.FormatTag = 0x0001;
        header.SamplesPerSec = 16000;
        header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
        header.DataHdrLeth = TOTAL_SIZE;
        Log.i("1大小", TOTAL_SIZE+"");
        byte[] h = null;
        try {
            h = header.getHeader();
        } catch (IOException e1) {
            Log.i("PcmToWav", e1.getMessage());
            return false;
        }
        /*
        if (h.length != 44) // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
            return false;
         */
        //先删除目标文件
        File destfile = new File(destinationPath);
        if (destfile.exists())
            destfile.delete();

        //合成所有的pcm文件的数据，写到目标文件
        try {
            buffer = new byte[1024 * 1000]; // 所有文件的長度，總大小
            InputStream inStream = null;
            OutputStream ouStream = null;

            ouStream = new BufferedOutputStream(new FileOutputStream(destinationPath));
            ouStream.write(h, 0, h.length);
            inStream = new BufferedInputStream(new FileInputStream(file));
            int size = inStream.read(buffer);
            //int PCMSize = 0;
            while (size != -1) {
                ouStream.write(buffer, 0, size);
                //PCMSize += size;
                size = inStream.read(buffer);
            }
            //Log.i("2大小", PCMSize+"");
            inStream.close();
            ouStream.close();
        } catch (FileNotFoundException e) {
            Log.i("PcmToWav", e.getMessage());
            return false;
        } catch (IOException ioe) {
            Log.i("PcmToWav", ioe.getMessage());
            return false;
        }
        if (deletePcmFile) {
            file.delete();
        }
        Log.i("PcmToWav", "makePCMFileToWAVFile  success!" + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(new Date()));
        return true;
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
}