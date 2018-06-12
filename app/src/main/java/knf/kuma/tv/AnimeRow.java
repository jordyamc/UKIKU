package knf.kuma.tv;

import android.support.v17.leanback.widget.ArrayObjectAdapter;

public class AnimeRow {

    private int page;
    private int id;
    private ArrayObjectAdapter adapter;
    private String title;

    public AnimeRow() {
    }

    public int getPage() {
        return page;
    }

    public AnimeRow setPage(int page) {
        this.page = page;
        return this;
    }

    public int getId() {
        return id;
    }

    public AnimeRow setId(int id) {
        this.id = id;
        return this;
    }

    public ArrayObjectAdapter getAdapter() {
        return adapter;
    }

    public AnimeRow setAdapter(ArrayObjectAdapter adapter) {
        this.adapter = adapter;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public AnimeRow setTitle(String title) {
        this.title = title;
        return this;
    }
}
