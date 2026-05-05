package knf.kuma.custom

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.Keep
import androidx.recyclerview.widget.LinearLayoutManager

class VariantLinearLayoutManager : LinearLayoutManager {
    @Keep
    constructor(context: Context) : super(context)
    @Keep
    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(context, orientation, reverseLayout)
    @Keep
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun supportsPredictiveItemAnimations(): Boolean = false
}