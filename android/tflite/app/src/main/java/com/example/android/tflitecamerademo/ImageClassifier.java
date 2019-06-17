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
  private static final String MODEL_PATH = "model-124500.lite";

  /** Name of the label file stored in Assets. */
  private static final String LABEL_PATH = "labels.txt";

  /** Number of results to show in the UI. */
  private static final int RESULTS_TO_SHOW = 3;

  /** Dimensions of inputs. */
  private static final int DIM_BATCH_SIZE = 2;

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


  /** An array to hold inference results, to be feed into Tensorflow Lite as outputs. */
  private float[][] labelProbArray = null;
  /** multi-stage low pass filter **/
  private float[][] filterLabelProbArray = null;
  private float[][][][] hotmaplabelProbArray = null;
  public float[][] mPrintPointArray = null;
  private Mat mMat =  null;//new Mat(out_width, out_height, CvType.CV_32F);;
  private static final int FILTER_STAGES = 3;
  private static final float FILTER_FACTOR = 0.4f;

  /*为区块追踪准备*/
  public boolean is_first = true;


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

    labelProbArray = new float[1][4];
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
     return textToShow;//textToShow;
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
    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    if (imgData == null) {
      return;
    }
    //如果是第一次就用两个相同bitmap代替
      // 否则就把前一半挪到后面，然后用新输入的图片填充前面的
    if (is_first){
        is_first = false;
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
        pixel = 0;
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

    }else{
        //imgData.rewind();
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                float r =imgData.getFloat((pixel*3+0+DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y*3)*4);
                imgData.putFloat((pixel*3+0)*4,r);
                float g =  imgData.getFloat((pixel*3+1+DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y*3)*4);
                imgData.putFloat((pixel*3+1)*4,g);
                float b = imgData.getFloat((pixel*3+2+DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y*3)*4);
                imgData.putFloat((pixel*3+2)*4,b);
                pixel++;
            }
        }

        pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel];
                float r = ((val >> 16) & 0xFF),g = ((val >> 8) & 0xFF),b = ((val) & 0xFF);
                r = r/255-(float)0.5;
                g = g/255-(float)0.5;
                b = b/255-(float)0.5;
                imgData.putFloat((pixel*3+0+DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y*3)*4,r);
                imgData.putFloat((pixel*3+1+DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y*3)*4,g);
                imgData.putFloat((pixel*3+2+DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y*3)*4,b);
                pixel++;
            }
        }

        //将buffer转换成bitmap
        int[] color1 = new int[32*32];
        pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                float r =imgData.getFloat((pixel*3+0)*4);
                r = (r+0.5f)*255;
                float g =  imgData.getFloat((pixel*3+1)*4);
                g = (g+0.5f)*255;
                float b = imgData.getFloat((pixel*3+2)*4);
                b = (b+0.5f)*255;
                color1[pixel] = ((int)r << 16) | ((int)g << 8) | (int)b | 0xFF000000;
                pixel++;


            }
        }
        Bitmap bmp1 = Bitmap.createBitmap(color1, 0, 32, 32, 32,
                Bitmap.Config.ARGB_8888);

        int[] color2 = new int[32*32];
        pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                float r =imgData.getFloat((pixel*3+0+DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y*3)*4);
                r = (r+0.5f)*255;
                float g =  imgData.getFloat((pixel*3+1+DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y*3)*4);
                g = (g+0.5f)*255;
                float b = imgData.getFloat((pixel*3+2+DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y*3)*4);
                b = (b+0.5f)*255;
                color2[pixel] = ((int)r << 16) | ((int)g << 8) | (int)b | 0xFF000000;
                pixel++;


            }
        }
        Bitmap bmp2 = Bitmap.createBitmap(color2, 0, 32, 32, 32,
                Bitmap.Config.ARGB_8888);

        pixel = 0;
        //imageView.setImageBitmap(stitchBmp);
        //imgData.flip();
//        pixel = 0;
//        int[][][][] image_read = new int[2][32][32][3];
//        for(int image_id = 0;image_id<2;image_id++) {
//            for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
//                for (int j = 0; j < DIM_IMG_SIZE_Y; ++j){
//                    for (int pix = 0; pix < 3; ++pix){
//                        float tmp = imgData.getFloat((pixel)*4);
//                        tmp = (tmp+0.5f)*255;
//                        int tmpi = (int)tmp;
//                        image_read[image_id][i][j][pix] = tmpi;
//                        pixel++;
//                    }
//                }
//            }
//        }
    }



    //

    // Convert the image to floating point.

    long endTime = SystemClock.uptimeMillis();
    Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
  }

  private void runInference(){
    labelProbArray = new float[1][4];
    tflite.run(imgData, labelProbArray);
    if (labelProbArray == null){
        Log.d("chen debug info", "labelProbArray == null");
        return;
    }
    if (!CameraActivity.Object.isOpenCVInit){
        Log.d("chen debug info", "!CameraActivity.Object.isOpenCVInit");
        return;
    }
    pre_r = labelProbArray[0][0];
    pre_x = labelProbArray[0][1];
    pre_y = labelProbArray[0][2];
    pre_z = labelProbArray[0][3];

    pre_r = (float)(Math.round(pre_r*1000))/1000;
    pre_x = (float)(Math.round(pre_x*1000))/1000;
    pre_y = (float)(Math.round(pre_y*1000))/1000;
    pre_z = (float)(Math.round(pre_z*1000))/1000;

  }
  private float get( int x, int y, float[] arr){
    if (x < 0 || y < 0 || x >= out_width || y >= out_height)
      return -1;
    else
      return arr[x * out_width + y];
  }
}
