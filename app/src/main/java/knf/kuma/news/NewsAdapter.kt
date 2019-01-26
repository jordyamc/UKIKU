package knf.kuma.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.isSameContent
import kotlinx.android.synthetic.main.item_news.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class NewsAdapter(val activity: AppCompatActivity) : RecyclerView.Adapter<NewsAdapter.NewsHolder>() {

    var list: List<NewsObject> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsHolder {
        return NewsHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: NewsHolder, position: Int) {
        val newsObject = list[position]
        holder.metadata.text = newsObject.metaData()
        holder.title.text = newsObject.title
        holder.description.text = newsObject.description
        holder.card.onClick {
            AchievementManager.onNewsOpened()
            NewsCreator.openNews(activity, newsObject)
        }
    }

    fun update(list: List<NewsObject>) {
        if (this.list isSameContent list)
            return
        this.list = list
        notifyDataSetChanged()
    }

    class NewsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.card
        val metadata: TextView = itemView.metadata
        val title: TextView = itemView.title
        val description: TextView = itemView.description
    }
}