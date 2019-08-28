package com.example.administrator.driftbottle;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.WifiConfiguration;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.VoicemailContract;
import android.util.Base64;
import android.util.Log;
//import android.support.v4.content.ModernAsyncTask;

import com.example.administrator.driftbottle.utils.WavHeader;
import com.zlw.main.recorderlib.RecordManager;
import com.zlw.main.recorderlib.recorder.RecordConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLEngineResult;

public class AudioRecorder {
    private static AudioRecorder audioRecorder;
    // 音频源：音频输入-麦克风
    private  int audioSource = MediaRecorder.AudioSource.MIC;
    // 采样率AudioRecord
    // 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    // 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private int sampleRateInHz = 16000;
    // 音频通道 单声道
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    // 音频格式：PCM编码
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区大小：缓冲区字节大小
    private int bufferSizeInBytes = 0;
    // 录音对象
    private AudioRecord audioRecord;
    // 文件名
    private String fileName = "recording";
    private String recordDir = String.format(Locale.getDefault(), "%s/Record/driftbottle/",
            Environment.getExternalStorageDirectory().getAbsolutePath());
    // 录音文件集合
    private List<String> filesName = new ArrayList<>();
    // 是否在录音
    private Boolean isRecording = false;
    // 录音数据源
    private byte audiodata[];
    // 录音文件存放路径
    private String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/audio";

//    private Handler recordingHandle = new Handler(){
//        @Override
//        public void handleMessage(Message msg){
//        }
//    };
    final RecordManager recordManager = RecordManager.getInstance();

    public void setOption (JSONObject option) {
        try {
            if (option.isNull("fileName")) {
                fileName = option.getString("fileName");
            }
            if (option.isNull("audioSource")) {
                audioSource = option.getInt("audioSource");
            }
            if (option.isNull("sampleRateInHz")) {
                sampleRateInHz = option.getInt("sampleRateInHz");
            }
            if (option.isNull("channelConfig")) {
                channelConfig = option.getInt("channelConfig");
            }
            if (option.isNull("audioFormat")) {
                audioFormat = option.getInt("audioFormat");
            }
        } catch (JSONException e) {
            fileName = "recording";
            audioSource = MediaRecorder.AudioSource.MIC;
            sampleRateInHz = 16000;
            channelConfig = AudioFormat.CHANNEL_IN_MONO;
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        }
    }

    public void start (JSONObject option) {
//        setOption(option); // 设置配置信息
        recordManager.changeFormat(RecordConfig.RecordFormat.WAV);
        recordManager.changeRecordConfig(recordManager.getRecordConfig().setSampleRate(sampleRateInHz));
        recordManager.changeRecordConfig(recordManager.getRecordConfig().setEncodingConfig(AudioFormat.ENCODING_PCM_16BIT));
        log("录音-文件路径", recordDir);
        recordManager.changeRecordDir(recordDir);
        recordManager.start();
    }
    public String end () {
//        recordManager.stop();
        return "";
    }

