package knf.kuma.custom

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import java.util.*

class ExpandableTV @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : AppCompatTextView(context, attrs, defStyle) {

    private val onExpandListeners: MutableList<ExpandableTV.OnExpandListener>
    private var mMaxLines = 4
    private var indicator: ImageButton? = null
    private var needIndicator = true
    /**
     * Returns the current [TimeInterpolator] for expanding.
     *
     * @return the current interpolator, null by default.
     */
    /**
     * Sets a [TimeInterpolator] for expanding.
     *
     * @param expandInterpolator the interpolator
     */
    private var expandInterpolator: TimeInterpolator? = null
    /**
     * Returns the current [TimeInterpolator] for collapsing.
     *
     * @return the current interpolator, null by default.
     */
    /**
     * Sets a [TimeInterpolator] for collpasing.
     *
     * @param collapseInterpolator the interpolator
     */
    var collapseInterpolator: TimeInterpolator? = null
    private var animationDuration: Long = 0
    private var animating: Boolean = false
    /**
     * Is this [ExpandableTV] expanded or not?
     *
     * @return true if expanded, false if collapsed.
     */
    var isExpanded: Boolean = false
        private set
    private var collapsedHeight: Int = 0

    init {

        // keep the original value of mMaxLines
        this.mMaxLines = this.maxLines

        // create bucket of OnExpandListener instances
        this.onExpandListeners = ArrayList()

        // create default interpolators
        this.expandInterpolator = AccelerateDecelerateInterpolator()
        this.collapseInterpolator = AccelerateDecelerateInterpolator()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var measureSpec = heightMeasureSpec
        // if this TextView is collapsed and mMaxLines = 0,
        // than make its height equals to zero
        if (this.mMaxLines == 0 && !this.isExpanded && !this.animating) {
            measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY)
        }

