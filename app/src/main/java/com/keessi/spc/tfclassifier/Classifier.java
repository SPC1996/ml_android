package com.keessi.spc.tfclassifier;

import android.content.res.AssetManager;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created Time : 2018/1/22
 * Created User : spc
 */

public class Classifier {
    private static final String TAG = "Classifier";

    private String inputName;
    private String outputName;
    private int inputSize;

    private float[] outputValues;
    private String[] outputNames;

    private List<String> labels = new ArrayList<String>();

    private TensorFlowInferenceInterface inferenceInterface;

    private Classifier() {
    }

    public static Classifier create(
            AssetManager assetManager,
            String modelFileName,
            String labelFileName,
            int inputSize,
            String inputName,
            String outputName
    ) throws IOException {
        Classifier classifier = new Classifier();
        classifier.inputName = inputName;
        classifier.outputName = outputName;

        Log.i(TAG, "Reading labels from: " + labelFileName);
        BufferedReader br;
        br = new BufferedReader(new InputStreamReader(assetManager.open(labelFileName)));
        String line;
        while ((line = br.readLine()) != null) {
            classifier.labels.add(line);
        }
        br.close();

        classifier.inferenceInterface = new TensorFlowInferenceInterface();
        if (classifier.inferenceInterface.initializeTensorFlow(assetManager, modelFileName) != 0) {
            throw new RuntimeException("TF initialization failed");
        }
        int numClasses = (int) classifier.inferenceInterface.graph().operation(outputName).output(0).shape().size(1);
        Log.i(TAG, "Read " + classifier.labels.size() + " labels, output layer size is " + numClasses);

        classifier.inputSize = inputSize;
        classifier.outputNames = new String[]{outputName};
        classifier.outputValues = new float[numClasses];

        return classifier;
    }

    public String predictOne(float[] inputValues) {
        Trace.beginSection("fillNodeFloat");
        inferenceInterface.fillNodeFloat(inputName, new int[]{1, inputSize}, inputValues);
        Trace.endSection();

        Trace.beginSection("runInference");
        inferenceInterface.runInference(outputNames);
        Trace.endSection();

        Trace.beginSection("readNodeFloat");
        inferenceInterface.readNodeFloat(outputName, outputValues);
        Trace.endSection();

        int maxIndex = 0;
        float maxValue = outputValues[0];
        for (int i = 1; i < outputValues.length; i++) {
            if (maxValue < outputValues[i]) {
                maxValue = outputValues[i];
                maxIndex = i;
            }
        }
        return labels.get(maxIndex);
    }

    public List<String> predictSome(float[][] inputValues) {
        List<String> results = new ArrayList<String>();
        for (float[] item : inputValues) {
            results.add(predictOne(item));
        }
        return results;
    }

    public boolean evaluate(List<String> predictions, String target, double threshold) {
        Log.i(TAG, "target value = " + target);
        Log.i(TAG, "predict value list = " + predictions.toString());
        int count = 0;
        for (String prediction : predictions) {
            if (prediction.equals(target)) {
                count++;
            }
        }
        double accuracy = ((double) count) / predictions.size();
        Log.i(TAG, "accuracy is " + accuracy);
        Log.i(TAG, "threshold is " + threshold);
        return accuracy >= threshold;
    }
}




