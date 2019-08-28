package com.example.administrator.driftbottle.utils;

public class WavHeader {
    /**
     * RIFF数据块
     */
    final String riffChunkId = "RIFF";
    int riffChunkSize;
    final String riffType = "WAVE";

    /**
     * FORMAT 数据块
     */
    final String formatChunkId = "fmt ";
    final int formatChunkSize = 16;
    final short audioFormat = 1;
    short channels;
    int sampleRate;
    int byteRate;
    short blockAlign;
    short sampleBits;
    /**
     * FORMAT 数据块
     */
    final String dataChunkId = "data";
    int dataChunkSize;

    /**
     * 生成wav格式的Header
     * wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk，
     * FMT Chunk，Fact chunk（可选）,Data chunk
     *
     * @param totalAudioLen 不包括header的音频数据总长度
     * @param sampleRate    采样率,也就是录制时使用的频率
     * @param channels      audioRecord的频道数量
     * @param sampleBits    位宽
     */
    public WavHeader(int totalAudioLen, int sampleRate, int channels, int sampleBits) {
        this.riffChunkSize = totalAudioLen;
        this.channels = (short) channels;
        this.sampleRate = sampleRate;
        this.byteRate = sampleRate * sampleBits / 8 * channels;
        this.blockAlign = (short) (channels * sampleBits / 8);
        this.sampleBits = (short) sampleBits;
        this.dataChunkSize = totalAudioLen - 44;
    }

    /**
     * WAV文件头信息由44个字节组成，所以只需要在PCM文件头部添加44个字节的WAV文件头，就可以生成WAV格式文件。
     * ChunkID:大小为4个字节数据，内容为“RIFF”，表示资源交换文件标识
     * ChunkSize：大小为4个字节数据，内容为一个整数，表示从下个地址开始到文件尾的总字节数
     * Format：大小为4个字节数据，内容为“WAVE”，表示WAV文件标识
     * Subchunkl ID:大小为4个字节数据，内容为“fmt ”，表示波形格式标识（fmt ），最后一位空格。
     * Subchunkl Size：大小为4个字节数据，内容为一个整数，表示PCMWAVEFORMAT的长度。
     * AudioFormat：大小为2个字节数据，内容为一个短整数，表示格式种类（值为1时，表示数据为线性PCM编码）
     * NumChannels：大小为2个字节数据，内容为一个短整数，表示通道数，单声道为1，双声道为2
     * SampleRate:大小为4个字节数据，内容为一个整数，表示采样率，比如44100
     * ByteRate:大小为4个字节数据，内容为一个整数，表示波形数据传输速率（每秒平均字节数），大小为 采样率 * 通道数 * 采样位数
     * BlockAlign：大小为2字节数据，内容为一个短整数，表示DATA数据块长度，大小为 通道数 * 采样位数
     * BitsPerSample：大小为2个字节数据，内容为一个短整数，表示采样位数，即PCM位宽，通常为8位或16bit
     * Subchunk2ID:大小为4个字节数据，内容为“data”，表示数据标记符
     * Subchunk2 Size:大小为4个字节数据，内容为一个整数，表示接下来声音数据的总大小，需要减去头部的44个字节。
     * data：就是其他编码文件内容
     * @return
     */
    public byte[] getHeader() {
        byte[] result;
        result = ByteUtils.merger(ByteUtils.toBytes(riffChunkId), ByteUtils.toBytes(riffChunkSize));
        result = ByteUtils.merger(result, ByteUtils.toBytes(riffType));
        result = ByteUtils.merger(result, ByteUtils.toBytes(formatChunkId));
        result = ByteUtils.merger(result, ByteUtils.toBytes(formatChunkSize));
        result = ByteUtils.merger(result, ByteUtils.toBytes(audioFormat));
        result = ByteUtils.merger(result, ByteUtils.toBytes(channels));
        result = ByteUtils.merger(result, ByteUtils.toBytes(sampleRate));
        result = ByteUtils.merger(result, ByteUtils.toBytes(byteRate));
        result = ByteUtils.merger(result, ByteUtils.toBytes(blockAlign));
        result = ByteUtils.merger(result, ByteUtils.toBytes(sampleBits));
        result = ByteUtils.merger(result, ByteUtils.toBytes(dataChunkId));
        result = ByteUtils.merger(result, ByteUtils.toBytes(dataChunkSize));
        return  result;
    }
}
