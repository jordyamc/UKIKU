package knf.kuma.recents.viewholders

import android.view.View
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import knf.kuma.R
import knf.kuma.commons.EAHelper
import org.jetbrains.anko.find

class RecyclerRefreshHolder(view: View) {
    val recyclerView: RecyclerView = view.find(R.id.recycler)
    val refreshLayout: SwipeRefreshLayout = view.find(R.id.refresh)
    val error: View = view.find(R.id.error)
    private val layoutManager: LinearLayoutManager = LinearLayoutManager(view.context)

    init {
        recyclerView.layoutManager = layoutManager
        recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(view.context, R.anim.layout_fall_down)
        refreshLayout.setColorSchemeResources(EAHelper.getThemeColor(), EAHelper.getThemeColorLight(), R.color.colorPrimary)
    }

    fun scrollToTop() {
        layoutManager.smoothScrollToPosition(recyclerView, null, 0)
    }

    fun scrollTo(position: Int) {
        layoutManager.smoothScrollToPosition(recyclerView, null, position)
    }

    fun setRefreshing(refreshing: Boolean) {
        refreshLayout.post { refreshLayout.isRefreshing = refreshing }
    }

    fun setError(visible: Boolean) {
        error.post { error.visibility = if (visible) View.VISIBLE else View.GONE }
    }
}
