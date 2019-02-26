package knf.kuma.custom

import android.content.Context
import android.util.AttributeSet
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

open class FSRecyclerView : FastScrollRecyclerView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun scrollToPositionAtProgress(touchFraction: Float): String {
        return try {
            super.scrollToPositionAtProgress(touchFraction)
        } catch (e: Exception) {
            ""
        }
    }
}
