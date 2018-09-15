package knf.kuma.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import butterknife.ButterKnife
import knf.kuma.R

class SeenAnimeOverlay : LinearLayout {
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
        inflater.inflate(R.layout.view_seen_overlay, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        ButterKnife.bind(this)
    }

    fun setSeen(seen: Boolean, animate: Boolean) {
        setState(seen)
        if (animate) {
            post {
                val animation = AnimationUtils.loadAnimation(context, if (seen) R.anim.fadein else R.anim.fadeout)
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
    }

    private fun setState(seen: Boolean) {
        post { visibility = if (seen) View.VISIBLE else View.GONE }
    }
}
