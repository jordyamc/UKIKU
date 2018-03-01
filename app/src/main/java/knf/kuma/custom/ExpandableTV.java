package knf.kuma.custom;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageButton;

import at.blogc.android.views.ExpandableTextView;

public class ExpandableTV extends ExpandableTextView {

    private ImageButton indicator;
    private String textSetted;

    public ExpandableTV(Context context) {
        super(context);
    }

    public ExpandableTV(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public ExpandableTV(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setIndicator(ImageButton indicator) {
        this.indicator = indicator;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        textSetted=text.toString();
        super.setText(text, type);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (indicator!=null&&textSetted!=null&&(getLayout().getText().toString()).equalsIgnoreCase(textSetted))
            indicator.post(new Runnable() {
                @Override
                public void run() {
                    indicator.setVisibility(GONE);
                }
            });
    }
}
