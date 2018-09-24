package knf.kuma.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import knf.kuma.R

class HiddenOverlay : LinearLayout {
    constructor(context: Context) : super(context) {
        inflate(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflate(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(context)
    }

    private fun inflate(context: Context) {
        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_hidden_overlay, this)
    }

    fun setHidden(hidden: Boolean, animate: Boolean) {
        setState(hidden)
        if (animate)
            post {
                val animation = AnimationUtils.loadAnimation(context, if (hidden) R.anim.fadein else R.anim.fadeout)
                animation.duration = 200
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {

                    }

                    override fun onAnimationEnd(animation: Animation) {

                    }

                    override fun onAnimationRepeat(animation: Animation) {

                    }
                })
                startAnimation(animation)
            }
    }

    private fun setState(hidden: Boolean) {
        post { visibility = if (hidden) View.VISIBLE else View.GONE }
    }
}
