package knf.kuma.search;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.cryse.widget.persistentsearch.SearchItem;
import org.cryse.widget.persistentsearch.SearchSuggestionsBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import knf.kuma.R;

public class FiltersSuggestion implements SearchSuggestionsBuilder {

    private Context context;

    public FiltersSuggestion(Context context) {
        this.context = context;
    }

    @Override
    public Collection<SearchItem> buildEmptySearchSuggestion(int maxCount) {
        return new ArrayList<>();
    }

    @Override
    public Collection<SearchItem> buildSearchSuggestion(int maxCount, String query) {
        List<SearchItem> items=new ArrayList<>();
        if (query.equals(":")){
            Drawable drawable=context.getResources().getDrawable(R.drawable.ic_hash);
            items.add(new SearchItem("En emisión",":emision:",SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable));
            items.add(new SearchItem("Finalizados",":finalizado:",SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable));
            items.add(new SearchItem("Animes",":anime:",SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable));
            items.add(new SearchItem("Ovas",":ova:",SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable));
            items.add(new SearchItem("Películas",":pelicula:",SearchItem.TYPE_SEARCH_ITEM_CUSTOM, drawable));
        }
        return items;
    }
}
