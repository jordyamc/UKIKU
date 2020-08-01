package knf.kuma.achievements

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.ads.AdCallback
import knf.kuma.ads.AdCardItemHolder
import knf.kuma.ads.AdsUtilsMob
import knf.kuma.ads.implAdsAchievement
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.noCrash
import knf.kuma.commons.noCrashLet
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.AchievementAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class AchievementAdapter(private val onClick: (achievement: Achievement) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var list: MutableList<Achievement> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1)
            return AdCardItemHolder(parent, AdCardItemHolder.TYPE_ACHIEVEMENT).also {
                it.loadAd(GlobalScope, object : AdCallback {
                    override fun getID(): String = AdsUtilsMob.ACHIEVEMENT_BANNER
                }, 500)
            }
        return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_achievements, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return noCrashLet { if (list[position] is AchievementAd) 1 else 0 } ?: 1
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val achievement = list[position]
        if (holder is ItemHolder) {
            holder.icon.setImageResource(achievement.usableIcon())
            holder.name.text = achievement.usableName()
            holder.state.text = achievement.getState()
            holder.exp.text = "${NumberFormat.getNumberInstance(Locale.US).format(achievement.points)} XP"
            holder.root.setOnClickListener { noCrash { onClick.invoke(achievement) } }
        }
    }

    fun setAchievements(list: MutableList<Achievement>) {
        GlobalScope.launch(Dispatchers.IO) {
            this@AchievementAdapter.list = list
            if (PrefsUtil.isNativeAdsEnabled)
                this@AchievementAdapter.list.implAdsAchievement()
            launch(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View by itemView.bind(R.id.root)
        val icon: ImageView by itemView.bind(R.id.icon)
        val name: TextView by itemView.bind(R.id.name)
        val state: TextView by itemView.bind(R.id.state)
        val exp: TextView by itemView.bind(R.id.exp)
    }
}