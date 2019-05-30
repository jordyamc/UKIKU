package knf.kuma.custom

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebView

class WrapWebView : WebView {
    constructor(context: Context) : super(context.configurated)

    constructor(context: Context, attrs: AttributeSet) : super(context.configurated, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context.configurated, attrs, defStyleAttr)
}

val Context.configurated: Context
    get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        createConfigurationContext(Configuration())
    else this
