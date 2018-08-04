package knf.kuma.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class WrapWebView extends WebView {
    public WrapWebView(Context context) {
        super(context);
    }

    public WrapWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WrapWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WrapWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public WrapWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
    }
}
