package knf.kuma.custom

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import knf.kuma.R
import knf.kuma.commons.doOnUIGlobal
import kotlinx.android.synthetic.main.layout_loading_text.view.*
import org.jetbrains.anko.textColor

class StateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {
    private var titleText = "PlaceHolder"
    private var isSetted = false

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

    fun load(contentText: String, state: Int = STATE_NORMAL) {
        val shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        doOnUIGlobal {
            visibility = View.VISIBLE
            when (state) {
                STATE_OK -> text.textColor = ContextCompat.getColor(context, R.color.stateOk)
                STATE_WARNING -> text.textColor = ContextCompat.getColor(context, R.color.stateWarning)
                STATE_ERROR -> text.textColor = ContextCompat.getColor(context, R.color.stateError)
            }
            text.apply {
                text = contentText
                if (!isSetted) {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate()
                            .alpha(1f)
                            .setDuration(shortAnimationDuration)
                            .setListener(null)
                }
            }
            if (!isSetted)
                loading.animate()
                        .alpha(0f)
                        .setDuration(shortAnimationDuration)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                loading.visibility = View.GONE
                            }
                        })
            isSetted = true
        }
    }

    companion object {
        const val STATE_NORMAL = 0
        const val STATE_OK = 1
        const val STATE_WARNING = 2
        const val STATE_ERROR = 3
    }
}