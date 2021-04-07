package org.cryse.widget.persistentsearch;

import java.util.Collection;

public interface SearchSuggestionsBuilder {
    Collection<SearchItem> buildEmptySearchSuggestion(int maxCount);
    Collection<SearchItem> buildSearchSuggestion(int maxCount, String query);
}
