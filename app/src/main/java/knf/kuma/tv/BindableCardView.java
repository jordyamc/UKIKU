package knf.kuma.tv;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.leanback.widget.BaseCardView;

public abstract class BindableCardView<T> extends BaseCardView {

    public Context context;

    public BindableCardView(Context context) {
        super(context);
        this.context = context;
        initLayout();
    }

    public BindableCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initLayout();
    }

    public BindableCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initLayout();
    }

    private void initLayout() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(getLayoutResource(), this);
    }

    public abstract void bind(T data);

    public abstract ImageView getImageView();

    public abstract @LayoutRes
    int getLayoutResource();
}
