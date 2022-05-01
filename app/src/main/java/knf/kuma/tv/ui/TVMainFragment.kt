package knf.kuma.tv.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.SparseArray
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import com.dropbox.core.android.Auth
import knf.kuma.App
import knf.kuma.Diagnostic
import knf.kuma.R
import knf.kuma.backup.Backups
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.backup.framework.BackupService
import knf.kuma.backup.framework.DropBoxService
import knf.kuma.commons.distinct
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirObject
import knf.kuma.home.StaffRecommendations
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.RecordObject
import knf.kuma.retrofit.Repository
import knf.kuma.tv.AnimeRow
import knf.kuma.tv.GlideBackgroundManager
import knf.kuma.tv.TVServersFactory
import knf.kuma.tv.anime.*
import knf.kuma.tv.details.TVAnimesDetails
import knf.kuma.tv.directory.DirPresenter
import knf.kuma.tv.search.TVSearch
import knf.kuma.tv.sections.DirSection
import knf.kuma.tv.sections.EmissionSection
import knf.kuma.tv.sections.SectionObject
import knf.kuma.tv.sync.BypassObject
import knf.kuma.tv.sync.LogOutObject
import knf.kuma.tv.sync.SyncObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xdroid.toaster.Toaster
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
class TVMainFragment : BrowseSupportFragment(), OnItemViewClickedListener, View.OnClickListener {
    private var mRows: SparseArray<AnimeRow>? = null

    private var backgroundManager: GlideBackgroundManager? = null
    private var service: BackupService? = null
    private var waitingLoginDropbox = false
    private var waitingLoginFirestore = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        backgroundManager = GlideBackgroundManager(activity as Activity)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        title = "UKIKU"
        brandColor = Color.parseColor("#424242")
        searchAffordanceColor = ContextCompat.getColor(App.context, R.color.colorAccent)
        setOnSearchClickedListener(this)
        createDataRows()
        createRows()
        prepareEntranceTransition()
        fetchData()
        service = Backups.createService()
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
        mRows?.put(STAFF, AnimeRow()
                .setId(STAFF)
                .setAdapter(ArrayObjectAdapter(DirPresenter()))
                .setTitle("Recomendados")
                .setPage(1))
        mRows?.put(BEST, AnimeRow()
                .setId(BEST)
                .setAdapter(ArrayObjectAdapter(DirPresenter()))
                .setTitle("Mejores en emisión")
                .setPage(1))
        mRows?.put(BESTGLOBAL, AnimeRow()
                .setId(BESTGLOBAL)
                .setAdapter(ArrayObjectAdapter(DirPresenter()))
                .setTitle("Mejores en general")
                .setPage(1))
        mRows?.put(SECTIONS, AnimeRow()
                .setId(SECTIONS)
                .setAdapter(ArrayObjectAdapter(SectionPresenter()))
                .setTitle("Secciones")
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
        onItemViewClickedListener = this
    }

