package knf.kuma.changelog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.changelog.objects.Changelog
import org.jetbrains.anko.find

internal class ReleaseAdapter(changelog: Changelog) : RecyclerView.Adapter<ReleaseAdapter.ReleaseItem>() {

    private val list = changelog.releases

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReleaseItem {
        return ReleaseItem(LayoutInflater.from(parent.context).inflate(R.layout.item_changelog, parent, false))
    }

    override fun onBindViewHolder(holder: ReleaseItem, position: Int) {
        val release = list[position]
        holder.version.text = release.version
        holder.code.text = release.code
        holder.recyclerView.adapter = ChangeAdapter(release)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    internal inner class ReleaseItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val version: TextView = itemView.find(R.id.version)
        val code: TextView = itemView.find(R.id.code)
        val recyclerView: RecyclerView = itemView.find(R.id.recycler)
    }
}
