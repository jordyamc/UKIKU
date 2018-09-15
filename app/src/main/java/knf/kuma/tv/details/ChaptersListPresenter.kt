package knf.kuma.tv.details

import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.RowPresenter

class ChaptersListPresenter(val position: Int) : ListRowPresenter() {

    override fun onBindRowViewHolder(holder: RowPresenter.ViewHolder, item: Any) {
        super.onBindRowViewHolder(holder, item)
        val vh = holder as ListRowPresenter.ViewHolder
        vh.gridView.selectedPosition = position
    }
}
