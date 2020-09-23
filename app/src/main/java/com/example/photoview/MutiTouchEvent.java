package com.example.photoview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * @Author: Jack Ou
 * @CreateDate: 2020/9/22 23:22
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/9/22 23:22
 * @UpdateRemark: 更新说明
 */
public class MutiTouchEvent extends View {

    private static final float IMAGE_WIDTH = Utils.dpToPixel(300);
    private Bitmap bitmap;
    private Paint paint;

    // 手指滑动偏移值
    private float offsetX;
    private float offsetY;

    // 按下时的x,y坐标
    private float downX;
    private float downY;

    // 上一次的偏移值
    private float lastOffsetX;
    private float lastOffsetY;

    // 当前按下的pointId
    private int currentPointId;

    public MutiTouchEvent(Context context) {
        this(context, null);
    }

    public MutiTouchEvent(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MutiTouchEvent(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bitmap = Utils.getPhoto(getResources(), (int) IMAGE_WIDTH);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, offsetX, offsetY, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //event.getAction() 单指操作
        //event.getActionMasked() 才能接收到MotionEvent.ACTION_POINTER_DOWN多指操作事件
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                //获取点击处的XY坐标
                downX = event.getX();
                downY = event.getY();
                lastOffsetX = offsetX;
                lastOffsetY = offsetY;
                currentPointId = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                //event返回index=0的坐标
                //Point（index,id）   index是变化的，id是固定的，某根手指按下了，Id确定了，只要不抬起来，就一直等于1
                //通过当前的id获得Index决定谁控制移动
                int index = event.findPointerIndex(currentPointId);
                offsetX = lastOffsetX + event.getX(index) - downX;
                offsetY = lastOffsetY + event.getY(index) - downY;
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //获取按下的index，准备用该index拿到Id
                int downIndex = event.getActionIndex();
                currentPointId = event.getPointerId(downIndex);

                //校正第二个手指按下的时候的坐标偏移
                downX = event.getX(downIndex);
                downY = event.getY(downIndex);
                lastOffsetX = offsetX;
                lastOffsetY = offsetY;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //获取抬起手指的Id
                int upIndex = event.getActionIndex();
                int pointerId = event.getPointerId(upIndex);

                //更新活跃手指的id,只处理当前可以移动的手指
                if (pointerId == currentPointId) {
                    //如果是最后一个手指抬起了
                    if (upIndex == event.getPointerCount() - 1) {
                        //抬起来了再减一就等于还剩下手指的最后一个index
                        upIndex = event.getPointerCount() -2;
                    } else {
                        //否者让下一个手指接管移动事件，看非最后手指移除之后，那个手指接管滑动
                        upIndex++;
                    }
                    currentPointId = event.getPointerId(upIndex);
                    //处理图片跳跃
                    downX = event.getX(upIndex);
                    downY = event.getY(upIndex);
                    lastOffsetX = offsetX;
                    lastOffsetY = offsetY;
                }
                break;
        }

        //返回true，把事件消耗了
        return true;
    }
}
