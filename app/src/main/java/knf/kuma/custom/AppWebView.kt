package knf.kuma.custom

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class AppWebView : WebView {
    constructor(context: Context) : super(context.applicationContext)

    constructor(context: Context, attrs: AttributeSet) : super(context.applicationContext, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context.applicationContext, attrs, defStyleAttr)
}
