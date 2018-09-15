package knf.kuma.search

import android.content.Context
import knf.kuma.R
import org.cryse.widget.persistentsearch.SearchItem
import org.cryse.widget.persistentsearch.SearchSuggestionsBuilder
import java.util.*

class FiltersSuggestion(private val context: Context) : SearchSuggestionsBuilder {

    override fun buildEmptySearchSuggestion(maxCount: Int): Collection<SearchItem> {
        return ArrayList()
    }

    override fun buildSearchSuggestion(maxCount: Int, query: String): Collection<SearchItem> {
        val items = ArrayList<SearchItem>()
        if (query == ":") {
            val drawable = context.getDrawable(R.drawable.ic_hash)
            items.add(SearchItem("En emisión", ":emision:", SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable))
            items.add(SearchItem("Finalizados", ":finalizado:", SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable))
            items.add(SearchItem("Animes", ":anime:", SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable))
            items.add(SearchItem("Ovas", ":ova:", SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable))
            items.add(SearchItem("Películas", ":pelicula:", SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable))
            items.add(SearchItem("Personalizado", ":personalizado:", SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable))
        }
        return items
    }
}
