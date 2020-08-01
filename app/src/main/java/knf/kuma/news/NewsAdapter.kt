package knf.kuma.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.AdCallback
import knf.kuma.ads.AdCardItemHolder
import knf.kuma.ads.AdsUtilsMob
import knf.kuma.ads.implAdsNews
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.isSameContent
import knf.kuma.commons.noCrashLet
import kotlinx.android.synthetic.main.item_news.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.coroutines.onClick

class NewsAdapter(val activity: AppCompatActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var list: List<NewsObject> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1)
            return AdCardItemHolder(parent, AdCardItemHolder.TYPE_NEWS).also {
                it.loadAd(activity.lifecycleScope, object : AdCallback {
                    override fun getID(): String = AdsUtilsMob.NEWS_BANNER
                }, 500)
            }
        return NewsHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return noCrashLet { if (list[position] is AdNewsObject) 1 else 0 } ?: 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val newsObject = list[position]
        if (holder is NewsHolder) {
            holder.metadata.text = newsObject.metaData()
            holder.title.text = newsObject.title
            holder.description.text = newsObject.description
            holder.card.onClick {
                AchievementManager.onNewsOpened()
                NewsCreator.openNews(activity, newsObject)
            }
        }
    }

    fun update(list: MutableList<NewsObject>) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            if (PrefsUtil.isNativeAdsEnabled)
                list.implAdsNews()
            if (this@NewsAdapter.list isSameContent list)
                return@launch
            this@NewsAdapter.list = list
            launch(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
    }

    class NewsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.card
        val metadata: TextView = itemView.metadata
        val title: TextView = itemView.title
        val description: TextView = itemView.description
    }
}