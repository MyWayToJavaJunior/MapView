package io.github.kirillf.mapview.widgets;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.FloatMath;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class Scroller  {
    private int mMode;

    private int mStartX;
    private int mStartY;
    private int mFinalX;
    private int mFinalY;

    private int mMinX;
    private int mMaxX;
    private int mMinY;
    private int mMaxY;

    private int mCurrX;
    private int mCurrY;
    private long mStartTime;
    private int mDuration;
    private boolean mFinished;

    private float mVelocity;

    private static final int SCROLL_MODE = 0;
    private static final int FLING_MODE = 1;

    private static float DECELERATION_RATE = (float) (Math.log(0.75) / Math.log(0.9));
    private static float ALPHA = 800;
    private static float START_TENSION = 0.4f;
    private static float END_TENSION = 1.0f - START_TENSION;
    private static final int NB_SAMPLES = 100;
    private static final float[] SPLINE = new float[NB_SAMPLES + 1];

    private float mDeceleration;
    private final float mPpi;

    static {
        float x_min = 0.0f;
        for (int i = 0; i <= NB_SAMPLES; i++) {
            final float t = (float) i / NB_SAMPLES;
            float x_max = 1.0f;
            float x, tx, coef;
            while (true) {
                x = x_min + (x_max - x_min) / 2.0f;
                coef = 3.0f * x * (1.0f - x);
                tx = coef * ((1.0f - x) * START_TENSION + x * END_TENSION) + x * x * x;
                if (Math.abs(tx - t) < 1E-5) break;
                if (tx > t) x_max = x;
                else x_min = x;
            }
            final float d = coef + x * x * x;
            SPLINE[i] = d;
        }
        SPLINE[NB_SAMPLES] = 1.0f;
    }


    public Scroller(Context context) {
        mFinished = true;
        mPpi = context.getResources().getDisplayMetrics().density * 160.0f;
        mDeceleration = computeDeceleration(ViewConfiguration.getScrollFriction());
    }

    public final void setFriction(float friction) {
        mDeceleration = computeDeceleration(friction);
    }
    
    private float computeDeceleration(float friction) {
        return SensorManager.GRAVITY_EARTH   // g (m/s^2)
                      * 39.37f               // inch/meter
                      * mPpi                 // pixels per inch
                      * friction;
    }

    public final boolean isFinished() {
        return mFinished;
    }
    
    public final int getCurrX() {
        return mCurrX;
    }
    
    public final int getCurrY() {
        return mCurrY;
    }
    
    public float getCurrVelocity() {
        return mVelocity - mDeceleration * timePassed() / 2000.0f;
    }

    public boolean computeScrollOffset() {
        if (mFinished) {
            return false;
        }

        int timePassed = (int)(AnimationUtils.currentAnimationTimeMillis() - mStartTime);
    
        if (timePassed < mDuration) {
            switch (mMode) {
            case SCROLL_MODE:

                mCurrX = mStartX + Math.round((float) timePassed);
                mCurrY = mStartY + Math.round((float) timePassed);
                break;
            case FLING_MODE:
                final float t = (float) timePassed / mDuration;
                final int index = (int) (NB_SAMPLES * t);
                final float t_inf = (float) index / NB_SAMPLES;
                final float t_sup = (float) (index + 1) / NB_SAMPLES;
                final float d_inf = SPLINE[index];
                final float d_sup = SPLINE[index + 1];
                final float distanceCoef = d_inf + (t - t_inf) / (t_sup - t_inf) * (d_sup - d_inf);
                
                mCurrX = mStartX + Math.round(distanceCoef * (mFinalX - mStartX));
                mCurrX = Math.min(mCurrX, mMaxX);
                mCurrX = Math.max(mCurrX, mMinX);
                
                mCurrY = mStartY + Math.round(distanceCoef * (mFinalY - mStartY));
                mCurrY = Math.min(mCurrY, mMaxY);
                mCurrY = Math.max(mCurrY, mMinY);

                if (mCurrX == mFinalX && mCurrY == mFinalY) {
                    mFinished = true;
                }
                break;
            }
        }
        else {
            mCurrX = mFinalX;
            mCurrY = mFinalY;
            mFinished = true;
        }
        return true;
    }


    public void fling(int startX, int startY, int velocityX, int velocityY,
            int minX, int maxX, int minY, int maxY) {
        if (!mFinished) {
            float oldVel = getCurrVelocity();

            float dx = (float) (mFinalX - mStartX);
            float dy = (float) (mFinalY - mStartY);
            float hyp = FloatMath.sqrt(dx * dx + dy * dy);

            float ndx = dx / hyp;
            float ndy = dy / hyp;

            float oldVelocityX = ndx * oldVel;
            float oldVelocityY = ndy * oldVel;
            if (Math.signum(velocityX) == Math.signum(oldVelocityX) &&
                    Math.signum(velocityY) == Math.signum(oldVelocityY)) {
                velocityX += oldVelocityX;
                velocityY += oldVelocityY;
            }
        }

        mMode = FLING_MODE;
        mFinished = false;

        float velocity = FloatMath.sqrt(velocityX * velocityX + velocityY * velocityY);
     
        mVelocity = velocity;
        final double l = Math.log(START_TENSION * velocity / ALPHA);
        mDuration = (int) (1000.0 * Math.exp(l / (DECELERATION_RATE - 1.0)));
        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mStartX = startX;
        mStartY = startY;

        float coeffX = velocity == 0 ? 1.0f : velocityX / velocity;
        float coeffY = velocity == 0 ? 1.0f : velocityY / velocity;

        int totalDistance =
                (int) (ALPHA * Math.exp(DECELERATION_RATE / (DECELERATION_RATE - 1.0) * l));
        
        mMinX = minX;
        mMaxX = maxX;
        mMinY = minY;
        mMaxY = maxY;

        mFinalX = startX + Math.round(totalDistance * coeffX);
        mFinalX = Math.min(mFinalX, mMaxX);
        mFinalX = Math.max(mFinalX, mMinX);
        
        mFinalY = startY + Math.round(totalDistance * coeffY);
        mFinalY = Math.min(mFinalY, mMaxY);
        mFinalY = Math.max(mFinalY, mMinY);
    }

    public void abortAnimation() {
        mCurrX = mFinalX;
        mCurrY = mFinalY;
        mFinished = true;
    }
    

    public int timePassed() {
        return (int)(AnimationUtils.currentAnimationTimeMillis() - mStartTime);
    }

}
