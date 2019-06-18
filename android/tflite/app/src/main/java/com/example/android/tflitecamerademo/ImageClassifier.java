/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.example.android.tflitecamerademo;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.opencv.android.LoaderCallbackInterface;
import org.tensorflow.lite.Interpreter;
import java.util.Map;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Tensor;

/** Classifies images with Tensorflow Lite. */
public class ImageClassifier{

  /** Tag for the {@link Log}. */
  private static final String TAG = "TfLiteCameraDemo";

  /** Name of the model file stored in Assets. */
  private static final String MODEL_PATH = "model-4200.lite";

  /** Name of the label file stored in Assets. */
  private static final String LABEL_PATH = "labels.txt";

  /** Number of results to show in the UI. */
  private static final int RESULTS_TO_SHOW = 3;

  /** Dimensions of inputs. */
  private static final int DIM_BATCH_SIZE = 1;

  private static final int DIM_PIXEL_SIZE = 3;

  static final int DIM_IMG_SIZE_X = 32;
  static final int DIM_IMG_SIZE_Y = 32;

  private static int out_width = 192;
  private static int out_height = 192;

  private static final int IMAGE_MEAN = 128;
  private static final float IMAGE_STD = 128.0f;


  /* Preallocated buffers for storing image data in. */
  private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

  /** An instance of the driver class to run model inference with Tensorflow Lite. */
  private Interpreter tflite;

  /** Labels corresponding to the output of the vision model. */
  private List<String> labelList;

  /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs. */
  private ByteBuffer imgData = null;

  public float pre_r = 0;
  public float pre_x = 0;
  public float pre_y = 0;
  public float pre_z = 0;
  public int centerx = 50;
  public int centery = 100;


  /** An array to hold inference results, to be feed into Tensorflow Lite as outputs. */
  private float[][][][] labelProbArray = null;
  /** multi-stage low pass filter **/
  private float[][] filterLabelProbArray = null;
  private float[][][][] hotmaplabelProbArray = null;
  public float[][] mPrintPointArray = null;
  private Mat mMat =  null;//new Mat(out_width, out_height, CvType.CV_32F);;
  private static final int FILTER_STAGES = 3;
  private static final float FILTER_FACTOR = 0.4f;