        super.onMeasure(widthMeasureSpec, measureSpec)
    }

    //region public helper methods

    /**
     * Toggle the expanded state of this [ExpandableTV].
     *
     * @return true if toggled, false otherwise.
     */
    fun toggle(): Boolean {
        return if (this.isExpanded)
            this.collapse()
        else
            this.expand()
    }

    /**
     * Expand this [ExpandableTV].
     *
     * @return true if expanded, false otherwise.
     */
    fun expand(): Boolean {
        if (!this.isExpanded && !this.animating && this.mMaxLines >= 0) {
            // notify listener
            this.notifyOnExpand()

            // measure collapsed height
            this.measure(
                    View.MeasureSpec.makeMeasureSpec(this.measuredWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            this.collapsedHeight = this.measuredHeight

            // indicate that we are now animating
            this.animating = true

            // set mMaxLines to MAX Integer, so we can calculate the expanded height
            this.maxLines = Integer.MAX_VALUE

            // measure expanded height
            this.measure(
                    View.MeasureSpec.makeMeasureSpec(this.measuredWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            val expandedHeight = this.measuredHeight

            // animate from collapsed height to expanded height
            val valueAnimator = ValueAnimator.ofInt(this.collapsedHeight, expandedHeight)
            valueAnimator.addUpdateListener { animation -> this@ExpandableTV.height = animation.animatedValue as Int }

            // wait for the animation to end
            valueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // reset min & max height (previously set with setHeight() method)
                    this@ExpandableTV.maxHeight = Integer.MAX_VALUE
                    this@ExpandableTV.minHeight = 0

                    // if fully expanded, set height to WRAP_CONTENT, because when rotating the device
                    // the height calculated with this ValueAnimator isn't correct anymore
                    val layoutParams = this@ExpandableTV.layoutParams
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    this@ExpandableTV.layoutParams = layoutParams

                    // keep track of current status
                    this@ExpandableTV.isExpanded = true
                    this@ExpandableTV.animating = false
                }
            })

            // set interpolator
            valueAnimator.interpolator = this.expandInterpolator

            // start the animation
            valueAnimator
                    .setDuration(this.animationDuration)
                    .start()

            return true
        }

        return false
    }

    /**
     * Collapse this [TextView].
     *
     * @return true if collapsed, false otherwise.
     */
    fun collapse(): Boolean {
        if (this.isExpanded && !this.animating && this.mMaxLines >= 0) {
            // notify listener
            this.notifyOnCollapse()

            // measure expanded height
            val expandedHeight = this.measuredHeight

            // indicate that we are now animating
            this.animating = true

            // animate from expanded height to collapsed height
            val valueAnimator = ValueAnimator.ofInt(expandedHeight, this.collapsedHeight)
            valueAnimator.addUpdateListener { animation -> this@ExpandableTV.height = animation.animatedValue as Int }

            // wait for the animation to end
            valueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // keep track of current status
                    this@ExpandableTV.isExpanded = false
                    this@ExpandableTV.animating = false

                    // set mMaxLines back to original value
                    this@ExpandableTV.maxLines = this@ExpandableTV.mMaxLines

                    // if fully collapsed, set height back to WRAP_CONTENT, because when rotating the device
                    // the height previously calculated with this ValueAnimator isn't correct anymore
                    val layoutParams = this@ExpandableTV.layoutParams
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    this@ExpandableTV.layoutParams = layoutParams
                }
            })

            // set interpolator
            valueAnimator.interpolator = this.collapseInterpolator

            // start the animation
            valueAnimator
                    .setDuration(this.animationDuration)
                    .start()

            return true
        }

        return false
    }

    //endregion

    //region public getters and setters

    /**
     * Sets the duration of the expand / collapse animation.
     *
     * @param animationDuration duration in milliseconds.
     */
    fun setAnimationDuration(animationDuration: Long) {
        this.animationDuration = animationDuration
    }

    /**
     * Adds a listener which receives updates about this [ExpandableTV].
     *
     * @param onExpandListener the listener.
     */
    fun addOnExpandListener(onExpandListener: ExpandableTV.OnExpandListener) {
        this.onExpandListeners.add(onExpandListener)
    }

    /**
     * Removes a listener which receives updates about this [ExpandableTV].
     *
     * @param onExpandListener the listener.
     */
    fun removeOnExpandListener(onExpandListener: ExpandableTV.OnExpandListener) {
        this.onExpandListeners.remove(onExpandListener)
    }

    /**
     * Sets a [TimeInterpolator] for expanding and collapsing.
     *
     * @param interpolator the interpolator
     */
    fun setInterpolator(interpolator: TimeInterpolator) {
        this.expandInterpolator = interpolator
        this.collapseInterpolator = interpolator
    }

    //endregion

    /**
     * This method will notify the listener about this view being expanded.
     */
    private fun notifyOnCollapse() {
        for (onExpandListener in this.onExpandListeners) {
            onExpandListener.onCollapse(this)
        }
    }

    /**
     * This method will notify the listener about this view being collapsed.
     */
    private fun notifyOnExpand() {
        for (onExpandListener in this.onExpandListeners) {
            onExpandListener.onExpand(this)
        }
    }

    //region public interfaces

    fun setIndicator(indicator: ImageButton) {
        this.indicator = indicator
    }

    fun setTextAndIndicator(charSequence: CharSequence, indicator: ImageButton) {
        this.indicator = indicator
        text = charSequence
        /*getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            needIndicator=getLineCount()>4;
            *//*this.measure
                    (
                            MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    );
            int nH = getMeasuredHeight();*//*
            setMaxLines(4);
            mMaxLines=4;
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

    fun checkIndicator() {
        val bounds = Rect()
        val paint = Paint()
        paint.textSize = textSize
        paint.getTextBounds(text.toString(), 0, text.length, bounds)
        val numLines = Math.ceil((bounds.width().toFloat() / textSize).toDouble()).toInt()
        needIndicator = numLines > 4
        maxLines = 4
        mMaxLines = 4
        if (!needIndicator)
            indicator!!.post { indicator!!.visibility = View.GONE }
    }

    /**
     * Interface definition for a callback to be invoked when
     * a [ExpandableTV] is expanded or collapsed.
     */
    interface OnExpandListener {
        /**
         * The [ExpandableTV] is being expanded.
         *
         * @param view the textview
         */
        fun onExpand(view: ExpandableTV)

        /**
         * The [ExpandableTV] is being collapsed.
         *
         * @param view the textview
         */
        fun onCollapse(view: ExpandableTV)
    }

    /**
     * Simple implementation of the [ExpandableTV.OnExpandListener] interface with stub
     * implementations of each method. Extend this if you do not intend to override
     * every method of [ExpandableTV.OnExpandListener].
     */
    class SimpleOnExpandListener : ExpandableTV.OnExpandListener {
        override fun onExpand(view: ExpandableTV) {
            // empty implementation
        }

        override fun onCollapse(view: ExpandableTV) {
            // empty implementation
        }
    }
}
