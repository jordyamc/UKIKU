package knf.kuma.custom

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.Keep
import androidx.recyclerview.widget.GridLayoutManager

class VariantGridLayoutManager : GridLayoutManager {
    @Keep
    constructor(context: Context, spanCount: Int) : super(context, spanCount)
    @Keep
    constructor(context: Context, spanCount: Int, orientation: Int, reverseLayout: Boolean) : super(context, spanCount, orientation, reverseLayout)
    @Keep
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun supportsPredictiveItemAnimations(): Boolean = false
}