package knf.kuma.tv.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.SparseArray
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Observer
import com.dropbox.core.android.Auth
import knf.kuma.App
import knf.kuma.R
import knf.kuma.backup.BUUtils
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.RecordObject
import knf.kuma.retrofit.Repository
import knf.kuma.tv.AnimeRow
import knf.kuma.tv.GlideBackgroundManager
import knf.kuma.tv.TVServersFactory
import knf.kuma.tv.anime.FavPresenter
import knf.kuma.tv.anime.RecentsPresenter
import knf.kuma.tv.anime.RecordPresenter
import knf.kuma.tv.anime.SyncPresenter
import knf.kuma.tv.details.TVAnimesDetails
import knf.kuma.tv.search.TVSearch
import knf.kuma.tv.sync.LogOutObject
import knf.kuma.tv.sync.SyncObject
import xdroid.toaster.Toaster

class TVMainFragment : BrowseSupportFragment(), OnItemViewSelectedListener, OnItemViewClickedListener, View.OnClickListener, BUUtils.LoginInterface {
    private var mRows: SparseArray<AnimeRow>? = null

    private var backgroundManager: GlideBackgroundManager? = null

    private var waitingLogin = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        backgroundManager = GlideBackgroundManager(activity as Activity)
        headersState = BrowseSupportFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        title = "UKIKU"
        brandColor = Color.parseColor("#424242")
        searchAffordanceColor = ContextCompat.getColor(App.context, R.color.colorAccent)
        setOnSearchClickedListener(this)
        createDataRows()
        createRows()
        prepareEntranceTransition()
        fetchData()
        BUUtils.init(activity as Activity, this, savedInstanceState == null)
        if (!BUUtils.isLogedIn)
            onLogin()
    }

    private fun createDataRows() {
        mRows = SparseArray()
        mRows?.put(RECENTS, AnimeRow()
                .setId(RECENTS)
                .setAdapter(ArrayObjectAdapter(RecentsPresenter()))
                .setTitle("Recientes")
                .setPage(1))
        mRows?.put(LAST_SEEN, AnimeRow()
                .setId(LAST_SEEN)
                .setAdapter(ArrayObjectAdapter(RecordPresenter()))
                .setTitle("Ultimos vistos")
                .setPage(1))
        mRows?.put(FAVORITES, AnimeRow()
                .setId(FAVORITES)
                .setAdapter(ArrayObjectAdapter(FavPresenter()))
                .setTitle("Favoritos")
                .setPage(1))
    }

    private fun createRows() {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        for (i in 0 until (mRows?.size() ?: 0)) {
            val row = mRows?.get(i)
            val headerItem = HeaderItem(row?.id?.toLong() ?: 0, row?.title)
            val listRow = ListRow(headerItem, row?.adapter)
            rowsAdapter.add(listRow)
        }
        adapter = rowsAdapter
        onItemViewSelectedListener = this
        onItemViewClickedListener = this
    }

    private fun fetchData() {
        Repository().reloadRecents()
        activity?.let {
            CacheDB.INSTANCE.recentsDAO().objects.observe(it, Observer { recentObjects ->
                val row = mRows?.get(RECENTS)
                row?.page = row?.page?.plus(1) ?: 0
                row?.adapter?.clear()
                for (recentObject in recentObjects) {
                    row?.adapter?.add(recentObject)
                }
                startEntranceTransition()
            })
            CacheDB.INSTANCE.recordsDAO().all.observe(it, Observer { recordObjects ->
                val row = mRows?.get(LAST_SEEN)
                row?.page = row?.page?.plus(1) ?: 0
                row?.adapter?.clear()
                for (recordObject in recordObjects) {
                    row?.adapter?.add(recordObject)
                }
                startEntranceTransition()
            })
            CacheDB.INSTANCE.favsDAO().all.observe(it, Observer { favoriteObjects ->
                val row = mRows?.get(FAVORITES)
                row?.page = row?.page?.plus(1) ?: 0
                row?.adapter?.clear()
                for (favoriteObject in favoriteObjects) {
                    row?.adapter?.add(favoriteObject)
                }
                startEntranceTransition()
            })
        }
    }

    override fun onClick(v: View) {
        context?.let { TVSearch.start(it) }
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is RecentObject) {
            TVServersFactory.start(activity as Activity, item.url, AnimeObject.WebInfo.AnimeChapter.fromRecent(item), activity as TVServersFactory.ServersInterface)
        } else if (item is RecordObject) {
            if (item.animeObject != null)
                context?.let { TVAnimesDetails.start(it, item.animeObject.link) }
            else
                Toaster.toast("Anime no encontrado")
        } else if (item is FavoriteObject) {
            context?.let { TVAnimesDetails.start(it, item.link) }
        } else if (item is SyncObject) {
            if (item is LogOutObject) {
                BUUtils.logOut()
                onLogin()
            } else {
                waitingLogin = true
                if (item.isDropbox)
                    BUUtils.startClient(BUUtils.BUType.DROPBOX, false)
                else
                    BUUtils.startClient(BUUtils.BUType.DRIVE, false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingLogin) {
            val token = Auth.getOAuth2Token()
            if (token != null)
                BUUtils.type = BUUtils.BUType.DROPBOX
            BUUtils.setDropBoxClient(token)
        }
    }

    override fun onLogin() {
        if (!BUUtils.isLogedIn && waitingLogin) {
            Toaster.toast("Error al iniciar sesión")
        } else {
            val adapter = ArrayObjectAdapter(SyncPresenter())
            val headerItem = HeaderItem(SYNC.toLong(), "Sincronización")
            if (BUUtils.isLogedIn) {
                adapter.add(LogOutObject())
                BUUtils.silentRestoreAll()
            } else {
                adapter.add(SyncObject(true))
                adapter.add(SyncObject(false))
            }
            if (getAdapter().size() == 3)
                (getAdapter() as ArrayObjectAdapter).add(SYNC, ListRow(headerItem, adapter))
            else
                (getAdapter() as ArrayObjectAdapter).replace(SYNC, ListRow(headerItem, adapter))
        }
        waitingLogin = false
    }

    override fun onItemSelected(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        /*String img=null;
        if (item instanceof RecentObject){
            img=PatternUtil.getCover(((RecentObject)item).aid);
        }else if (item instanceof RecordObject){
            img=PatternUtil.getCover(((RecordObject)item).aid);
        }else if (item instanceof FavoriteObject){
            img=PatternUtil.getCover(((FavoriteObject)item).aid);
        }
        if (img!=null){
            backgroundManager.cancelBackgroundChange();
            backgroundManager.loadImage(img);
        }else
            backgroundManager.setBackground(null);*/
    }

    companion object {

        private const val RECENTS = 0
        private const val LAST_SEEN = 1
        private const val FAVORITES = 2
        private const val SYNC = 3

        fun get(): TVMainFragment {
            return TVMainFragment()
        }
    }
}
