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
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.ShareEvent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import es.munix.multidisplaycast.CastManager
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.animeinfo.img.ActivityImgFull
import knf.kuma.animeinfo.viewholders.AnimeActivityHolder
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.doOnUI
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirObject
import knf.kuma.pojos.*
import knf.kuma.recommended.AnimeShortObject
import knf.kuma.recommended.RankType
import knf.kuma.recommended.RecommendHelper
import knf.kuma.search.SearchObject
import knf.kuma.widgets.emision.WEListItem
import xdroid.toaster.Toaster
import java.util.*

class ActivityAnime : GenericActivity(), AnimeActivityHolder.Interface {
    private var isEdited = false
    private val viewModel: AnimeViewModel by lazy { ViewModelProviders.of(this).get(AnimeViewModel::class.java) }
    private val holder: AnimeActivityHolder by lazy { AnimeActivityHolder(this) }
    private var favoriteObject: FavoriteObject? = null
    private val dao = CacheDB.INSTANCE.favsDAO()
    private var chapters: MutableList<AnimeObject.WebInfo.AnimeChapter> = ArrayList()
    private var genres: MutableList<String> = ArrayList()
    private val aidOnly = intent?.getBooleanExtra(keyAidOnly, false) ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getThemeNA(this))
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
    }

    private fun load() {
        viewModel.liveData.observe(this, Observer { animeObject ->
            if (animeObject != null) {
                doOnUI {
                    Crashlytics.setString("screen", "Anime: " + animeObject.name)
                    Answers.getInstance().logContentView(ContentViewEvent().putContentName(animeObject.name).putContentType(animeObject.type).putContentId(animeObject.aid))
                    chapters = animeObject.chapters ?: mutableListOf()
                    genres = animeObject.genres ?: mutableListOf()
                    favoriteObject = FavoriteObject(animeObject)
                    holder.setTitle(animeObject.name)
                    holder.loadImg(PatternUtil.getCover(animeObject.aid), View.OnClickListener {
                        startActivity(
                                Intent(this@ActivityAnime, ActivityImgFull::class.java)
                                        .setData(Uri.parse(PatternUtil.getCover(animeObject.aid)))
                                        .putExtra(keyTitle, animeObject.name), ActivityOptionsCompat.makeSceneTransitionAnimation(this@ActivityAnime, holder.imageView, "img")
                                .toBundle()
                        )
                    })
                    holder.setFABState(dao.isFav(favoriteObject?.key ?: 0))
                    holder.showFAB()
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
        doOnUI {
            setResult()
            favoriteObject?.let {
                val isFav = dao.isFav(it.key)
                if (isFav) {
                    holder.setFABState(false)
                    dao.deleteFav(it)
                    RecommendHelper.registerAll(genres, RankType.UNFAV)
                } else {
                    holder.setFABState(true)
                    dao.addFav(it)
                    RecommendHelper.registerAll(genres, RankType.FAV)
                    AchievementManager.onFavAdded(it)
                }
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
            CastManager.getInstance().registerForActivity(this, menu, R.id.castMenu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> share()
            R.id.action_comments ->
                if (chapters.isNotEmpty())
                    CommentsDialog(chapters).show(this)
                else
                    Toaster.toast("Aun no hay episodios!")
        }
        return true
    }

    private fun share() {
        try {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, favoriteObject?.name + "\n" + favoriteObject?.link), "Compartir"))
            Answers.getInstance().logShare(ShareEvent().putContentName(favoriteObject?.name).putContentId(favoriteObject?.aid))
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

        fun open(fragment: Fragment, recentObject: RecentObject, view: ImageView, position: Int) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, ActivityAnime::class.java)
            intent.data = Uri.parse(recentObject.anime)
            intent.putExtra(keyTitle, recentObject.name)
            intent.putExtra(keyAid, recentObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(recentObject.aid))
            intent.putExtra(keyPosition, position)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        @JvmOverloads
        fun open(fragment: Fragment, animeObject: AnimeObject, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, ActivityAnime::class.java)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, animeObject: SearchObject, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, ActivityAnime::class.java)
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
                val intent = Intent(fragment.context, ActivityAnime::class.java)
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

        fun open(activity: Activity, animeObject: AnimeObject, view: ImageView, persist: Boolean, animate: Boolean) {
            val intent = Intent(activity, ActivityAnime::class.java)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(activity: Activity, animeObject: SearchObject, view: ImageView, persist: Boolean, animate: Boolean) {
            val intent = Intent(activity, ActivityAnime::class.java)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(activity: Activity, animeObject: AnimeShortObject, view: ImageView, persist: Boolean, animate: Boolean) {
            val intent = Intent(activity, ActivityAnime::class.java)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, explorerObject: ExplorerObject, view: ImageView) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, ActivityAnime::class.java)
            intent.data = Uri.parse(explorerObject.link)
            intent.putExtra(keyTitle, explorerObject.name)
            intent.putExtra(keyAid, explorerObject.key.toString())
            intent.putExtra(keyImg, PatternUtil.getCover(explorerObject.aid))
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(activity: Activity, recordObject: RecordObject, view: ImageView) {
            val intent = Intent(activity, ActivityAnime::class.java)
            intent.data = Uri.parse(recordObject.animeObject.link)
            intent.putExtra(keyTitle, recordObject.name)
            intent.putExtra(keyAid, recordObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(recordObject.animeObject.aid))
            intent.putExtra(keyPersist, true)
            intent.putExtra(keyIsRecord, true)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(activity: Activity, seeingObject: SeeingObject, view: ImageView) {
            val intent = Intent(activity, ActivityAnime::class.java)
            intent.data = Uri.parse(seeingObject.link)
            intent.putExtra(keyTitle, seeingObject.title)
            intent.putExtra(keyAid, seeingObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(seeingObject.aid))
            intent.putExtra(keyPersist, true)
            intent.putExtra(keyNoTransition, true)
            intent.putExtra(keyIsRecord, true)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(context: Context, animeObject: SearchObject) {
            val intent = Intent(context, ActivityAnime::class.java)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(animeObject.aid))
            context.startActivity(intent)
        }

        fun open(fragment: Fragment, favoriteObject: FavoriteObject, view: ImageView) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, ActivityAnime::class.java)
            intent.data = Uri.parse(favoriteObject.link)
            intent.putExtra(keyTitle, favoriteObject.name)
            intent.putExtra(keyAid, favoriteObject.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(favoriteObject.aid))
            intent.putExtra(keyFromFav, true)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(activity: Activity, queueObject: QueueObject, view: ImageView) {
            val intent = Intent(activity, ActivityAnime::class.java)
            intent.putExtra(keyTitle, queueObject.chapter.name)
            intent.putExtra(keyAid, queueObject.chapter.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(queueObject.chapter.aid))
            intent.putExtra(keyAidOnly, true)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, animeRelated: AnimeObject.WebInfo.AnimeRelated) {
            val intent = Intent(fragment.context, ActivityAnime::class.java)
            intent.data = Uri.parse("https://animeflv.net/" + animeRelated.link)
            intent.putExtra(keyTitle, animeRelated.name)
            intent.putExtra(keyAid, animeRelated.aid)
            fragment.startActivityForResult(intent, REQUEST_CODE)
        }

        fun getSimpleIntent(context: Context, item: WEListItem): Intent {
            val intent = Intent(context, ActivityAnime::class.java)
            intent.data = Uri.parse(item.link)
            intent.putExtra(keyTitle, item.title)
            intent.putExtra(keyAid, item.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(item.aid))
            return intent
        }
    }
}
