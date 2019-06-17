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
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

/** Main {@code Activity} class for the Camera app. */
public class CameraActivity extends Activity {

  private static final String TAG = "TfLiteCameraDemo";
  public BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
    @Override
    public void onManagerConnected(int status) {
      switch (status) {
        case LoaderCallbackInterface.SUCCESS:
        {
          Log.i("chen debug info", "OpenCV loaded successfully");
          // Create and set View
          CameraActivity.Object.isOpenCVInit = true;
        } break;
        case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
        { } break;
        case LoaderCallbackInterface.INIT_FAILED:
        {Log.i("chen debug info", "OpenCV loaded INIT_FAILED"); } break;
        case LoaderCallbackInterface.INSTALL_CANCELED:
        { } break;
        case LoaderCallbackInterface.MARKET_ERROR:
        { } break;
        default:
        {
          super.onManagerConnected(status);
        } break;
      }
    }
  };
  /** Call on every application resume **/
  @Override
  protected void onResume()
  {
    Log.i(TAG, "Called onResume");
    super.onResume();
    if (!OpenCVLoader.initDebug()) {
      OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mOpenCVCallBack);
    } else {
      mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Display display = getWindowManager().getDefaultDisplay();
    Point outSize = new Point();
    display.getSize(outSize);//不能省略,必须有
    int screenWidth = outSize.x;//得到屏幕的宽度
    int screenHeight = outSize.y;//得到屏幕的高度

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_camera);
    if (null == savedInstanceState) {
      getFragmentManager()
          .beginTransaction()
          .replace(R.id.container, Camera2BasicFragment.newInstance())
          .commit();
    }
  }

  static class Object{
    public Object(){
        //        System.loadLibrary("opencv_java");
        System.loadLibrary("opencv_java3");
    }

    static Boolean isOpenCVInit = false;
  }
}
