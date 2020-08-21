package knf.kuma.search

import androidx.recyclerview.widget.DiffUtil
import knf.kuma.database.CacheDB

open class SearchObjectFav(val key: Int, val aid: String, val name: String, val link: String) {

    var isFav = CacheDB.INSTANCE.favsDAO().isFav(aid.toInt())

    override fun equals(other: Any?): Boolean {
        return other is SearchObjectFav &&
                key == other.key &&
                aid == other.aid &&
                name == other.name &&
                link == other.link
    }

    override fun hashCode(): Int {
        return "$key$aid$name$link".hashCode()
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SearchObjectFav>() {
            override fun areItemsTheSame(oldItem: SearchObjectFav, newItem: SearchObjectFav): Boolean =
                    oldItem.key == newItem.key

            override fun areContentsTheSame(oldItem: SearchObjectFav, newItem: SearchObjectFav): Boolean =
                    oldItem == newItem
        }
    }
}

fun SearchObject.forFav(): SearchObjectFav = SearchObjectFav(key, aid, name, link)