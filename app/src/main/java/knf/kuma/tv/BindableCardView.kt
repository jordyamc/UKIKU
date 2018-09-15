package knf.kuma.tv

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView

import androidx.annotation.LayoutRes
import androidx.leanback.widget.BaseCardView

abstract class BindableCardView<T> : BaseCardView {

    abstract val imageView: ImageView

    @get:LayoutRes
    abstract val layoutResource: Int

    constructor(context: Context) : super(context) {
        initLayout()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initLayout()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initLayout()
    }

    private fun initLayout() {
        isFocusable = true
        isFocusableInTouchMode = true
        val inflater = LayoutInflater.from(context)
        inflater.inflate(layoutResource, this)
    }

    abstract fun bind(data: T)
}
