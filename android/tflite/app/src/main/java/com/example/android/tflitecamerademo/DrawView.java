/*
 * Copyright 2018 Zihua Zeng (edvard_hua@live.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.tflitecamerademo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.concurrent.CopyOnWriteArrayList;


public class DrawView extends View {

    public static final int COLOR_TOP = Color.parseColor("#980000");
    public static final int COLOR_NECK = Color.parseColor("#ff0000");
    public static final int COLOR_RSHOULDER = Color.parseColor("#ff9900");
    public static final int COLOR_RELBOW = Color.parseColor("#ffff00");
    public static final int COLOR_RWRIST = Color.parseColor("#00ff00");
    public static final int COLOR_LSHOULDER = Color.parseColor("#00ffff");
    public static final int COLOR_LELBOW = Color.parseColor("#4a86e8");
    public static final int COLOR_LWRIST = Color.parseColor("#0000ff");
    public static final int COLOR_RHIP = Color.parseColor("#9900ff");
    public static final int COLOR_RKNEE = Color.parseColor("#274e13");
    public static final int COLOR_RANKLE = Color.parseColor("#e6b8af");
    public static final int COLOR_LHIP = Color.parseColor("#0c343d");
    public static final int COLOR_LKNEE = Color.parseColor("#1c4587");
    public static final int COLOR_LANKLE = Color.parseColor("#073763");
    public static final int COLOR_BACKGROUND = Color.parseColor("#20124d");

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    private final CopyOnWriteArrayList<PointF> mDrawPoint = new CopyOnWriteArrayList<>();
    private int mWidth, mHeight;
    private float mRatioX, mRatioY;
    private int mImgWidth, mImgHeight;

    private final int[] mColorArray = new int[]{
            COLOR_TOP,
            COLOR_NECK,COLOR_NECK,COLOR_NECK,COLOR_NECK,
            COLOR_RWRIST,COLOR_RWRIST,COLOR_RWRIST,COLOR_RWRIST,
            COLOR_LWRIST,COLOR_LWRIST,COLOR_LWRIST,COLOR_LWRIST,
            COLOR_RANKLE,COLOR_RANKLE,COLOR_RANKLE,COLOR_RANKLE,
            COLOR_LANKLE, COLOR_LANKLE, COLOR_LANKLE, COLOR_LANKLE

    };
    private Paint mPaint;

    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setImgSize(int width, int height) {
        mImgWidth = width;
        mImgHeight = height;
        requestLayout();
    }

    /**
     * 输入的为92*92的图,然后按照比例放大
     * 先按ratio放大,再按机器实际尺寸放大
     *
     * @param point 2*14
     */
    public void setDrawPoint(float[][] point, float ratio) {
        mDrawPoint.clear();

        float tempX, tempY;
        for (int i = 0; i < 4; i++) {
            tempX = point[0][i] / mRatioX;
            tempY = point[1][i] / mRatioY;
            mDrawPoint.add(new PointF(tempX, tempY));
        }
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that is,
     * calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAntiAlias(true);
        }

        canvas.drawColor(Color.TRANSPARENT);

        int colorIndex = 0;
        for (PointF pointF : mDrawPoint) {
//            if (pointF.x == 0 && pointF.y == 0) {
//                colorIndex++;
//                continue;
//            }
            mPaint.setColor(mColorArray[colorIndex++]);
            canvas.drawCircle(pointF.x, pointF.y, 8, mPaint);
        }

        mPaint.setColor(Color.parseColor("#6fa8dc"));
        mPaint.setStrokeWidth(5);

        if (mDrawPoint.size() <= 0)
            return;

        PointF p0 = mDrawPoint.get(0);
        PointF p1 = mDrawPoint.get(1);
        PointF p2 = mDrawPoint.get(2);
        PointF p3 = mDrawPoint.get(3);

        //0-1 5 9 13 17
        canvas.drawLine(p0.x, p0.y, p1.x, p1.y, mPaint);
        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, mPaint);
        canvas.drawLine(p2.x, p2.y, p3.x, p3.y, mPaint);
        canvas.drawLine(p3.x, p3.y, p0.x, p0.y, mPaint);




    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                mWidth = width;
                mHeight = width * mRatioHeight / mRatioWidth;
            } else {
                mWidth = height * mRatioWidth / mRatioHeight;
                mHeight = height;
            }
        }

        setMeasuredDimension(mWidth, mHeight);

        mRatioX = ((float) mImgWidth) / mWidth;
        mRatioY = ((float) mImgHeight) / mHeight;
    }
}
