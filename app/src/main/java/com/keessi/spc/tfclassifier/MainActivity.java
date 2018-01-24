package com.keessi.spc.tfclassifier;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.keessi.spc.tfclassifier.audioprocess.AudioDataProcess;
import com.keessi.spc.tfclassifier.classifier.Classifier;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private String targetChord;
    private double threshold;

    private Spinner spinner;
    private TextView textShow;
    private TextView textRes;
    private TextView textAmplitude;
    private TextView textDecibel;
    private TextView textFrequency;

    private Handler handler;
    private Classifier classifier;
    private AudioRecordManager audioRecordManager;
    private AudioDataProcess audioDataProcess;
    private AudioRecordManager.OnAudioFrameCaptureListener audioRecordListener = new AudioRecordManager.OnAudioFrameCaptureListener() {
        @Override
        public void onAudioFrameCapture(byte[] audioData) {
            audioDataProcess.setAudioByteData(audioData);
            int amplitude = audioDataProcess.getAudioAmplitude();
            double decibel = audioDataProcess.getAudioDecibel();
            double frequency = audioDataProcess.getAudioFrequency();

            final String textResStr;
            final String textAmplitudeStr = String.valueOf(amplitude + " AMP");
            final String textDecibelStr = String.valueOf(decibel + " DB");
            final String textFrequencyStr = String.valueOf(frequency + " HZ");
            if (decibel > 50.0) {
                Log.i(TAG, "process data start");
                float[][] preData = audioDataProcess.getAudioChromaFloatData();
                Log.i(TAG, "process data end");

                Log.i(TAG, "predict data start");
                List<String> predictResult = classifier.predictSome(preData);
                Log.i(TAG, "predict data end");

                Log.i(TAG, "evaluate data start");
                boolean evaluateResult = classifier.evaluate(predictResult, targetChord, threshold);
                Log.i(TAG, "predict data end");

                Log.i(TAG, "show result start");
                textResStr = Boolean.toString(evaluateResult) + "\n" + predictResult.toString();
            } else {
                textResStr = "声音太小 ！！！";
                Log.i(TAG, "voice is too low");
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    textRes.setText(textResStr);
                    textAmplitude.setText(textAmplitudeStr);
                    textDecibel.setText(textDecibelStr);
                    textFrequency.setText(textFrequencyStr);
                }
            });
            Log.i(TAG, "show result end");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        spinner = findViewById(R.id.chooseChord);
        String[] chords = {"a", "am", "bm", "c", "d", "dm", "e", "em", "f", "g"};
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, chords);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setTargetChord(adapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setTargetChord("a");
            }
        });

        textShow = findViewById(R.id.textShow);
        textShow.setMovementMethod(ScrollingMovementMethod.getInstance());
        textRes = findViewById(R.id.textRes);
        textAmplitude = findViewById(R.id.textAmplitude);
        textDecibel = findViewById(R.id.textDecibel);
        textFrequency = findViewById(R.id.textFrequency);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    public void onClickLoadModel(View view) {
        textShow.setText(R.string.text_hint);
        textShow.append("加载模型开始\n");
        classifier = Classifier.createDefault(getAssets());
        textShow.append("加载模型结束\n");
    }

    public void onClickStartTest(View view) {
        if (classifier == null) {
            textShow.append("请先加载模型\n");
        } else {
            textShow.append("录音开始\n");
            if (audioRecordManager == null) {
                audioRecordManager = new AudioRecordManager();
            }
            if (audioDataProcess == null) {
                audioDataProcess = new AudioDataProcess();
            }
            setTargetChord(spinner.getSelectedItem().toString());
            setThreshold(0.7);
            audioRecordManager.setOnAudioFrameCaptureListener(audioRecordListener);
            audioRecordManager.startCapture();
        }
    }

    public void onClickStopTest(View view) {
        audioRecordManager.stopCapture();
        textShow.append("录音结束\n");
        classifier = null;
        audioDataProcess = null;
        textShow.append("释放模型\n");
    }

    public void setTargetChord(String targetChord) {
        this.targetChord = targetChord;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}
