# AudioWaveViewEdit
音檔波形繪製裁剪
------- 

使用多個github項目，實現了以下：

* 讀取手機上有的音檔
* 滑動自訂 裁剪音檔區間 
* 可錄音暫停後繼續錄音
* 錄音同時繪製波型圖

###### 引用項目裡的一些邏輯都修改了，大部分改成以RxJava2為主
###### 使用了MVP架構(沒有很嚴謹....(́=◞౪◟=‵))
###### 錄音繪製修改為適合RxJava2的方式
###### 修改了cokuscz大神的波型圖，讓其可以讀取繪製手機裡其它音檔，以及暫停錄音

![image](https://github.com/mkjihu/AudioWaveViewEdit/blob/master/1.png)
![image](https://github.com/mkjihu/AudioWaveViewEdit/blob/master/2.jpg)
![image](https://github.com/mkjihu/AudioWaveViewEdit/blob/master/3.png)


參考github項目：
    
* [cokuscz/audioWaveCanvas](https://github.com/cokuscz/audioWaveCanvas)      

* [Jay-Goo/RangeSeekBar](https://github.com/Jay-Goo/RangeSeekBar) 

* [google/ringdroid](https://github.com/google/ringdroid) 
