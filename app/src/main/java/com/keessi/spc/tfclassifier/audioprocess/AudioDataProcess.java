package com.keessi.spc.tfclassifier.audioprocess;

import com.chroma.chroma.Chroma;

/**
 * Created Time : 2018/1/24
 * Created User : spc
 */
public class AudioDataProcess {
    private byte[] audioByteData;
    private short[] audioShortData;
    private double[] audioDoubleData;
    private float[][] audioChromaFloatData;
    private int[] audioAmplitudes;
    private double[] audioDecibels;
    private double audioFrequency;
    private int audioAmplitude;
    private double audioDecibel;

    public AudioDataProcess() {
    }

    public AudioDataProcess(byte[] audioByteData) {
        this.audioByteData = audioByteData;
        audioShortData = null;
        audioDoubleData = null;
        audioChromaFloatData = null;
        audioAmplitudes = null;
        audioDecibels = null;
        audioFrequency = 0.0;
        audioAmplitude = 0;
        audioDecibel = 0.0;
    }

    public void setAudioByteData(byte[] audioByteData) {
        this.audioByteData = audioByteData;
        audioShortData = null;
        audioDoubleData = null;
        audioChromaFloatData = null;
        audioAmplitudes = null;
        audioDecibels = null;
        audioFrequency = 0.0;
        audioAmplitude = 0;
        audioDecibel = 0.0;
    }

    /**
     * 将录制音频获取的字节数组转化为short数组
     *
     * @return short数组，长度为字节数组长度一半
     */
    public short[] getAudioShortData() {
        if (audioShortData != null) {
            return audioShortData;
        } else {
            audioShortData = new short[audioByteData.length / 2];
            byte bl, bh;
            for (int i = 0; i < audioShortData.length; i++) {
                bl = audioByteData[2 * i];
                bh = audioByteData[2 * i + 1];
                audioShortData[i] = (short) ((bh & 0x00ff) << 8 | bl & 0x00ff);
            }
            return audioShortData;
        }
    }

    /**
     * 将录制音频获取的字节数组转化为double数组
     *
     * @return double数组，长度为字节数组长度一半
     */
    public double[] getAudioDoubleData() {
        if (audioDoubleData != null) {
            return audioDoubleData;
        } else {
            audioDoubleData = new double[audioByteData.length / 2];
            byte bl, bh;
            short s;
            for (int i = 0; i < audioDoubleData.length; i++) {
                bl = audioByteData[2 * i];
                bh = audioByteData[2 * i + 1];
                s = (short) ((bh & 0x00ff) << 8 | bl & 0x00ff);
                audioDoubleData[i] = s / 32768f;
            }
            return audioDoubleData;
        }
    }

    /**
     * 将录制音频获取的字节数组转化为表示色度信息的二维float数组
     *
     * @return 二维float数组，维度为n * 12
     */
    public float[][] getAudioChromaFloatData() {
        if (audioChromaFloatData != null) {
            return audioChromaFloatData;
        } else {
            double[] audioDoubleData = getAudioDoubleData();
            Chroma chroma = new Chroma();
            double[][] audioChromaDoubleData = chroma.signal2Chroma(audioDoubleData);
            float[][] audioChromaFloatData = new float[audioChromaDoubleData.length][12];
            for (int i = 0; i < audioChromaFloatData.length; i++) {
                for (int j = 0; j < 12; j++) {
                    audioChromaFloatData[i][j] = (float) audioChromaDoubleData[i][j];
                }
            }
            return audioChromaFloatData;
        }
    }

    /**
     * 根据录制音频的字节数组获取表示振幅信息的int数组
     *
     * @return int数组，长度为字节数组长度一半
     */
    public int[] getAudioAmplitudes() {
        if (audioAmplitudes != null) {
            return audioAmplitudes;
        } else {
            audioAmplitudes = new int[audioByteData.length / 2];
            byte bl, bh;
            short s;
            for (int i = 0; i < audioAmplitudes.length; i++) {
                bl = audioByteData[2 * i];
                bh = audioByteData[2 * i + 1];
                s = (short) ((bh & 0x00ff) << 8 | bl & 0x00ff);
                audioAmplitudes[i] = s;
            }
            return audioAmplitudes;
        }
    }

    /**
     * 根据录制音频的字节数组获取表示音量信息的double数组
     *
     * @return double数组，长度为字节数组长度一半
     */
    public double[] getAudioDecibels() {
        if (audioAmplitudes == null) {
            getAudioAmplitudes();
        }

        if (audioDecibels != null) {
            return audioDecibels;
        } else {
            audioDecibels = new double[audioAmplitudes.length];
            for (int i = 0; i < audioAmplitudes.length; i++) {
                audioDecibels[i] = resizeNumber(getRealDecibel(audioAmplitudes[i]));
            }
            return audioDecibels;
        }
    }

    /**
     * 获取音频振幅的范围
     *
     * @return new int[]{major, minor} major 最大值, minor 最小值
     */
    public int[] getAudioAmplitudeLevels() {
        if (audioAmplitudes == null) {
            getAudioAmplitudes();
        }
        int major = 0;
        int minor = 0;
        for (int i : audioAmplitudes) {
            if (i > major) {
                major = i;
            }
            if (i < minor) {
                minor = i;
            }
        }
        return new int[]{major, minor};
    }

    /**
     * 获取最大的振幅
     *
     * @return 最大的振幅
     */
    public int getAudioAmplitude() {
        if (audioAmplitude != 0) {
            return audioAmplitude;
        } else {
            int[] level = getAudioAmplitudeLevels();
            audioAmplitude = Math.max(level[0], -level[1]);
            return audioAmplitude;
        }
    }

    /**
     * 获取声音分贝大小
     *
     * @return 音量大小，以分贝为单位
     */
    public double getAudioDecibel() {
        if (audioDecibel != 0.0d) {
            return audioDecibel;
        } else {
            if (audioDecibels == null) {
                getAudioDecibels();
            }
            double decibel = 0.0;
            for (double i : audioDecibels) {
                decibel += i;
            }
            audioDecibel = resizeNumber(decibel / audioDecibels.length);
            return audioDecibel;
        }
    }

    /**
     * 获取频率信息
     *
     * @return 频率信息
     */
    public double getAudioFrequency() {
        if (audioFrequency != 0.0d) {
            return audioFrequency;
        } else {
            int length = audioByteData.length / 2;
            int sampleSize = 8192;
            while (sampleSize > length) {
                sampleSize = sampleSize >> 1;
            }
            FrequencyCalculator frequencyCalculator = new FrequencyCalculator(sampleSize);
            frequencyCalculator.feedData(audioByteData, length);
            audioFrequency = resizeNumber(frequencyCalculator.getFreq());
            return audioFrequency;
        }
    }

    private double getRealDecibel(int amplitude) {
        if (amplitude < 0) {
            amplitude *= -1;
        }
        if (amplitude == 0) {
            amplitude = 1;
        }
        return 10 * Math.log10(amplitude * amplitude);
    }

    private double resizeNumber(double value) {
        int temp = (int) (value * 10.0d);
        return temp / 10.0d;
    }
}
