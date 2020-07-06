package com.victormeunier.camqr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class DrawView extends View {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint;
    private Paint mRectPaint;
    private ArrayList<RectF> rectanglesList = new ArrayList<>();

    public DrawView(Context c,  AttributeSet attrs) {
        super(c, attrs);

        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mRectPaint = new Paint();
        mRectPaint.setAntiAlias(true);
        mRectPaint.setDither(true);
        mRectPaint.setColor(Color.parseColor("#4dc96e"));
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeJoin(Paint.Join.ROUND);
        mRectPaint.setStrokeCap(Paint.Cap.ROUND);
        mRectPaint.setStrokeWidth(15);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

        if(rectanglesList.size() > 0) {

            int i = 0;

            for (RectF r: rectanglesList) {
                //mRectPaint.setColor(Color.parseColor(String.format("#%06X", (0xFFFFFF & colorsList.get(i)))));
                canvas.drawRect(r, mRectPaint);
                i++;
            }
        }

    }

    public void clearRectangles() {
        rectanglesList.clear();
        invalidate();
    }

    public void setRectangles(RectF r) {
        rectanglesList.add(r);
        invalidate();
    }


    public void clear(){
        mBitmap.eraseColor(Color.TRANSPARENT);
        invalidate();
        System.gc();
    }
}