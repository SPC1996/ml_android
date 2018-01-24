package com.keessi.spc.tfclassifier;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created Time : 2018/1/23
 * Created User : spc
 */

public class AudioRecordManager {
    private static final String TAG = "AudioRecordManager";

    private static final int DEFAULT_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int DEFAULT_DATA_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SAMPLE_PER_FRAME = 28000;

    private AudioRecord audioRecord;
    private OnAudioFrameCaptureListener audioFrameCaptureListener;
    private Thread captureThread;
    private boolean isCaptureStarted = false;
    private volatile boolean isLoopExit = false;

    public interface OnAudioFrameCaptureListener {
        void onAudioFrameCapture(byte[] audioData);
    }

    private class AudioCaptureRunnable implements Runnable {
        @Override
        public void run() {
            while (!isLoopExit) {
                byte[] buffer = new byte[SAMPLE_PER_FRAME];
                int ret = audioRecord.read(buffer, 0, buffer.length);
                if (ret == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.d(TAG, "Error ERROR_INVALID_OPERATION");
                } else if (ret == AudioRecord.ERROR_BAD_VALUE) {
                    Log.d(TAG, "Error ERROR_BAD_VALUE");
                } else {
                    if (audioFrameCaptureListener != null) {
                        audioFrameCaptureListener.onAudioFrameCapture(buffer);
                    }
                }
            }
        }
    }

    public boolean isCaptureStarted() {
        return isCaptureStarted;
    }

    public void setOnAudioFrameCaptureListener(OnAudioFrameCaptureListener audioFrameCaptureListener) {
        this.audioFrameCaptureListener = audioFrameCaptureListener;
    }

    public boolean startCapture() {
        return startCapture(DEFAULT_SOURCE, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_DATA_FORMAT);
    }

    public boolean startCapture(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {
        if (isCaptureStarted) {
            Log.e(TAG, "Capture already started !");
            return false;
        }

        int minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid parameter !");
            return false;
        }

        audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, minBufferSize * 3);
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "AudioRecord initialize failed !");
            return false;
        }

        audioRecord.startRecording();

        isLoopExit = false;

        captureThread = new Thread(new AudioCaptureRunnable());
        captureThread.start();
        isCaptureStarted = true;

        Log.i(TAG, "start audio capture success !");
        return true;
    }

    public void stopCapture() {
        if (!isCaptureStarted) {
            return;
        }

        isLoopExit = true;
        try {
            captureThread.interrupt();
            captureThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
        }
        isCaptureStarted = false;
    }
}