  private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
      new PriorityQueue<>(
          RESULTS_TO_SHOW,
          new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
              return (o1.getValue()).compareTo(o2.getValue());
            }
          });
    public Map<Integer, Object> allocateOutputBuffers(int[] shapes){
        int o_size = shapes.length;
        Map<Integer, Object> outputs = new HashMap<>();
        for (int i=0; i < o_size; i++) {
            ByteBuffer o_bytes = ByteBuffer.allocate(shapes[i]);
            outputs.put(i, o_bytes);
        }
        return outputs;
    }
    private Object[] allocateInputBuffers(int[] shapes){
        int i_size = shapes.length;
        Object inputs[] = new Object[i_size];
        for (int i=0; i < i_size; i++) {
            ByteBuffer i_bytes = ByteBuffer.allocate(shapes[i]);
            inputs[i] = i_bytes;
        }
        return inputs;
    }
  /** Initializes an {@code ImageClassifier}. */
  ImageClassifier(Activity activity) throws IOException {
    tflite = new Interpreter(loadModelFile(activity));
    labelList = loadLabelList(activity);
    imgData =
        ByteBuffer.allocateDirect(
            4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
    imgData.order(ByteOrder.nativeOrder());

    labelProbArray = new float[1][32][32][2];
    hotmaplabelProbArray = new float[1][out_width][out_height][21];
    filterLabelProbArray = new float[FILTER_STAGES][labelList.size()];
    Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
  }

  /** Classifies a frame from the preview stream. */
  String classifyFrame(Bitmap bitmap) {
    Log.d("chen debug", "into classifyFrame: " );
    if (tflite == null) {
      Log.e(TAG, "Image classifier has not been initialized; Skipped.");
      return "Image classifier has not been initialized; Skipped.";
    }
    convertBitmapToByteBuffer(bitmap);
    // Here's where the magic happens!!!
    long startTime = SystemClock.uptimeMillis();
    runInference();
    long endTime = SystemClock.uptimeMillis();
    Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));
    //String textToShow = Long.toString(endTime - startTime) + "ms" ;
    String textToShow ="pre_r:"+Float.toString(pre_r)+"\npre_x:"+Float.toString(pre_x)+"\npre_y:"+Float.toString(pre_y)+"\npre_z:"+Float.toString(pre_z);
    return textToShow;
  }

  /** Closes tflite to release resources. */
  public void close() {
    tflite.close();
    tflite = null;
  }

  /** Reads label list from Assets. */
  private List<String> loadLabelList(Activity activity) throws IOException {
    List<String> labelList = new ArrayList<String>();
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
    String line;
    while ((line = reader.readLine()) != null) {
      labelList.add(line);
    }
    reader.close();
    return labelList;
  }

  /** Memory-map the model file in Assets. */
  private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
    AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
    FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
    FileChannel fileChannel = inputStream.getChannel();
    long startOffset = fileDescriptor.getStartOffset();
    long declaredLength = fileDescriptor.getDeclaredLength();
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
  }

  /** Writes Image data into a {@code ByteBuffer}. */
  private void convertBitmapToByteBuffer(Bitmap bitmap) {
    long startTime = SystemClock.uptimeMillis();

    if (imgData == null) {
      return;
    }
    imgData.rewind();
    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    int pixel = 0;
    for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
        for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
            final int val = intValues[pixel++];
            float r = ((val >> 16) & 0xFF),g = ((val >> 8) & 0xFF),b = ((val) & 0xFF);
            r = r/255-(float)0.5;
            g = g/255-(float)0.5;
            b = b/255-(float)0.5;
            imgData.putFloat(r);
            imgData.putFloat(g);
            imgData.putFloat(b);
        }
    }

    //

    // Convert the image to floating point.

    long endTime = SystemClock.uptimeMillis();
    Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
  }

  private void runInference(){
    tflite.run(imgData, labelProbArray);
    if (labelProbArray == null){
        Log.d("chen debug info", "labelProbArray == null");
        return;
    }
    if (!CameraActivity.Object.isOpenCVInit){
        Log.d("chen debug info", "!CameraActivity.Object.isOpenCVInit");
        return;
    }
    float max_x = 0;
    float max_y = 0;
    float sum_1 = 0;
    float sum_2 = 0;
    float max_value = labelProbArray[0][0][0][0]-labelProbArray[0][0][0][1];
    for(int hang = 0;hang<32;hang++){
        for(int lie = 0;lie<32;lie++){
            sum_1+=labelProbArray[0][hang][lie][0];
            sum_2+=labelProbArray[0][hang][lie][1];
            float labelProbArray_tmp = labelProbArray[0][hang][lie][0]-labelProbArray[0][hang][lie][1];
            if(max_value<labelProbArray_tmp){
                max_value = labelProbArray_tmp;
                max_x = hang;
                max_y = lie;
            }
        }
    }
    Log.d("chen debug info", "sum_1: "+Float.toString(sum_1)+"sum_2: "+Float.toString(sum_2));
    int right_move = (int)(max_x - 15.5);
    int down_move = (int)(max_y - 15.5);
    centery = centery+down_move;
    centerx = centerx+right_move;
    if(centery<16|centery>320-16){
        centery = 160;
    }
    if(centerx<16|centerx>320-16){
        centerx = 160;
    }
    max_value = 0.0f;
    //根据原始的坐标截图，根据改变后的坐标移动
    mPrintPointArray = new float[2][4];
    mPrintPointArray[0][0] = centerx-16;
    mPrintPointArray[1][0] = centery-16;
    mPrintPointArray[0][1] = centerx-16;
    mPrintPointArray[1][1] = centery+16;
    mPrintPointArray[0][2] = centerx+16;
    mPrintPointArray[1][2] = centery+16;
    mPrintPointArray[0][3] = centerx+16;
    mPrintPointArray[1][3] = centery-16;

  }
  private float get( int x, int y, float[] arr){
    if (x < 0 || y < 0 || x >= out_width || y >= out_height)
      return -1;
    else
      return arr[x * out_width + y];
  }
}
