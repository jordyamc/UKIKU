package knf.kuma.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import knf.kuma.R
import kotlinx.android.synthetic.main.lay_banner_container.view.*

class BannerContainerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {
    private var showBottom = false

    init {
        attrs?.let {
            val array = context.obtainStyledAttributes(it, R.styleable.BannerContainerView)
            showBottom = array.getBoolean(R.styleable.BannerContainerView_showBottomSpace, false)
            array.recycle()
        }
        View.inflate(context, R.layout.lay_banner_container, this)
    }

    fun show(view: View) {
        if (showBottom)
            spaceBottom.visibility = View.VISIBLE
        spaceTop.visibility = View.VISIBLE
        container.removeAllViews()
        container.addView(view)
    }
}