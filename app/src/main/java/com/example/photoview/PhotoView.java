package com.example.photoview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;

/**
 * @Author: Jack Ou
 * @CreateDate: 2020/9/21 23:01
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/9/21 23:01
 * @UpdateRemark: 更新说明
 */
public class PhotoView extends View {

    private static final float IMAGE_WIDTH = Utils.dpToPixel(300);
    private Bitmap bitmap;
    private Paint paint;
    private float originOffsetX;
    private float originOffsetY;

    private float smallScale;
    private float largeScale;
    private float currentScale;

    private boolean isLarge = false;

    private float offectX = 0f;
    private float offectY = 0f;

    private ObjectAnimator scaleAnimator;

    private OverScroller overScroller;
    //private Scroller scroller;

    //单指
    private GestureDetector gestureDetector;
    //双指
    private ScaleGestureDetector scaleGestureDetector;

    public PhotoView(Context context) {
        this(context, null);
    }

    public PhotoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        bitmap = Utils.getPhoto(getResources(), (int) IMAGE_WIDTH);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gestureDetector = new GestureDetector(context, new PhotoGestureDetector());
        //关闭长按
        //gestureDetector.setIsLongpressEnabled(false);

        overScroller = new OverScroller(context);
        //scroller = new Scroller(context);

        scaleGestureDetector = new ScaleGestureDetector(context, new PhotoScaleGestureDetector());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scaleFactor = (currentScale - smallScale) / (largeScale - smallScale);
        canvas.translate(offectX * scaleFactor, offectY * scaleFactor);
        canvas.scale(currentScale, currentScale, getWidth() / 2f, getHeight() / 2f);
        canvas.drawBitmap(bitmap, originOffsetX, originOffsetY, paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        originOffsetX = (getWidth() - bitmap.getWidth()) / 2f;
        originOffsetY = (getHeight() - bitmap.getHeight()) / 2f;

        if ((float) bitmap.getWidth() / bitmap.getHeight() > (float) getWidth() / getHeight()) {
            smallScale = (float) getWidth() / bitmap.getWidth();
            largeScale = (float) getHeight() / bitmap.getHeight();
        } else {
            smallScale = (float) getHeight() / bitmap.getHeight();
            largeScale = (float) getWidth() / bitmap.getWidth();
        }
        currentScale = smallScale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //让GestureDetector的onTouchEvent接管事件分发
        //以双指缩放优先
        boolean result = scaleGestureDetector.onTouchEvent(event);
        if (!scaleGestureDetector.isInProgress()) {
            result = gestureDetector.onTouchEvent(event);
        }
        return result;
    }

    //双指缩放
    class PhotoScaleGestureDetector implements ScaleGestureDetector.OnScaleGestureListener {

        private float initScale;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            //解决原始大小不能缩放
            Log.e("JackOu", "onScale ====== current:" + currentScale + " smallScale:" + smallScale + " largeScale:" + largeScale + " isLarge:" + isLarge);
            if (((currentScale == smallScale) && isLarge) || (currentScale > smallScale && !isLarge)) {
                isLarge = !isLarge;
            }
            //detector.getScaleFactor();获得缩放因子
            currentScale = initScale * detector.getScaleFactor();
            if (currentScale <= smallScale) {
                currentScale = smallScale;
            } else if (currentScale >= largeScale) {
                currentScale = largeScale;
            }
            invalidate();
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            initScale = currentScale;
            //消耗事件
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    }

    //一般重新定义个类，想实现多少就实现多少
    class PhotoGestureDetector extends GestureDetector.SimpleOnGestureListener {


        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return super.onSingleTapUp(e);
        }

