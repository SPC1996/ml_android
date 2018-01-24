package com.keessi.spc.tfclassifier;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.chroma.chroma.Chroma;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AudioRecordManager.OnAudioFrameCaptureListener {
    private static final String TAG = "MainActivity";

    private static final int INPUT_SIZE = 12;
    private static final String INPUT_NAME = "inputs";
    private static final String OUTPUT_NAME = "outputs";
    private static final String MODEL_FILENAME = "chord.pb";
    private static final String LABEL_FILENAME = "chord_label.txt";

    private Classifier classifier;
    private AudioRecordManager audioRecordManager;
    private String targetChord;
    private double threshold;

    private TextView textShow;
    private TextView testRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textShow = findViewById(R.id.textShow);
        textShow.setMovementMethod(ScrollingMovementMethod.getInstance());
        testRes = findViewById(R.id.textRes);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private void initTensorFlowAndLoadModel() {
        try {
            classifier = Classifier.create(
                    getAssets(),
                    MODEL_FILENAME,
                    LABEL_FILENAME,
                    INPUT_SIZE,
                    INPUT_NAME,
                    OUTPUT_NAME
            );
        } catch (IOException e) {
            Log.e(TAG, "fail to init tensorflow and load model");
        }
    }

    public void onClickLoadModel(View view) {
        textShow.setText(R.string.text_hint);
        Log.i(TAG, "load model start");
        textShow.append("加载模型开始\n");
        initTensorFlowAndLoadModel();
        textShow.append("加载模型结束\n");
        Log.i(TAG, "load model end");
    }

    public void onClickStartTest(View view) {
        if (classifier == null) {
            textShow.append("请先加载模型\n");
        } else {
            Log.i(TAG, "test start");
            textShow.append("录音开始\n");
            if (audioRecordManager == null) {
                audioRecordManager = new AudioRecordManager();
            }
            setTargetChord("a");
            setThreshold(0.7);
            audioRecordManager.setOnAudioFrameCaptureListener(this);
            audioRecordManager.startCapture();
        }
    }

    public void onClickStopTest(View view) {
        audioRecordManager.stopCapture();
        textShow.append("录音结束\n");
        Log.i(TAG, "test end");
        classifier = null;
        textShow.append("释放模型\n");
        Log.i(TAG, "release model");

    }

    @Override
    public void onAudioFrameCapture(byte[] audioData) {
        Log.i(TAG, "preprocess data start");
        float[][] preData = preprocessAudioData(audioData);
        Log.i(TAG, "preprocess data end");

        Log.i(TAG, "predict data start");
        List<String> predictResult = predict(preData);
        Log.i(TAG, "predict data end");

        Log.i(TAG, "evaluate data start");
        boolean evaluateResult = evaluate(predictResult);
        Log.i(TAG, "predict data end");

        Log.i(TAG, "show result start");
        showByPredictResult(Boolean.toString(evaluateResult) + "   " + predictResult.toString());
        Log.i(TAG, "show result end");
    }

    private float[][] preprocessAudioData(byte[] audioData) {
        double[] stepOne = new double[audioData.length / 2];
        byte bl, bh;
        short s;
        for (int i = 0; i < stepOne.length; i++) {
            bl = audioData[2 * i];
            bh = audioData[2 * i + 1];
            s = (short) ((bh & 0x00ff) << 8 | bl & 0x00ff);
            stepOne[i] = s / 32768f;
        }

        Chroma chroma = new Chroma();
        double[][] stepTwo = chroma.signal2Chroma(stepOne);

        float[][] preData = new float[stepTwo.length][12];
        for (int i = 0; i < preData.length; i++) {
            for (int j = 0; j < 12; j++) {
                preData[i][j] = (float) stepTwo[i][j];
            }
        }

        return preData;
    }

    private List<String> predict(float[][] preData) {
        return classifier.predictSome(preData);
    }

    private boolean evaluate(List<String> predictData) {
        return classifier.evaluate(predictData, targetChord, threshold);
    }

    private void showByPredictResult(final String predictResult) {
        Log.i(TAG, predictResult);
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testRes.setText(predictResult);
            }
        });
    }

    public void setTargetChord(String targetChord) {
        this.targetChord = targetChord;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}
