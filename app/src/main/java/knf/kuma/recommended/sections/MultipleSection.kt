package knf.kuma.recommended.sections

import android.app.Activity
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.recommended.AnimeShortObject
import knf.kuma.recommended.RHHolder
import knf.kuma.recommended.RIHolder

/**
 * Created by jordy on 26/03/2018.
 */

class MultipleSection(private val activity: Activity, private val name: String, list: MutableList<AnimeShortObject>, isGrid: Boolean) : StatelessSection(SectionParameters.builder()
        .itemResourceId(if (isGrid) R.layout.item_fav_grid else R.layout.item_fav)
        .headerResourceId(R.layout.item_recommend_header)
        .build()) {
    private val animeObjects = list

    override fun getContentItemsTotal(): Int {
        return animeObjects.size
    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
        return RIHolder(view)
    }

    override fun onBindItemViewHolder(h: RecyclerView.ViewHolder, position: Int) {
        val holder = h as RIHolder
        val animeObject = animeObjects[position]
        PicassoSingle.get().load(PatternUtil.getCover(animeObject.aid)).into(holder.img)
        holder.title.text = animeObject.name
        holder.type.text = animeObject.type
        holder.cardView.setOnClickListener { ActivityAnime.open(activity, animeObject, holder.img, true, true) }
    }

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
        return RHHolder(view)
    }

    override fun onBindHeaderViewHolder(h: RecyclerView.ViewHolder?) {
        val holder = h as RHHolder?
        holder?.title?.text = name
    }
}