        //长按
        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }

        //滚动

        /**
         * @param e1        手指按下
         * @param e2        当前的事件
         * @param distanceX 旧位置 - 新位置    很短   向右X相反
         * @param distanceY
         * @return
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (isLarge) {
                offectX -= distanceX;
                offectY -= distanceY;
                //校正x，y  不让移除图片，出现白色
                fixOffect();
                invalidate();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        //当up事件惯性滑动   50dp/s  ----- 8000dp/s

        /**
         * overScroller中overX overY代表惯性滑动过头了，留白多少
         * overScroller有最后两个参数，OverX和OverY
         * Scroller没有这两个参数
         *
         * @param e1        手指按下
         * @param e2        当前的事件
         * @param velocityX 运动速度 单位px
         * @param velocityY 运动速度 单位px
         * @return
         */
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (isLarge) {
                overScroller.fling((int) offectX, (int) offectY, (int) velocityX, (int) velocityY,
                        -(int) (bitmap.getWidth() * largeScale - getWidth()) / 2, (int) (bitmap.getWidth() * largeScale - getWidth()) / 2,
                        -(int) (bitmap.getHeight() * largeScale - getHeight()) / 2, (int) (bitmap.getHeight() * largeScale - getHeight()) / 2,
                        200, 200);
                //scroller.fling();
                //postOnAnimation下一帧动画执行时调用
                //不能用for  因为for比较快
                postOnAnimation(new FlingRunner());
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        //点击效果处理，延时100毫秒触发
        @Override
        public void onShowPress(MotionEvent e) {
            super.onShowPress(e);
        }

        //只有返回true消费此次事件，双击等才可以响应到事件
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        //双击的第二次down触发，双击出发时间 40ms    --间隔300ms以内
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            isLarge = !isLarge;
            if (isLarge) {
                //解决点哪里那个地方方法   直接置0会中间
//                offectX = 0;
//                offectY = 0;
                offectX = (e.getX() - getWidth() / 2f) - (e.getX() - getWidth() / 2f) * largeScale / smallScale;
                offectY = (e.getY() - getHeight() / 2f) - (e.getY() - getHeight() / 2f) * largeScale / smallScale;
                fixOffect();
                //开启属性动画
                getScaleAnimator().start();
            } else {
                //反向属性动画
                getScaleAnimator().reverse();
            }
            return super.onDoubleTap(e);
        }

        //双击的第二次down触发，move，up都触发
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return super.onDoubleTapEvent(e);
        }

        //单机按下抬起都会触发，但是双击不触发
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }
    }

    private ObjectAnimator getScaleAnimator() {
        if (scaleAnimator == null) {
            scaleAnimator = ObjectAnimator.ofFloat(this, "currentScale", 0);
            scaleAnimator.setDuration(2000);
        }
        Log.e("JackOu", "current:" + currentScale + " smallScale:" + smallScale + " largeScale:" + largeScale + " isLarge:" + isLarge);
        if (currentScale > smallScale && !isLarge) {
            scaleAnimator.setFloatValues(smallScale, currentScale);
        } else if (currentScale > smallScale && isLarge) {
            scaleAnimator.setFloatValues(currentScale, largeScale);
        } else {
            scaleAnimator.setFloatValues(smallScale, largeScale);
        }
        return scaleAnimator;
    }

    public float getCurrentScale() {
        return currentScale;
    }

    public void setCurrentScale(float currentScale) {
        this.currentScale = currentScale;
        //每次改变值就重绘一次
        invalidate();
    }

    private void fixOffect() {
        offectX = Math.min(offectX, (bitmap.getWidth() * largeScale - getWidth()) / 2);
        offectX = Math.max(offectX, -(bitmap.getWidth() * largeScale - getWidth()) / 2);
        offectY = Math.min(offectY, (bitmap.getHeight() * largeScale - getHeight()) / 2);
        offectY = Math.max(offectY, -(bitmap.getHeight() * largeScale - getHeight()) / 2);
    }

    class FlingRunner implements Runnable {
        @Override
        public void run() {
            // overScroller.computeScrollOffset() 返回true说明fling正在进行
            //computeScrollOffset()计算最新的偏移
            if (overScroller.computeScrollOffset()) {
                offectX = overScroller.getCurrX();
                offectY = overScroller.getCurrY();
                invalidate();
                postOnAnimation(this);
            }
        }
    }
}