    // 开始录音
    public Boolean startRecording (JSONObject option, final Handler recordingHandle) {
        Log.d("录音-进入", isRecording.toString());
        if (isRecording) {
            Log.d("录音-开始-退出", isRecording.toString());
            return true;
        } else {
            Log.d("录音-开始-准备", option.toString());
//            setOption(option); // 设置配置信息
            Log.d("录音-配置-信息", "fileName:" + fileName + ", audioSource:" + audioSource + ", sampleRateInHz:" + sampleRateInHz + ", channelConfig:" + channelConfig + ", audioFormat:" + audioFormat);
            bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
            audiodata = new byte[bufferSizeInBytes];
            audioRecord.startRecording();
            isRecording = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FileOutputStream fos = null;
                    int readsize = 0;
                    Message msg = new Message();
                    try {
                        String currentFileName = fileName;
                        File path = new File(dirPath);
                        if (!path.exists()) {
                            path.mkdirs();
                        }
                        File file = File.createTempFile(fileName, ".pcm", path);
                        if (file.exists()) {
                            file.delete();
                        }
                        fos = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        Log.e("录音异常", e.getMessage());
                        msg.what = 501;
                        recordingHandle.sendMessage(msg);
                    } catch (IOException e) {
                        Log.e("录音-创建文件-异常", e.getMessage());
                        msg.what = 502;
                        recordingHandle.sendMessage(msg);
                    }
                    if (null != fos) {
                        while(isRecording) {
                            readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
                            if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
                                try {
                                    fos.write(audiodata);
                                } catch (IOException e) {
                                    Log.e("录音-写入-异常", e.getMessage());
                                    msg.what = 503;
                                    recordingHandle.sendMessage(msg);
                                }
                            }
                        }
                        try {
                            fos.close();
                        } catch (IOException e) {
                            Log.e("录音-文件流关闭-异常", e.getMessage());
                            msg.what = 504;
                            recordingHandle.sendMessage(msg);
                        }
                    }
                }
            }).start();
            Log.d("录音-正式开启", isRecording.toString());
            return true;
        }
    }
    // 结束录音
    public String endRecording () {
        JSONObject backData = new JSONObject();
        try {
            Log.d("录音-结束-开始", isRecording.toString());
            if (isRecording) {
                isRecording = false;
                audioRecord.stop();
                Log.d("录音-停止录音完成", isRecording.toString());
//                File path = new File(dirPath);
//                if (!path.exists()) {
//                    path.mkdirs();
//                }
//                File inFilename = File.createTempFile(fileName, ".pcm", path);
////                File outFilename = File.createTempFile(fileName, ".wav", path);
////                if (outFilename.exists()) {
////                    outFilename.delete();
////                }
//                FileInputStream in = new FileInputStream(inFilename);
////                FileOutputStream out = new FileOutputStream(outFilename);
//                byte[] inbuffer = new byte[audiodata.length];
//                in.read(inbuffer);
//                in.close();

                int channels =  channelConfig == AudioFormat.CHANNEL_IN_MONO ? 1 : 2;
                int sampleBits = audioFormat == AudioFormat.ENCODING_PCM_16BIT ? 16 : 8;
                int sampleRate = sampleRateInHz;
                int totalAudioLen = audiodata.length;
                Log.d("录音-wavHead-配置信息", totalAudioLen +" - " + sampleRate + " - " + channels + " - " + sampleBits);
                WavHeader wh = new WavHeader(totalAudioLen, sampleRate, channels, sampleBits);
                byte[] wavHead = wh.getHeader();
//                long byteRate = 16 * sampleRateInHz * channels / 8;
//                byte[] wavHead = getWavHead(audiodata.length, audiodata.length + 44, sampleRateInHz, channels, byteRate);
                log("录音-结束-audiodata", Base64.encodeToString(audiodata, Base64.DEFAULT));
                byte[] wavData = new byte[audiodata.length+44];
                System.arraycopy(wavHead, 0, wavData, 0, wavHead.length);
               log("录音-结束-wavHead长度", "" + wavHead.length + " - " + Base64.encodeToString(wavHead, Base64.DEFAULT));
                System.arraycopy(audiodata, 0, wavData, wavHead.length, audiodata.length);
                Log.d("录音-结束-长度", "" + wavData.length + " - " + (wavHead.length + audiodata.length));
                backData.put("code", 200);
                backData.put("msg", "录音成功");
                JSONObject data = new JSONObject();
                String wavBase = Base64.encodeToString(wavData, Base64.DEFAULT);
                log("录音-结束-wavBase-" + wavBase.length(), wavBase);
                data.put("wav", wavBase);
//                data.put("pcm1", Base64.encodeToString(inbuffer, Base64.DEFAULT));
//                data.put("pcm", Base64.encodeToString(audiodata, Base64.DEFAULT));
                backData.put("data", data);
                Log.d("录音-结束-成功", backData.toString());
                audioRecord.release();
                audioRecord = null;
                audiodata = null;
            } else {
                backData.put("code", 501);
                backData.put("msg", "未开始录音");
            }
        } catch (JSONException e) {
            Log.d("录音-结束-异常", e.getMessage());
            return "{\"code\": 500, \"msg\": \"录音结束-JSON操作-异常\", \"data\":{\"e\":\""+ e.getMessage() +"\"}}";
        }
//        catch (FileNotFoundException e) {
//            Log.e("录音结束异常", e.getMessage());
//            return "{\"code\": 500, \"msg\": \"录音结束-异常\", \"data\":{\"e\":\""+ e.getMessage() +"\"}}";
//        } catch (IOException e) {
//            Log.e("录音结束-创建文件-异常", e.getMessage());
//            return "{\"code\": 500, \"msg\": \"录音结束-创建文件-异常\", \"data\": {\"e\":\""+ e.getMessage() +"\"}}";
//        }
        String backStr = backData.toString();
        Log.d("录音-返回-数据长度", backStr.length() + "");
        return backStr;
    }
    public void log (String tag, String msg) {
        if (tag == null || tag.length() == 0
                || msg == null || msg.length() == 0)
            return;
        int segmentSize = 3 * 1024;
        long length = msg.length();
        if (length <= segmentSize ) {// 长度小于等于限制直接打印
            Log.e(tag, msg);
        }else {
            while (msg.length() > segmentSize ) {// 循环分段打印日志
                String logContent = msg.substring(0, segmentSize );
                msg = msg.replace(logContent, "");
                Log.e(tag, logContent);
            }
            Log.e(tag, msg);// 打印剩余日志
        }
    }

    /**
     * 获取wav文件头信息
     * @param totalAudioLen 音频总长度
     * @param totalDataLen 数据总长度
     * @param longSampleRate 采样率
     * @param channels 单双声道
     * @param byteRate 字节频率
     * @return
     */
    public byte[] getWavHead (long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) {
        byte[] header = new byte[44];
        // RIFF/WAVE header
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        //WAVE
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // 'fmt ' chunk
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        // 4 bytes: size of 'fmt ' chunk
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // format = 1
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // block align
        header[32] = (byte) (channels * 16 / 8);
        header[33] = 0;
        // bits per sample
        header[34] = 16;
        header[35] = 0;
        //data
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        return  header;
    }
}
