package org.cryse.widget.persistentsearch;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import androidx.appcompat.graphics.drawable.SupportDrawerArrowDrawable;
import android.util.AttributeSet;
import android.util.Property;
import android.widget.ImageView;

public class HomeButton extends ImageView {
    public enum IconState {
        BURGER, ARROW;
        public int toDrawablePosition() {
            switch (this) {
                case BURGER:
                    return 0;
                case ARROW:
                    return 1;
                default:
                    return 0;
            }
        }
    }
    private ArrowDrawablePositionProperty mArrowPositionProperty = new ArrowDrawablePositionProperty();
    private SupportDrawerArrowDrawable mArrowDrawable;

    private IconState mButtonState = IconState.BURGER;
    private long mAnimationDuration = 300l;
    public HomeButton(Context context) {
        super(context);
        init();
    }

    public HomeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HomeButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HomeButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setArrowDrawableColor(int color) {
        ColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        mArrowDrawable.setColorFilter(colorFilter);
    }

    private void init() {
        mArrowDrawable = new SupportDrawerArrowDrawable(getContext());
        ColorFilter colorFilter = new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        mArrowDrawable.setColorFilter(colorFilter);
        this.setImageDrawable(mArrowDrawable);
    }

    public void setState(IconState state) {
        mButtonState = state;
        mArrowDrawable.setPosition(mButtonState.toDrawablePosition());
    }

    public void animateState(IconState state) {
        float to = state.toDrawablePosition();
        float from = mButtonState.toDrawablePosition();
        mButtonState = state;
        if(Float.compare(from, to) == 0) {
            // mArrowDrawable.setPosition(mButtonState.toDrawablePosition());
        } else {
            ObjectAnimator.ofFloat(mArrowDrawable, mArrowPositionProperty, from, to).setDuration(mAnimationDuration).start();
        }
        //mArrowDrawable.setPosition(mButtonState.toDrawablePosition());
    }

    public void setAnimationDuration(long animationDuration) {
        this.mAnimationDuration = animationDuration;
    }

    static class ArrowDrawablePositionProperty extends Property<SupportDrawerArrowDrawable, Float> {

        public ArrowDrawablePositionProperty() {
            super(Float.TYPE, "position");
        }

        @Override
        public void set(SupportDrawerArrowDrawable object, Float value) {
            object.setPosition(value);
        }

        @Override
        public Float get(SupportDrawerArrowDrawable object) {
            return object.getPosition();
        }
    }
}
