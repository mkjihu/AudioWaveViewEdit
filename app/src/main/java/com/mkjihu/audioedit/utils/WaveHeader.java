package com.mkjihu.audioedit.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WaveHeader
{
    public final char fileID[] = { 'R', 'I', 'F', 'F' };
    
    public int fileLength;
    
    public char wavTag[] = { 'W', 'A', 'V', 'E' };;
    
    public char FmtHdrID[] = { 'f', 'm', 't', ' ' };
    
    public int FmtHdrLeth;
    
    public short FormatTag;
    
    public short Channels;
    
    public int SamplesPerSec;
    
    public int AvgBytesPerSec;
    
    public short BlockAlign;
    
    public short BitsPerSample;
    
    public char DataHdrID[] = { 'd', 'a', 't', 'a' };
    
    public int DataHdrLeth;
    
    private int mNumBytesPerSample;  // 每個樣本的字節數，包括的所有通道
    
    public byte[] getHeader() throws IOException
    {
    	/*
    	byte[] header = new byte[46];
        int offset = 0;
        int size;

        mNumBytesPerSample = 2 * Channels; //假設每個樣本2個字節（1個通道） 
        
        // 設置RIFF塊
        System.arraycopy(new byte[] {'R', 'I', 'F', 'F'}, 0, header, offset, 4);
        
        
        offset += 4;
        size = fileLength+10;//36 + mNumSamples * mNumBytesPerSample;
        header[offset++] = (byte)(size & 0xFF);
        header[offset++] = (byte)((size >> 8) & 0xFF);
        header[offset++] = (byte)((size >> 16) & 0xFF);
        header[offset++] = (byte)((size >> 24) & 0xFF);
        
        //檔案格式
        System.arraycopy(new byte[] {'W', 'A', 'V', 'E'}, 0, header, offset, 4);
        
        offset += 4;
        // 設置fmt塊
        System.arraycopy(new byte[] {'f', 'm', 't', ' '}, 0, header, offset, 4);
        
        offset += 4;
        System.arraycopy(new byte[] {0x10, 0, 0, 0}, 0, header, offset, 4);  // chunk size = 16
       
        offset += 4;
        System.arraycopy(new byte[] {1, 0}, 0, header, offset, 2);  // format = 1 for PCM
       
        //-單聲道
        offset += 2;
        header[offset++] = (byte)(Channels & 0xFF);
        header[offset++] = (byte)((Channels >> 8) & 0xFF);
        
        //-取樣頻率
        header[offset++] = (byte)(SamplesPerSec & 0xFF);
        header[offset++] = (byte)((SamplesPerSec >> 8) & 0xFF);
        header[offset++] = (byte)((SamplesPerSec >> 16) & 0xFF);
        header[offset++] = (byte)((SamplesPerSec >> 24) & 0xFF);
        
        //-位元率
        int byteRate = SamplesPerSec * mNumBytesPerSample;
        header[offset++] = (byte)(byteRate & 0xFF);
        header[offset++] = (byte)((byteRate >> 8) & 0xFF);
        header[offset++] = (byte)((byteRate >> 16) & 0xFF);
        header[offset++] = (byte)((byteRate >> 24) & 0xFF);
        
        //-區塊對齊
        header[offset++] = (byte)(mNumBytesPerSample & 0xFF);
        header[offset++] = (byte)((mNumBytesPerSample >> 8) & 0xFF);
        
        //-位元深度
        System.arraycopy(new byte[] {0x10, 0}, 0, header, offset, 2);
        
        offset += 2;
        System.arraycopy(new byte[] {'d', 'a', 't', 'a'}, 0, header, offset, 4); // 設置數據塊的開始
        
        //-子區塊大小
        offset += 4;
        size = fileLength;
        header[offset++] = (byte)(size & 0xFF);
        header[offset++] = (byte)((size >> 8) & 0xFF);
        header[offset++] = (byte)((size >> 16) & 0xFF);
        header[offset++] = (byte)((size >> 24) & 0xFF);

        return header;
    	 	*/
    	
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WriteChar(bos, fileID);
        WriteInt(bos, fileLength);
        WriteChar(bos, wavTag);
        WriteChar(bos, FmtHdrID);
        WriteInt(bos, FmtHdrLeth);
        WriteShort(bos, FormatTag);
        WriteShort(bos, Channels);
        WriteInt(bos, SamplesPerSec);
        WriteInt(bos, AvgBytesPerSec);
        WriteShort(bos, BlockAlign);
        WriteShort(bos, BitsPerSample);
        WriteChar(bos, DataHdrID);
        WriteInt(bos, DataHdrLeth);
        bos.flush();
        byte[] r = bos.toByteArray();
        bos.close();
        return r;
       
    }
    
    private void WriteShort(ByteArrayOutputStream bos, int s)
            throws IOException
    {
        byte[] mybyte = new byte[2];
        mybyte[1] = (byte) ((s << 16) >> 24);
        mybyte[0] = (byte) ((s << 24) >> 24);
        bos.write(mybyte);
    }
    
    private void WriteInt(ByteArrayOutputStream bos, int n) throws IOException
    {
        byte[] buf = new byte[4];
        buf[3] = (byte) (n >> 24);
        buf[2] = (byte) ((n << 8) >> 24);
        buf[1] = (byte) ((n << 16) >> 24);
        buf[0] = (byte) ((n << 24) >> 24);
        bos.write(buf);
    }
    
    private void WriteChar(ByteArrayOutputStream bos, char[] id)
    {
        for (int i = 0; i < id.length; i++)
        {
            char c = id[i];
            bos.write(c);
        }
    }
}