package knf.kuma.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import knf.kuma.R;

/**
 * Created by Jordy on 02/10/2017.
 */

@SuppressWarnings("unused")
public class ExpandableTextView extends LinearLayout
        implements
        View.OnClickListener {

    /**
     * handler信号
     * <br>handler signal
     */
    private final int WHAT = 2;
    /**
     * 动画结束信号
     * <br>animation end signal of handler
     */
    private final int WHAT_ANIMATION_END = 3;
    /**
     * 动画结束，只是改变图标，并不隐藏
     * <br>animation end and expand only,but not disappear
     */
    private final int WHAT_EXPAND_ONLY = 4;
    /**
     * TextView
     */
    private TextView textView;
    /**
     * 点击进行折叠/展开的图片
     * <br>shrink/expand icon
     */
    @Nullable
    private ImageButton ivExpandOrShrink;
    /**
     * 提示折叠的图片资源
     * <br>shrink drawable
     */
    private Drawable drawableShrink;
    /**
     * 提示显示全部的图片资源
     * <br>expand drawable
     */
    private Drawable drawableExpand;
    /**
     * 全部/收起文本的字体颜色
     * <br>color of shrink/expand text
     */
    private int textViewStateColor;
    /**
     * 是否折叠显示的标示
     * <br>flag of shrink/expand
     */
    private boolean isShrink = false;
    /**
     * 是否需要折叠的标示
     * <br>flag of expand needed
     */
    private boolean isExpandNeeded = false;
    /**
     * 是否初始化TextView
     * <br>flag of TextView Initialization
     */
    private boolean isInitTextView = true;
    /**
     * 折叠显示的行数
     * <br>number of lines to expand
     */
    private int expandLines;
    /**
     * 文本的行数
     * <br>Original number of lines
     */
    private int textLines;
    /**
     * 显示的文本
     * <br>content text
     */
    private CharSequence textContent;
    /**
     * 显示的文本颜色
     * <br>content color
     */
    private int textContentColor;
    /**
     * 显示的文本字体大小
     * <br>content text size
     */
    private float textContentSize;
    /**
     * 动画线程
     * <br>thread
     */
    private Thread thread;
    /**
     * 动画过度间隔
     * <br>animation interval
     */
    private int sleepTime = 22;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (WHAT == msg.what) {
                textView.setMaxLines(msg.arg1);
                textView.invalidate();
            } else if (WHAT_ANIMATION_END == msg.what) {
                setExpandState(msg.arg1);
            } else if (WHAT_EXPAND_ONLY == msg.what) {
                changeExpandState(msg.arg1);
            }
            super.handleMessage(msg);
        }

    };

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initValue(context, attrs);
        initView(context);
        initClick();
    }

    private void initValue(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs,
                R.styleable.ExpandableTextView);

        expandLines = ta.getInteger(
                R.styleable.ExpandableTextView_tvea_expandLines, 5);

        drawableShrink = ta
                .getDrawable(R.styleable.ExpandableTextView_tvea_shrinkBitmap);
        drawableExpand = ta
                .getDrawable(R.styleable.ExpandableTextView_tvea_expandBitmap);

        textViewStateColor = ta.getColor(R.styleable.ExpandableTextView_tvea_textStateColor, ContextCompat.getColor(context, R.color.colorPrimary));

        textContentSize = ta.getDimension(R.styleable.ExpandableTextView_tvea_textContentSize, 18);

        ta.recycle();
    }

    private void initView(Context context) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.layout_expandable_textview, this);

        textView = findViewById(R.id.tv_expand_text_view_animation);
        //textView.setTextColor(textContentColor);
        //textView.getPaint().setTextSize(textContentSize);

        textView.setMaxLines(expandLines);

    }

    public void setExpandIndicatorButton(ImageButton button){
        ivExpandOrShrink=button;
        ivExpandOrShrink.setOnClickListener(this);
    }

    private void initClick() {
        textView.setOnClickListener(this);
    }

    public void setTextColor(@ColorInt int color) {
        textView.setTextColor(color);
    }

    public void setStateColorFilter(@ColorInt int color) {
        if (ivExpandOrShrink!=null)
            ivExpandOrShrink.setColorFilter(color);
    }

    public void setText(final CharSequence charSequence) {
        textContent = charSequence;
        textView.setText(charSequence.toString());
        ViewTreeObserver viewTreeObserver = textView.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!isInitTextView) {
                    return true;
                }
                textLines = textView.getLineCount();
                Log.e("Expandable","Lines: "+textLines);
                isExpandNeeded = textLines > expandLines;
                isInitTextView = false;
                if (isExpandNeeded) {
                    isShrink = true;
                    doAnimation(textLines, expandLines, WHAT_ANIMATION_END);
                } else {
                    isShrink = false;
                    doNotExpand();
                }
                return true;
            }
        });
    }

    /**
     * @param startIndex 开始动画的起点行数 <br> start index of animation
     * @param endIndex   结束动画的终点行数 <br> end index of animation
     * @param what       动画结束后的handler信号标示 <br> signal of animation end
     */
    private void doAnimation(final int startIndex, final int endIndex,
                             final int what) {

        thread = new Thread(new Runnable() {

            @Override
            public void run() {

                if (startIndex < endIndex) {
                    // 如果起止行数小于结束行数，那么往下展开至结束行数
                    // if start index smaller than end index ,do expand action
                    int count = startIndex;
                    while (count++ < endIndex) {
                        Message msg = handler.obtainMessage(WHAT, count, 0);

                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        handler.sendMessage(msg);
                    }
                } else if (startIndex > endIndex) {
                    // 如果起止行数大于结束行数，那么往上折叠至结束行数
                    // if start index bigger than end index ,do shrink action
                    int count = startIndex;
                    while (count-- > endIndex) {
                        Message msg = handler.obtainMessage(WHAT, count, 0);
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        handler.sendMessage(msg);
                    }
                }

                // 动画结束后发送结束的信号
                // animation end,send signal
                Message msg = handler.obtainMessage(what, endIndex, 0);
                handler.sendMessage(msg);

            }

        });

        thread.start();

    }

    /**
     * 改变折叠状态（仅仅改变折叠与展开状态，不会隐藏折叠/展开图片布局）
     * change shrink/expand state(only change state,but not hide shrink/expand icon)
     *
     * @param endIndex end
     */
    @SuppressWarnings("deprecation")
    private void changeExpandState(int endIndex) {
        if (endIndex < textLines) {
            if (ivExpandOrShrink!=null)
                ivExpandOrShrink.setImageDrawable(drawableExpand);
        } else {
            if (ivExpandOrShrink!=null)
                ivExpandOrShrink.setImageDrawable(drawableShrink);
        }

    }

    /**
     * 设置折叠状态（如果折叠行数设定大于文本行数，那么折叠/展开图片布局将会隐藏,文本将一直处于展开状态）
     * change shrink/expand state(if number of expand lines bigger than original text lines,hide shrink/expand icon,and TextView will always be at expand state)
     *
     * @param endIndex end
     */
    @SuppressWarnings("deprecation")
    private void setExpandState(int endIndex) {

        if (endIndex < textLines) {
            isShrink = true;
            if (ivExpandOrShrink!=null)
                ivExpandOrShrink.setImageDrawable(drawableExpand);
            textView.setOnClickListener(this);
        } else {
            isShrink = false;
            if (ivExpandOrShrink!=null)
                ivExpandOrShrink.setImageDrawable(drawableShrink);
            textView.setOnClickListener(null);
        }

    }

    /**
     * 无需折叠
     * do not expand
     */
    private void doNotExpand() {
        textView.setMaxLines(expandLines);
        textView.setOnClickListener(null);
        if (ivExpandOrShrink!=null){
            ivExpandOrShrink.setOnClickListener(null);
            ivExpandOrShrink.setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View v) {
        clickImageToggle();
    }

    private void clickImageToggle() {
        if (isShrink) {
            // 如果是已经折叠，那么进行非折叠处理
            // do shrink action
            doAnimation(expandLines, textLines, WHAT_EXPAND_ONLY);
        } else {
            // 如果是非折叠，那么进行折叠处理
            // do expand action
            doAnimation(textLines, expandLines, WHAT_EXPAND_ONLY);
        }

        // 切换状态
        // set flag
        isShrink = !isShrink;
    }

    public Drawable getDrawableShrink() {
        return drawableShrink;
    }

    public void setDrawableShrink(Drawable drawableShrink) {
        this.drawableShrink = drawableShrink;
    }

    public Drawable getDrawableExpand() {
        return drawableExpand;
    }

    public void setDrawableExpand(Drawable drawableExpand) {
        this.drawableExpand = drawableExpand;
    }

    public int getExpandLines() {
        return expandLines;
    }

    public void setExpandLines(int newExpandLines) {
        int start = isShrink ? this.expandLines : textLines;
        int end = textLines < newExpandLines ? textLines : newExpandLines;
        doAnimation(start, end, WHAT_ANIMATION_END);
        this.expandLines = newExpandLines;
    }

    /**
     * 取得显示的文本内容
     * get content text
     *
     * @return content text
     */
    public CharSequence getTextContent() {
        return textContent;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

}
