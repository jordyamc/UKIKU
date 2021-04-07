package org.cryse.widget.persistentsearch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

public class LogoView extends AppCompatTextView {
    private Drawable mLogoDrawable;

    public LogoView(Context context) {
        super(context);
    }

    public LogoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LogoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setLogo(String logoText) {
        setLogo(new TextDrawable(getResources(), logoText));
    }

    public void setLogo(@DrawableRes int drawableRes) {
        setLogo(ResourcesCompat.getDrawable(getResources(), drawableRes, null));
    }

    public void setLogo(Drawable drawable) {
        this.mLogoDrawable = drawable;
        mLogoDrawable.setBounds(0, 0, mLogoDrawable.getIntrinsicWidth(), mLogoDrawable.getIntrinsicHeight());
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!TextUtils.isEmpty(getText()))
            super.onDraw(canvas);
        else {
            if (mLogoDrawable != null) {
                mLogoDrawable.draw(canvas);
            }
        }
    }
}