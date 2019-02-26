package knf.kuma.custom

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import knf.kuma.R
import knf.kuma.commons.doOnUI
import kotlinx.android.synthetic.main.layout_loading_text.view.*

class StateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {
    private var titleText = "PlaceHolder"

    init {
        attrs?.let {
            val array = context.obtainStyledAttributes(it, R.styleable.StateView)
            titleText = array.getString(R.styleable.StateView_sv_title) ?: titleText
            array.recycle()
        }
        View.inflate(context, R.layout.layout_loading_text, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        title.text = titleText
    }

    fun load(contentText: String) {
        val shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        doOnUI {
            text.apply {
                text = contentText
                alpha = 0f
                visibility = View.VISIBLE
                animate()
                        .alpha(1f)
                        .setDuration(shortAnimationDuration)
                        .setListener(null)
            }
            loading.animate()
                    .alpha(0f)
                    .setDuration(shortAnimationDuration)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            loading.visibility = View.GONE
                        }
                    })
        }
    }
}