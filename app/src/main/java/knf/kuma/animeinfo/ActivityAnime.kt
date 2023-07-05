package knf.kuma.animeinfo

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.InflateException
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.showRandomInterstitial
import knf.kuma.animeinfo.img.ActivityImgFull
import knf.kuma.animeinfo.viewholders.AnimeActivityHolder
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.CastUtil
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUI
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirObject
import knf.kuma.directory.DirObjectCompact
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.ExplorerObject
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.NotificationObj
import knf.kuma.pojos.QueueObject
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.RecordObject
import knf.kuma.pojos.SeeingObject
import knf.kuma.recommended.AnimeShortObject
import knf.kuma.recommended.RankType
import knf.kuma.recommended.RecommendHelper
import knf.kuma.search.SearchObject
import knf.kuma.search.SearchObjectFav
import knf.kuma.widgets.emision.WEListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onLongClick
import org.jetbrains.anko.toast
import xdroid.toaster.Toaster
import java.util.Locale
import kotlin.random.Random

class ActivityAnime : GenericActivity(), AnimeActivityHolder.Interface {
    private var isEdited = false
    private val viewModel: AnimeViewModel by viewModels()
    private val holder: AnimeActivityHolder by lazy { AnimeActivityHolder(this) }
    private var favoriteObject: FavoriteObject? = null
    private val dao = CacheDB.INSTANCE.favsDAO()
    private var chapters: MutableList<AnimeObject.WebInfo.AnimeChapter> = ArrayList()
    private var genres: MutableList<String> = ArrayList()
    private val aidOnly get() = intent?.getBooleanExtra(keyAidOnly, false) ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getThemeNA())
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_anime_info)
        } catch (e: InflateException) {
            setContentView(R.layout.activity_anime_info_nwv)
        }
        setSupportActionBar(holder.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        holder.toolbar.setNavigationOnClickListener { closeActivity() }
        if (aidOnly)
            viewModel.init(intent.getStringExtra(keyAid))
        else
            viewModel.init(this@ActivityAnime, intent.dataString, intent.getBooleanExtra(keyPersist, true))
        if (intent.getBooleanExtra(keyNotification, false))
            sendBroadcast(NotificationObj.fromIntent(intent).getBroadcast(this@ActivityAnime))
        load()
        checkBypass()
        showRandomInterstitial(this)
    }

    private fun load() {
        viewModel.liveData.observe(this, Observer { animeObject ->
            if (animeObject != null) {
                doOnUI {
                    chapters = animeObject.chapters ?: mutableListOf()
                    genres = animeObject.genres ?: mutableListOf()
                    if (PrefsUtil.isFamilyFriendly && genres.map { it.lowercase(Locale.getDefault()) }.contains("ecchi")) {
                        toast("Anime no familiar")
                        onBackPressed()
                    }
                    favoriteObject = FavoriteObject(animeObject)
                    favoriteObject?.let { fav ->
                        holder.imageView.onLongClick(returnValue = true) {
                            doAsync {
                                val isFav = dao.isFav(fav.key)
                                if (isFav) {
                                    holder.setFABState(false)
                                    dao.deleteFav(fav)
                                    RecommendHelper.registerAll(genres, RankType.UNFAV)
                                    doOnUI { toast("Removido de favoritos") }
                                } else {
                                    holder.setFABState(true)
                                    dao.addFav(fav)
                                    RecommendHelper.registerAll(genres, RankType.FAV)
                                    AchievementManager.onFavAdded(fav)
                                    doOnUI { toast("Añadido a favoritos") }
                                }
                                syncData { favs() }
                            }
                        }
                        dao.isFavLive(fav.key).observe(this, Observer { holder.setFABState(it) })
                    }
                    holder.setTitle(animeObject.name)
                    holder.loadImg(PatternUtil.getCover(animeObject.aid), View.OnClickListener {
                        startActivity(
                                Intent(this@ActivityAnime, ActivityImgFull::class.java)
                                        .setData(Uri.parse(PatternUtil.getCover(animeObject.aid)))
                                        .putExtra(keyTitle, animeObject.name), ActivityOptionsCompat.makeSceneTransitionAnimation(this@ActivityAnime, holder.imageView, "img")
                                .toBundle()
                        )
                    })
                    lifecycleScope.launch(Dispatchers.Main){
                        holder.setFABState(withContext(Dispatchers.IO) { dao.isFav(favoriteObject?.key ?: 0) })
                        holder.showFAB()
                    }
                    invalidateOptionsMenu()
                    RecommendHelper.registerAll(genres, RankType.CHECK)
                }
            } else {
                Toaster.toast("Error al cargar información del anime")
                onBackPressed()
            }
        })
    }

    private fun setResult() {
        isEdited = true
    }

    override fun onFabClicked(actionButton: FloatingActionButton) {
        lifecycleScope.launch(Dispatchers.Main) {
            setResult()
            favoriteObject?.let {
                val isFav = withContext(Dispatchers.IO) { dao.isFav(it.key) }
                if (isFav) {
                    holder.setFABState(false)
                    withContext(Dispatchers.IO) { dao.deleteFav(it) }
                    RecommendHelper.registerAll(genres, RankType.UNFAV)
                } else {
                    holder.setFABState(true)
                    withContext(Dispatchers.IO) { dao.addFav(it) }
                    RecommendHelper.registerAll(genres, RankType.FAV)
                    AchievementManager.onFavAdded(it)
                }
                syncData { favs() }
            }
        }
    }

    override fun onImgClicked(imageView: ImageView) {

    }

    override fun onBypassUpdated() {
        try {
            if (!aidOnly)
                viewModel.reload(this, intent.dataString, intent.getBooleanExtra(keyPersist, true))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getSnackbarAnchor(): View? {
        return findViewById(R.id.coordinator)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (favoriteObject != null) {
            menuInflater.inflate(R.menu.menu_anime_info, menu)
            CastUtil.registerActivity(this, menu, R.id.castMenu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> share()
        }
        return true
    }

    private fun share() {
        try {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, favoriteObject?.name + "\n" + favoriteObject?.link), "Compartir"))
            AchievementManager.onShare()
        } catch (e: ActivityNotFoundException) {
            Toaster.toast("No se encontraron aplicaciones para enviar")
        }

    }

    private fun closeActivity() {
        holder.hideFABForce()
        if (intent.getBooleanExtra(keyFromFav, false) && isEdited) {
            finish()
        } else if (intent.getBooleanExtra(keyNoTransition, false)) {
            finish()
        } else {
            supportFinishAfterTransition()
        }
    }

    override fun onBackPressed() {
        closeActivity()
    }

    companion object {
        private var REQUEST_CODE = 558
        private const val keyTitle = "title"
        private const val keyAid = "aid"
        private const val keyImg = "img"
        private const val keyPosition = "persist"
        private const val keyPersist = "persist"
        private const val keyNoTransition = "noTransition"
        private const val keyIsRecord = "isRecord"
        private const val keyFromFav = "from_fav"
        private const val keyAidOnly = "aid_only"
        private const val keyNotification = "notification"
        private const val sharedImg = "img"

        fun open(fragment: Fragment, animeObject: SearchObject, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, DesignUtils.infoClass)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, animeObject: SearchObjectFav, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, DesignUtils.infoClass)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, recentObject: RecentObject, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, DesignUtils.infoClass)
            intent.data = Uri.parse(recentObject.url)
            intent.putExtra(keyTitle, recentObject.name)
            intent.putExtra(keyAid, recentObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(recentObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, animeObject: DirObjectCompact, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, DesignUtils.infoClass)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, animeObject: DirObject, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val activity = fragment.activity ?: return
            try {
                val intent = Intent(fragment.context, DesignUtils.infoClass)
                intent.data = Uri.parse(animeObject.link)
                intent.putExtra(keyTitle, animeObject.name)
                intent.putExtra(keyAid, animeObject.aid)
                intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
                intent.putExtra(keyPersist, persist)
                intent.putExtra(keyNoTransition, !animate)
                fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun open(activity: Activity, animeObject: SearchObject, view: ImageView, persist: Boolean, animate: Boolean) {
            val intent = Intent(activity, DesignUtils.infoClass)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(activity: Activity?, animeObject: AnimeShortObject, view: ImageView, persist: Boolean, animate: Boolean) {
            val intent = Intent(activity, DesignUtils.infoClass)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            activity?.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, explorerObject: ExplorerObject, view: ImageView) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, DesignUtils.infoClass)
            intent.data = Uri.parse(explorerObject.link)
            intent.putExtra(keyTitle, explorerObject.name)
            intent.putExtra(keyAid, explorerObject.key.toString())
            intent.putExtra(keyImg, PatternUtil.getCover(explorerObject.aid))
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(activity: Activity, recordObject: RecordObject, view: ImageView) {
            val intent = Intent(activity, DesignUtils.infoClass)
            intent.data = Uri.parse(recordObject.animeObject.link)
            intent.putExtra(keyTitle, recordObject.name)
            intent.putExtra(keyAid, recordObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(recordObject.animeObject.aid))
            intent.putExtra(keyPersist, true)
            intent.putExtra(keyIsRecord, true)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(activity: Activity?, seeingObject: SeeingObject) {
            activity ?: return
            val intent = Intent(activity, DesignUtils.infoClass)
            intent.data = Uri.parse(seeingObject.link)
            intent.putExtra(keyTitle, seeingObject.title)
            //intent.putExtra(keyAid, seeingObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(seeingObject.aid))
            intent.putExtra(keyPersist, true)
            intent.putExtra(keyNoTransition, true)
            intent.putExtra(keyIsRecord, true)
            activity.startActivity(intent)
        }

        fun open(context: Context, animeObject: SearchObject) {
            val intent = Intent(context, DesignUtils.infoClass)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            context.startActivity(intent)
        }

        fun open(fragment: Fragment, favoriteObject: FavoriteObject, view: ImageView) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, DesignUtils.infoClass)
            intent.data = Uri.parse(favoriteObject.link)
            intent.putExtra(keyTitle, favoriteObject.name)
            intent.putExtra(keyAid, favoriteObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(favoriteObject.aid))
            intent.putExtra(keyFromFav, true)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(activity: Activity, queueObject: QueueObject, view: ImageView) {
            val intent = Intent(activity, DesignUtils.infoClass)
            intent.putExtra(keyTitle, queueObject.chapter.name)
            intent.putExtra(keyAid, queueObject.chapter.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(queueObject.chapter.aid))
            intent.putExtra(keyAidOnly, true)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, animeRelated: AnimeObject.WebInfo.AnimeRelated) {
            val intent = Intent(fragment.context, DesignUtils.infoClass)
            intent.data = Uri.parse("https://www3.animeflv.net/" + animeRelated.link)
            intent.putExtra(keyTitle, animeRelated.name)
            intent.putExtra(keyAid, animeRelated.aid)
            fragment.startActivityForResult(intent, REQUEST_CODE)
        }

        fun open(fragment: Fragment, animeObject: AnimeObject.WebInfo.AnimeRelated, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, DesignUtils.infoClass)
            intent.data = Uri.parse("https://www3.animeflv.net/" + animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(context: Context, url: String) {
            val intent = Intent(context, DesignUtils.infoClass)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        }

        fun getSimpleIntent(context: Context, item: WEListItem): Intent {
            val intent = Intent(context, DesignUtils.infoClass)
            intent.data = Uri.parse(item.link)
            intent.action = "${Random.nextInt(1, 9000)}"
            intent.putExtra(keyTitle, item.title)
            intent.putExtra(keyAid, item.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(item.aid))
            return intent
        }
    }
}
