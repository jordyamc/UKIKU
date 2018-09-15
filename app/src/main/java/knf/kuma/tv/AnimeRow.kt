package knf.kuma.tv

import androidx.leanback.widget.ArrayObjectAdapter

class AnimeRow {

    internal var page: Int = 0
    internal var id: Int = 0
    internal var adapter: ArrayObjectAdapter? = null
    internal var title: String? = null

    fun getPage(): Int {
        return page
    }

    fun setPage(page: Int): AnimeRow {
        this.page = page
        return this
    }

    fun getId(): Int {
        return id
    }

    fun setId(id: Int): AnimeRow {
        this.id = id
        return this
    }

    fun getAdapter(): ArrayObjectAdapter? {
        return adapter
    }

    fun setAdapter(adapter: ArrayObjectAdapter): AnimeRow {
        this.adapter = adapter
        return this
    }

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String): AnimeRow {
        this.title = title
        return this
    }
}
