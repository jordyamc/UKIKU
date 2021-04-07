package org.cryse.widget.persistentsearch;

public class SimpleSearchListener implements PersistentSearchView.SearchListener {

    @Override
    public boolean onSuggestion(SearchItem searchItem) {
        return true;
    }

    @Override
    public void onSearchCleared() {

    }

    @Override
    public void onSearchTermChanged(String term) {

    }

    @Override
    public void onSearch(String query) {

    }

    @Override
    public void onSearchEditOpened() {

    }

    @Override
    public void onSearchEditClosed() {

    }

    @Override
    public boolean onSearchEditBackPressed() {
        return false;
    }

    @Override
    public void onSearchExit() {

    }
}
