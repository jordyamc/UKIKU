package knf.kuma.custom;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class ExpandableTV extends AppCompatTextView {

    private final List<ExpandableTV.OnExpandListener> onExpandListeners;
    private int maxLines = 4;
    private ImageButton indicator;
    private boolean needIndicator = true;
    private TimeInterpolator expandInterpolator;
    private TimeInterpolator collapseInterpolator;
    private long animationDuration;
    private boolean animating;
    private boolean expanded;
    private int collapsedHeight;

    public ExpandableTV(final Context context) {
        this(context, null);
    }

    public ExpandableTV(final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandableTV(final Context context, @Nullable final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        // keep the original value of maxLines
        this.maxLines = this.getMaxLines();

        // create bucket of OnExpandListener instances
        this.onExpandListeners = new ArrayList<>();

        // create default interpolators
        this.expandInterpolator = new AccelerateDecelerateInterpolator();
        this.collapseInterpolator = new AccelerateDecelerateInterpolator();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, int heightMeasureSpec) {
        // if this TextView is collapsed and maxLines = 0,
        // than make its height equals to zero
        if (this.maxLines == 0 && !this.expanded && !this.animating) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    //region public helper methods

    /**
     * Toggle the expanded state of this {@link ExpandableTV}.
     *
     * @return true if toggled, false otherwise.
     */
    public boolean toggle() {
        return this.expanded
                ? this.collapse()
                : this.expand();
    }

    /**
     * Expand this {@link ExpandableTV}.
     *
     * @return true if expanded, false otherwise.
     */
    public boolean expand() {
        if (!this.expanded && !this.animating && this.maxLines >= 0) {
            // notify listener
            this.notifyOnExpand();

            // measure collapsed height
            this.measure
                    (
                            MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    );

            this.collapsedHeight = this.getMeasuredHeight();

            // indicate that we are now animating
            this.animating = true;

            // set maxLines to MAX Integer, so we can calculate the expanded height
            this.setMaxLines(Integer.MAX_VALUE);

            // measure expanded height
            this.measure
                    (
                            MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    );

            final int expandedHeight = this.getMeasuredHeight();

            // animate from collapsed height to expanded height
            final ValueAnimator valueAnimator = ValueAnimator.ofInt(this.collapsedHeight, expandedHeight);
            valueAnimator.addUpdateListener(animation -> ExpandableTV.this.setHeight((int) animation.getAnimatedValue()));

            // wait for the animation to end
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    // reset min & max height (previously set with setHeight() method)
                    ExpandableTV.this.setMaxHeight(Integer.MAX_VALUE);
                    ExpandableTV.this.setMinHeight(0);

                    // if fully expanded, set height to WRAP_CONTENT, because when rotating the device
                    // the height calculated with this ValueAnimator isn't correct anymore
                    final ViewGroup.LayoutParams layoutParams = ExpandableTV.this.getLayoutParams();
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    ExpandableTV.this.setLayoutParams(layoutParams);

                    // keep track of current status
                    ExpandableTV.this.expanded = true;
                    ExpandableTV.this.animating = false;
                }
            });

            // set interpolator
            valueAnimator.setInterpolator(this.expandInterpolator);

            // start the animation
            valueAnimator
                    .setDuration(this.animationDuration)
                    .start();

            return true;
        }

        return false;
    }

    /**
     * Collapse this {@link TextView}.
     *
     * @return true if collapsed, false otherwise.
     */
    public boolean collapse() {
        if (this.expanded && !this.animating && this.maxLines >= 0) {
            // notify listener
            this.notifyOnCollapse();

            // measure expanded height
            final int expandedHeight = this.getMeasuredHeight();

            // indicate that we are now animating
            this.animating = true;

            // animate from expanded height to collapsed height
            final ValueAnimator valueAnimator = ValueAnimator.ofInt(expandedHeight, this.collapsedHeight);
            valueAnimator.addUpdateListener(animation -> ExpandableTV.this.setHeight((int) animation.getAnimatedValue()));

            // wait for the animation to end
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    // keep track of current status
                    ExpandableTV.this.expanded = false;
                    ExpandableTV.this.animating = false;

                    // set maxLines back to original value
                    ExpandableTV.this.setMaxLines(ExpandableTV.this.maxLines);

                    // if fully collapsed, set height back to WRAP_CONTENT, because when rotating the device
                    // the height previously calculated with this ValueAnimator isn't correct anymore
                    final ViewGroup.LayoutParams layoutParams = ExpandableTV.this.getLayoutParams();
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    ExpandableTV.this.setLayoutParams(layoutParams);
                }
            });

            // set interpolator
            valueAnimator.setInterpolator(this.collapseInterpolator);

            // start the animation
            valueAnimator
                    .setDuration(this.animationDuration)
                    .start();

            return true;
        }

        return false;
    }

    //endregion

    //region public getters and setters

    /**
     * Sets the duration of the expand / collapse animation.
     *
     * @param animationDuration duration in milliseconds.
     */
    public void setAnimationDuration(final long animationDuration) {
        this.animationDuration = animationDuration;
    }

    /**
     * Adds a listener which receives updates about this {@link ExpandableTV}.
     *
     * @param onExpandListener the listener.
     */
    public void addOnExpandListener(final ExpandableTV.OnExpandListener onExpandListener) {
        this.onExpandListeners.add(onExpandListener);
    }

    /**
     * Removes a listener which receives updates about this {@link ExpandableTV}.
     *
     * @param onExpandListener the listener.
     */
    public void removeOnExpandListener(final ExpandableTV.OnExpandListener onExpandListener) {
        this.onExpandListeners.remove(onExpandListener);
    }

    /**
     * Sets a {@link TimeInterpolator} for expanding and collapsing.
     *
     * @param interpolator the interpolator
     */
    public void setInterpolator(final TimeInterpolator interpolator) {
        this.expandInterpolator = interpolator;
        this.collapseInterpolator = interpolator;
    }

    /**
     * Returns the current {@link TimeInterpolator} for expanding.
     *
     * @return the current interpolator, null by default.
     */
    public TimeInterpolator getExpandInterpolator() {
        return this.expandInterpolator;
    }

    /**
     * Sets a {@link TimeInterpolator} for expanding.
     *
     * @param expandInterpolator the interpolator
     */
    public void setExpandInterpolator(final TimeInterpolator expandInterpolator) {
        this.expandInterpolator = expandInterpolator;
    }

    /**
     * Returns the current {@link TimeInterpolator} for collapsing.
     *
     * @return the current interpolator, null by default.
     */
    public TimeInterpolator getCollapseInterpolator() {
        return this.collapseInterpolator;
    }

    /**
     * Sets a {@link TimeInterpolator} for collpasing.
     *
     * @param collapseInterpolator the interpolator
     */
    public void setCollapseInterpolator(final TimeInterpolator collapseInterpolator) {
        this.collapseInterpolator = collapseInterpolator;
    }

    /**
     * Is this {@link ExpandableTV} expanded or not?
     *
     * @return true if expanded, false if collapsed.
     */
    public boolean isExpanded() {
        return this.expanded;
    }

    //endregion

    /**
     * This method will notify the listener about this view being expanded.
     */
    private void notifyOnCollapse() {
        for (final ExpandableTV.OnExpandListener onExpandListener : this.onExpandListeners) {
            onExpandListener.onCollapse(this);
        }
    }

    /**
     * This method will notify the listener about this view being collapsed.
     */
    private void notifyOnExpand() {
        for (final ExpandableTV.OnExpandListener onExpandListener : this.onExpandListeners) {
            onExpandListener.onExpand(this);
        }
    }

    //region public interfaces

    public void setIndicator(ImageButton indicator) {
        this.indicator = indicator;
    }

    public void setTextAndIndicator(CharSequence charSequence, ImageButton indicator) {
        this.indicator = indicator;
        setText(charSequence);
        /*getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            needIndicator=getLineCount()>4;
            *//*this.measure
                    (
                            MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    );
            int nH = getMeasuredHeight();*//*
            setMaxLines(4);
            maxLines=4;
            *//*this.measure
                    (
                            MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    );
            int cH = getMeasuredHeight();
            needIndicator = nH>cH;*//*
            if (!needIndicator)
                indicator.post(() -> indicator.setVisibility(GONE));
        });*/
    }

    public void checkIndicator() {
        Rect bounds = new Rect();
        Paint paint = new Paint();
        paint.setTextSize(getTextSize());
        paint.getTextBounds(getText().toString(), 0, getText().length(), bounds);
        int numLines = (int) Math.ceil((float) bounds.width() / getTextSize());
        needIndicator = numLines > 4;
        setMaxLines(4);
        maxLines = 4;
        if (!needIndicator)
            indicator.post(() -> indicator.setVisibility(GONE));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        /*if (!needIndicator)
            indicator.post(() -> indicator.setVisibility(GONE));*/
    }

    /**
     * Interface definition for a callback to be invoked when
     * a {@link ExpandableTV} is expanded or collapsed.
     */
    public interface OnExpandListener {
        /**
         * The {@link ExpandableTV} is being expanded.
         *
         * @param view the textview
         */
        void onExpand(@NonNull ExpandableTV view);

        /**
         * The {@link ExpandableTV} is being collapsed.
         *
         * @param view the textview
         */
        void onCollapse(@NonNull ExpandableTV view);
    }

    /**
     * Simple implementation of the {@link ExpandableTV.OnExpandListener} interface with stub
     * implementations of each method. Extend this if you do not intend to override
     * every method of {@link ExpandableTV.OnExpandListener}.
     */
    public static class SimpleOnExpandListener implements ExpandableTV.OnExpandListener {
        @Override
        public void onExpand(@NonNull final ExpandableTV view) {
            // empty implementation
        }

        @Override
        public void onCollapse(@NonNull final ExpandableTV view) {
            // empty implementation
        }
    }
}
