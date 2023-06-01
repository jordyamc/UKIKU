package knf.kuma.news

import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.inflate
import knf.kuma.commons.load
import knf.kuma.commons.noCrash
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick

class MaterialNewsAdapter(val activity: AppCompatActivity) : PagingDataAdapter<NewsItem, MaterialNewsAdapter.NewsViewHolder>(NewsItem.DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder =
            NewsViewHolder(parent.inflate(R.layout.item_news_material))

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = getItem(position)
        if (item == null)
            holder.apply {
                root.setOnClickListener(null)
                image.setImageDrawable(null)
                progress.visibility = View.VISIBLE
                type.text = null
                title.text = null
                date.text = null
            }
        else
            holder.apply {
                root.onClick {
                    AchievementManager.onNewsOpened()
                    openNews(activity, item)
                }
                noCrash { image.load(item.image) }
                progress.visibility = View.GONE
                type.text = item.type
                title.text = item.title
                date.text = item.date
            }
    }

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = itemView.find(R.id.root)
        val image: ShapeableImageView = itemView.find(R.id.image)
        val progress: ProgressBar = itemView.find(R.id.progress)
        val type: TextView = itemView.find(R.id.type)
        val title: TextView = itemView.find(R.id.title)
        val date: TextView = itemView.find(R.id.date)
    }
}