    private fun fetchData() {
        Repository().reloadRecents()
        activity?.let {
            CacheDB.INSTANCE.recentsDAO().objects.distinct.observe(it, { recentObjects ->
                mRows?.get(RECENTS)?.apply {
                    page = page.plus(1)
                    adapter?.apply {
                        clear()
                        addAll(0, recentObjects)
                    }
                }
                startEntranceTransition()
            })
            CacheDB.INSTANCE.recordsDAO().allLive.distinct.observe(it, { recordObjects ->
                mRows?.get(LAST_SEEN)?.apply {
                    page = page.plus(1)
                    adapter?.apply {
                        clear()
                        addAll(0, recordObjects)
                    }
                }
                startEntranceTransition()
            })
            CacheDB.INSTANCE.favsDAO().all.distinct.observe(it, { favoriteObjects ->
                mRows?.get(FAVORITES)?.apply {
                    page = page.plus(1)
                    adapter?.apply {
                        clear()
                        addAll(0, favoriteObjects)
                    }
                }
                startEntranceTransition()
            })
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                var recList = emptyList<DirObject>()
                while (recList.size < 15) {
                    delay(100)
                    recList = CacheDB.INSTANCE.animeDAO()
                        .animesDirWithIDRandomNL(StaffRecommendations.randomIds(15))
                }
                launch(Dispatchers.Main) {
                    mRows?.get(STAFF)?.apply {
                        page = page.plus(1)
                        adapter?.apply {
                            clear()
                            addAll(0, recList)
                        }
                    }
                    startEntranceTransition()
                }
            }
            CacheDB.INSTANCE.animeDAO().emissionVotesLimited.distinct.observe(
                it,
                { emissionObjects ->
                    mRows?.get(BEST)?.apply {
                        page = page.plus(1)
                        adapter?.apply {
                            clear()
                            addAll(0, emissionObjects)
                        }
                    }
                    startEntranceTransition()
                })
            CacheDB.INSTANCE.animeDAO().allVotesLimited.distinct.observe(it, { emissionObjects ->
                mRows?.get(BESTGLOBAL)?.apply {
                    page = page.plus(1)
                    adapter?.apply {
                        clear()
                        addAll(0, emissionObjects)
                    }
                }
                startEntranceTransition()
            })
            arrayListOf(DirSection(), EmissionSection()).let { sections ->
                mRows?.get(SECTIONS)?.apply {
                    page = page.plus(1)
                    adapter?.apply {
                        clear()
                        addAll(0, sections)
                    }
                }
                startEntranceTransition()
            }
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
        } else if (item is DirObject) {
            context?.let { TVAnimesDetails.start(it, item.link) }
        } else if (item is SectionObject) {
            item.open(context)
        } else if (item is SyncObject) {
            when (item) {
                is LogOutObject -> {
                    service?.logOut()
                    activity?.let { FirestoreManager.doSignOut(it) }
                    Backups.type = Backups.Type.NONE
                    onLogin()
                }
                is BypassObject -> startActivity(Intent(context, Diagnostic.FullBypass::class.java))
                else -> {
                    when (item.type) {
                        Backups.Type.DROPBOX -> {
                            waitingLoginDropbox = true
                            if (item.type == Backups.Type.DROPBOX)
                                service = DropBoxService().also { it.logIn() }
                        }
                        Backups.Type.FIRESTORE -> {
                            waitingLoginFirestore = true
                            activity?.let { FirestoreManager.doLogin(it) }
                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingLoginDropbox) {
            val token = Auth.getOAuth2Token()
            if (service is DropBoxService && service?.logIn(token) == true) {
                Backups.type = Backups.Type.DROPBOX
            }
            onLogin()
        }
        if (waitingLoginFirestore) {
            onLogin()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activity?.let { FirestoreManager.handleLogin(it, requestCode, resultCode, data) }
    }

    private fun onLogin() {
        if (service?.isLoggedIn == false && waitingLoginDropbox) {
            Toaster.toast("Error al iniciar sesión")
        } else {
            val adapter = ArrayObjectAdapter(SyncPresenter())
            val headerItem = HeaderItem(SYNC.toLong(), "Sincronización")
            if (service?.isLoggedIn == true || FirestoreManager.isLoggedIn) {
                adapter.add(LogOutObject())
                Backups.restoreAll()
            } else {
                adapter.add(SyncObject(Backups.Type.DROPBOX))
                adapter.add(SyncObject(Backups.Type.FIRESTORE))
            }
            adapter.add(BypassObject())
            if (getAdapter().size() == 7)
                (getAdapter() as ArrayObjectAdapter).add(SYNC, ListRow(headerItem, adapter))
            else
                (getAdapter() as ArrayObjectAdapter).replace(SYNC, ListRow(headerItem, adapter))
        }
        waitingLoginDropbox = false
        waitingLoginFirestore = false
    }

    companion object {

        private const val RECENTS = 0
        private const val LAST_SEEN = 1
        private const val FAVORITES = 2
        private const val STAFF = 3
        private const val BEST = 4
        private const val BESTGLOBAL = 5
        private const val SECTIONS = 6
        private const val SYNC = 7

        fun get(): TVMainFragment {
            return TVMainFragment()
        }
    }
}
