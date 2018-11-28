package knf.kuma.achievements

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.commons.bind
import knf.kuma.pojos.Achievement
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.text.NumberFormat
import java.util.*

class AchievementAdapter(private val onClick: (achievement: Achievement) -> Unit) : RecyclerView.Adapter<AchievementAdapter.ItemHolder>() {

    private var list: List<Achievement> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_achievements, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val achievement = list[position]
        holder.icon.setImageResource(achievement.usableIcon())
        holder.name.text = achievement.usableName()
        holder.state.text = achievement.getState()
        holder.exp.text = "${NumberFormat.getNumberInstance(Locale.US).format(achievement.points)} XP"
        holder.root.onClick { onClick.invoke(achievement) }
    }

    fun setAchievements(list: List<Achievement>) {
        this.list = list
        notifyDataSetChanged()
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View by itemView.bind(R.id.root)
        val icon: ImageView by itemView.bind(R.id.icon)
        val name: TextView by itemView.bind(R.id.name)
        val state: TextView by itemView.bind(R.id.state)
        val exp: TextView by itemView.bind(R.id.exp)
    }
}