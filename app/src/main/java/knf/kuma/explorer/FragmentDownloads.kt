package knf.kuma.explorer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import knf.kuma.R
import knf.kuma.database.CacheDB
import knf.kuma.pojos.DownloadObject
import kotlinx.android.synthetic.main.recycler_downloading.*

class FragmentDownloads : FragmentBase() {
    private var isFirst = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CacheDB.INSTANCE.downloadsDAO().active.observe(this, Observer { downloadObjects ->
            progress!!.visibility = View.GONE
            error!!.visibility = if (downloadObjects.isEmpty()) View.VISIBLE else View.GONE
            if (isFirst || downloadObjects.isEmpty() || recycler.adapter != null && downloadObjects.size > recycler.adapter!!.itemCount) {
                isFirst = false
                recycler.adapter = DownloadingAdapter(this@FragmentDownloads, downloadObjects as MutableList<DownloadObject>)
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recycler_downloading, container, false)
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    companion object {

        fun get(): FragmentDownloads {
            return FragmentDownloads()
        }
    }
}
