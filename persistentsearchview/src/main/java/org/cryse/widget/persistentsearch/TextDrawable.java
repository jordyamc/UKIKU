package org.cryse.widget.persistentsearch;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public class TextDrawable extends Drawable {

    private final String mText;
    private final Paint mPaint;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;
    private float mTextSize;

    public TextDrawable(Resources resources, String text) {

        this.mText = text;

        mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                24f, resources.getDisplayMetrics());

        this.mPaint = new Paint();
        mPaint.setColor(Color.GRAY);
        mPaint.setTextSize(mTextSize);
        mPaint.setAntiAlias(true);
        mPaint.setFakeBoldText(false);
        mPaint.setShadowLayer(2f, 0, 0, Color.BLACK);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextAlign(Paint.Align.LEFT);

        Rect bounds = new Rect();
        mPaint.getTextBounds(mText, 0, mText.length(), bounds);
        mIntrinsicWidth = bounds.width();
        mIntrinsicHeight = bounds.height();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect r = getBounds();

        int count = canvas.save();
        canvas.translate(r.left, r.top);
        int height = canvas.getHeight() < 0 ? r.height() : canvas.getHeight();
        canvas.drawText(mText, 0, height / 2 - ((mPaint.descent() + mPaint.ascent()) / 2), mPaint);
        canvas.restoreToCount(count);
    }

    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }
    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}