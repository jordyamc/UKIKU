package knf.kuma.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import knf.kuma.R

class ExpandableTextView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs), View.OnClickListener {

    private val WHAT = 2
    private val WHAT_ANIMATION_END = 3
    private val WHAT_EXPAND_ONLY = 4
    private var textView: TextView? = null
    private var ivExpandOrShrink: ImageButton? = null
    private var drawableShrink: Drawable? = null
    private var drawableExpand: Drawable? = null
    private var textViewStateColor: Int = 0
    private var isShrink = false
    private var isExpandNeeded = false
    private var isInitTextView = true
    private var expandLines: Int = 0
    private var textLines: Int = 0
    private var textContent: CharSequence? = null
    private val textContentColor: Int = 0
    private var textContentSize: Float = 0.toFloat()
    private var thread: Thread? = null
    private var sleepTime = 22
    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {

        override fun handleMessage(msg: Message) {
            when {
                WHAT == msg.what -> {
                    textView?.maxLines = msg.arg1
                    textView?.invalidate()
                }
                WHAT_ANIMATION_END == msg.what -> setExpandState(msg.arg1)
                WHAT_EXPAND_ONLY == msg.what -> changeExpandState(msg.arg1)
            }
            super.handleMessage(msg)
        }

    }

    init {
        initValue(context, attrs)
        initView(context)
        initClick()
    }

    private fun initValue(context: Context, attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs,
                R.styleable.ExpandableTextView)

        expandLines = ta.getInteger(
                R.styleable.ExpandableTextView_tvea_expandLines, 5)

        drawableShrink = ta
                .getDrawable(R.styleable.ExpandableTextView_tvea_shrinkBitmap)
        drawableExpand = ta
                .getDrawable(R.styleable.ExpandableTextView_tvea_expandBitmap)

        textViewStateColor = ta.getColor(R.styleable.ExpandableTextView_tvea_textStateColor, ContextCompat.getColor(context, R.color.colorPrimary))

        textContentSize = ta.getDimension(R.styleable.ExpandableTextView_tvea_textContentSize, 18f)

        ta.recycle()
    }

    private fun initView(context: Context) {

        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater
        inflater?.inflate(R.layout.layout_expandable_textview, this)

        textView = findViewById(R.id.tv_expand_text_view_animation)
        //textView.setTextColor(textContentColor);
        //textView.getPaint().setTextSize(textContentSize);

        textView?.maxLines = expandLines

    }

    fun setExpandIndicatorButton(button: ImageButton) {
        ivExpandOrShrink = button
        ivExpandOrShrink?.setOnClickListener(this)
    }

    private fun initClick() {
        textView?.setOnClickListener(this)
    }

    fun setTextColor(@ColorInt color: Int) {
        textView?.setTextColor(color)
    }

    fun setStateColorFilter(@ColorInt color: Int) {
        if (ivExpandOrShrink != null)
            ivExpandOrShrink?.setColorFilter(color)
    }

    fun setText(charSequence: CharSequence) {
        textContent = charSequence
        textView?.text = charSequence.toString()
        val viewTreeObserver = textView?.viewTreeObserver
        viewTreeObserver?.addOnPreDrawListener {
            if (!isInitTextView) {
                return@addOnPreDrawListener true
            }
            textLines = textView?.lineCount ?: 0
            Log.e("Expandable", "Lines: $textLines")
            isExpandNeeded = textLines > expandLines
            isInitTextView = false
            if (isExpandNeeded) {
                isShrink = true
                doAnimation(textLines, expandLines, WHAT_ANIMATION_END)
            } else {
                isShrink = false
                doNotExpand()
            }
            true
        }
    }

    private fun doAnimation(startIndex: Int, endIndex: Int,
                            what: Int) {

        thread = Thread {

            if (startIndex < endIndex) {
                // 如果起止行数小于结束行数，那么往下展开至结束行数
                // if open index smaller than end index ,do expand action
                var count = startIndex
                while (count++ < endIndex) {
                    val msg = handler.obtainMessage(WHAT, count, 0)

                    try {
                        Thread.sleep(sleepTime.toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    handler.sendMessage(msg)
                }
            } else if (startIndex > endIndex) {
                // 如果起止行数大于结束行数，那么往上折叠至结束行数
                // if open index bigger than end index ,do shrink action
                var count = startIndex
                while (count-- > endIndex) {
                    val msg = handler.obtainMessage(WHAT, count, 0)
                    try {
                        Thread.sleep(sleepTime.toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    handler.sendMessage(msg)
                }
            }

            // 动画结束后发送结束的信号
            // animation end,send signal
            val msg = handler.obtainMessage(what, endIndex, 0)
            handler.sendMessage(msg)

        }

        thread?.start()

    }

    private fun changeExpandState(endIndex: Int) {
        if (endIndex < textLines) {
            if (ivExpandOrShrink != null)
                ivExpandOrShrink?.setImageDrawable(drawableExpand)
        } else {
            if (ivExpandOrShrink != null)
                ivExpandOrShrink?.setImageDrawable(drawableShrink)
        }

    }

    private fun setExpandState(endIndex: Int) {

        if (endIndex < textLines) {
            isShrink = true
            if (ivExpandOrShrink != null)
                ivExpandOrShrink?.setImageDrawable(drawableExpand)
            textView?.setOnClickListener(this)
        } else {
            isShrink = false
            if (ivExpandOrShrink != null)
                ivExpandOrShrink?.setImageDrawable(drawableShrink)
            textView?.setOnClickListener(null)
        }

    }

    private fun doNotExpand() {
        textView?.maxLines = expandLines
        textView?.setOnClickListener(null)
        if (ivExpandOrShrink != null) {
            ivExpandOrShrink?.setOnClickListener(null)
            ivExpandOrShrink?.visibility = View.GONE
        }
    }

    override fun onClick(v: View) {
        clickImageToggle()
    }

    private fun clickImageToggle() {
        if (isShrink) {
            // 如果是已经折叠，那么进行非折叠处理
            // do shrink action
            doAnimation(expandLines, textLines, WHAT_EXPAND_ONLY)
        } else {
            // 如果是非折叠，那么进行折叠处理
            // do expand action
            doAnimation(textLines, expandLines, WHAT_EXPAND_ONLY)
        }

        // 切换状态
        // set flag
        isShrink = !isShrink
    }

    fun getExpandLines(): Int {
        return expandLines
    }

    fun setExpandLines(newExpandLines: Int) {
        val start = if (isShrink) this.expandLines else textLines
        val end = if (textLines < newExpandLines) textLines else newExpandLines
        doAnimation(start, end, WHAT_ANIMATION_END)
        this.expandLines = newExpandLines
    }

}
