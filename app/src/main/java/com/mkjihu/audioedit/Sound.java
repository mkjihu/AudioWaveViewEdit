package com.mkjihu.audioedit;

import java.nio.ShortBuffer;

public class Sound {
    public ShortBuffer mSamples;
    public int mSampleRate;
    public int mChannels;
    public int mNumSamples;  // 每個頻道的樣本數
	public ShortBuffer getmSamples() {
		return mSamples;
	}
	public void setmSamples(ShortBuffer mSamples) {
		this.mSamples = mSamples;
	}
	public int getmSampleRate() {
		return mSampleRate;
	}
	public void setmSampleRate(int mSampleRate) {
		this.mSampleRate = mSampleRate;
	}
	public int getmChannels() {
		return mChannels;
	}
	public void setmChannels(int mChannels) {
		this.mChannels = mChannels;
	}
	public int getmNumSamples() {
		return mNumSamples;
	}
	public void setmNumSamples(int mNumSamples) {
		this.mNumSamples = mNumSamples;
	}
    
    
    
    
    
}
