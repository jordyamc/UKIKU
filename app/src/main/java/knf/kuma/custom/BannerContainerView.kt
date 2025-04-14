package knf.kuma.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import knf.kuma.R
import org.jetbrains.anko.find

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
        if (showBottom) {
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                find<View>(R.id.spaceBottom).layoutParams.height = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                WindowInsetsCompat.CONSUMED
            }
            find<View>(R.id.spaceBottom).visibility = View.VISIBLE
        }
        find<View>(R.id.spaceTop).visibility = View.VISIBLE
        with(find<ViewGroup>(R.id.container)) {
            removeAllViews()
            addView(view)
        }
    }
}