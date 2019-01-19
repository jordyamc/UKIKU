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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.ShareEvent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import es.munix.multidisplaycast.CastManager
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.animeinfo.viewholders.AnimeActivityHolder
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.doOnUI
import knf.kuma.commons.safeShow
import knf.kuma.database.CacheDB
import knf.kuma.pojos.*
import knf.kuma.recommended.RankType
import knf.kuma.recommended.RecommendHelper
import knf.kuma.widgets.emision.WEListItem
import xdroid.toaster.Toaster
import java.util.*

class ActivityAnime : AppCompatActivity(), AnimeActivityHolder.Interface {
    private var isEdited = false
    private var viewModel: AnimeViewModel? = null
    private var holder: AnimeActivityHolder? = null
    private var favoriteObject: FavoriteObject? = null
    private val dao = CacheDB.INSTANCE.favsDAO()
    private val seeingDAO = CacheDB.INSTANCE.seeingDAO()
    private var chapters: MutableList<AnimeObject.WebInfo.AnimeChapter> = ArrayList()
    private var genres: MutableList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getThemeNA(this))
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_anime_info)
        } catch (e: InflateException) {
            setContentView(R.layout.activity_anime_info_nwv)
        }
        viewModel = ViewModelProviders.of(this@ActivityAnime).get(AnimeViewModel::class.java)
        if (intent.getBooleanExtra("aid_only", false))
            viewModel?.init(intent.getStringExtra("aid"))
        else
            viewModel?.init(this@ActivityAnime, intent.dataString, intent.getBooleanExtra("persist", true))
        holder = AnimeActivityHolder(this@ActivityAnime)
        doOnUI {
            if (intent.getBooleanExtra("notification", false))
                sendBroadcast(NotificationObj.fromIntent(intent).getBroadcast(this@ActivityAnime))
            setSupportActionBar(holder?.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(false)
            holder?.toolbar?.setNavigationOnClickListener { closeActivity() }
            load()
        }
    }

    private fun load() {
        viewModel?.liveData?.observe(this, Observer { animeObject ->
            doOnUI {
                if (animeObject != null) {
                    Crashlytics.setString("screen", "Anime: " + animeObject.name)
                    Answers.getInstance().logContentView(ContentViewEvent().putContentName(animeObject.name).putContentType(animeObject.type).putContentId(animeObject.aid))
                    chapters = animeObject.chapters!!
                    genres = animeObject.genres!!
                    favoriteObject = FavoriteObject(animeObject)
                    holder?.setTitle(animeObject.name!!)
                    holder?.loadImg(PatternUtil.getCover(animeObject.aid!!), View.OnClickListener {
                        startActivity(
                                Intent(this@ActivityAnime, ActivityImgFull::class.java)
                                        .setData(Uri.parse(PatternUtil.getCover(animeObject.aid!!)))
                                        .putExtra("title", animeObject.name), ActivityOptionsCompat.makeSceneTransitionAnimation(this@ActivityAnime, holder!!.imageView, "img")
                                .toBundle()
                        )
                    })
                    holder?.setFABState(dao.isFav(favoriteObject!!.key)/*, seeingDAO.getByAid(favoriteObject!!.aid!!) != null*/)
                    holder?.showFAB()
                    invalidateOptionsMenu()
                    RecommendHelper.registerAll(genres, RankType.CHECK)
                } else {
                    Toaster.toast("Error al cargar informacion del anime")
                    onBackPressed()
                }
            }
        })
    }

    private fun setResult() {
        isEdited = true
    }

    override fun onFabClicked(actionButton: FloatingActionButton) {
        doOnUI {
            setResult()
            val isfav = dao.isFav(favoriteObject!!.key)
            val isSeeing = /*seeingDAO.getByAid(favoriteObject!!.aid!!) != null*/false
            if (isfav && isSeeing)
                onFabLongClicked(actionButton)
            else if (isfav) {
                holder?.setFABState(false)
                dao.deleteFav(favoriteObject!!)
                RecommendHelper.registerAll(genres, RankType.UNFAV)
            } else if (isSeeing) {
                onFabLongClicked(actionButton)
            } else {
                holder?.setFABState(true)
                dao.addFav(favoriteObject!!)
                RecommendHelper.registerAll(genres, RankType.FAV)
                AchievementManager.onFavAdded(favoriteObject!!)
            }
        }
    }

    override fun onFabLongClicked(actionButton: FloatingActionButton) {
        try {
            val seeingObject = seeingDAO.getByAid(favoriteObject!!.aid!!)
            val isfav = dao.isFav(favoriteObject!!.key)
            val isSeeing = seeingObject != null
            if (isSeeing) {
                MaterialDialog(this).safeShow {
                    listItems(items = listOf("Convertir en favorito", "Quitar favorito", "Dropear")) { _, index, _ ->
                        when (index) {
                            0 -> {
                                dao.addFav(favoriteObject!!)
                                seeingDAO.remove(seeingObject!!)
                                holder?.setFABState(true)
                                RecommendHelper.registerAll(genres, RankType.FAV)
                            }
                            1 -> {
                                dao.deleteFav(favoriteObject!!)
                                holder?.setFABSeeing()
                                RecommendHelper.registerAll(genres, RankType.UNFAV)
                            }
                            2 -> {
                                seeingDAO.remove(seeingObject!!)
                                holder?.setFABState(isfav)
                                RecommendHelper.registerAll(genres, RankType.UNFOLLOW)
                            }
                        }
                    }
                }
            } else if (!isfav) {
                MaterialDialog(this).safeShow {
                    listItems(items = listOf("Seguir", "Seguir y favorito")) { _, index, _ ->
                        when (index) {
                            0 -> {
                                seeingDAO.add(SeeingObject.fromAnime(favoriteObject!!))
                                holder?.setFABSeeing()
                                RecommendHelper.registerAll(genres, RankType.FOLLOW)
                                Toaster.toast("Agregado a animes seguidos")
                            }
                            1 -> {
                                dao.addFav(favoriteObject!!)
                                seeingDAO.add(SeeingObject.fromAnime(favoriteObject!!))
                                holder?.setFABState(true, true)
                                RecommendHelper.registerAll(genres, RankType.FAV)
                                RecommendHelper.registerAll(genres, RankType.FOLLOW)
                                Toaster.toast("Agregado a animes seguidos y favoritos")
                            }
                        }
                    }
                }
            } else {
                seeingDAO.add(SeeingObject.fromAnime(favoriteObject!!))
                holder?.setFABState(true, true)
                RecommendHelper.registerAll(genres, RankType.FOLLOW)
                Toaster.toast("Agregado a animes seguidos")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onImgClicked(imageView: ImageView) {

    }

    override fun onNeedRecreate() {
        try {
            if (!intent.getBooleanExtra("aid_only", false)) {
                viewModel?.reload(this, intent.dataString!!, intent.getBooleanExtra("persist", true))
                load()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
                    .putExtra(Intent.EXTRA_TEXT, favoriteObject!!.name + "\n" + favoriteObject!!.link), "Compartir"))
            Answers.getInstance().logShare(ShareEvent().putContentName(favoriteObject!!.name).putContentId(favoriteObject!!.aid))
            AchievementManager.onShare()
        } catch (e: ActivityNotFoundException) {
            Toaster.toast("No se encontraron aplicaciones para enviar")
        }

    }

    private fun closeActivity() {
        holder?.hideFABForce()
        if (intent.getBooleanExtra("from_fav", false) && isEdited) {
            finish()
        } else if (intent.getBooleanExtra("noTransition", false)) {
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

        fun open(fragment: Fragment, recentObject: RecentObject, view: ImageView, position: Int) {
            val intent = Intent(fragment.context, ActivityAnime::class.java)
            intent.data = Uri.parse(recentObject.anime)
            intent.putExtra("title", recentObject.name)
            intent.putExtra("aid", recentObject.aid)
            intent.putExtra("img", PatternUtil.getCover(recentObject.aid!!))
            intent.putExtra("position", position)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.activity!!, view, "img").toBundle())
        }

        @JvmOverloads
        fun open(fragment: Fragment, animeObject: AnimeObject, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val intent = Intent(fragment.context, ActivityAnime::class.java)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra("title", animeObject.name)
            intent.putExtra("aid", animeObject.aid)
            intent.putExtra("img", PatternUtil.getCover(animeObject.aid!!))
            intent.putExtra("persist", persist)
            intent.putExtra("noTransition", !animate)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.activity!!, view, "img").toBundle())
        }

        fun open(activity: Activity, animeObject: AnimeObject, view: ImageView, persist: Boolean, animate: Boolean) {
            val intent = Intent(activity, ActivityAnime::class.java)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra("title", animeObject.name)
            intent.putExtra("aid", animeObject.aid)
            intent.putExtra("img", PatternUtil.getCover(animeObject.aid!!))
            intent.putExtra("persist", persist)
            intent.putExtra("noTransition", !animate)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "img").toBundle())
        }

        fun open(fragment: Fragment, explorerObject: ExplorerObject, view: ImageView) {
            val intent = Intent(fragment.context, ActivityAnime::class.java)
            intent.data = Uri.parse(explorerObject.link)
            intent.putExtra("title", explorerObject.name)
            intent.putExtra("aid", explorerObject.key.toString())
            intent.putExtra("img", PatternUtil.getCover(explorerObject.aid!!))
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.activity!!, view, "img").toBundle())
        }

        fun open(activity: Activity, recordObject: RecordObject, view: ImageView) {
            val intent = Intent(activity, ActivityAnime::class.java)
            intent.data = Uri.parse(recordObject.animeObject!!.link)
            intent.putExtra("title", recordObject.name)
            intent.putExtra("aid", recordObject.aid)
            intent.putExtra("img", PatternUtil.getCover(recordObject.animeObject.let { it!!.aid }!!))
            intent.putExtra("persist", true)
            intent.putExtra("isRecord", true)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "img").toBundle())
        }

        fun open(activity: Activity, seeingObject: SeeingObject, view: ImageView) {
            val intent = Intent(activity, ActivityAnime::class.java)
            intent.data = Uri.parse(seeingObject.link)
            intent.putExtra("title", seeingObject.title)
            intent.putExtra("aid", seeingObject.aid)
            intent.putExtra("img", PatternUtil.getCover(seeingObject.aid!!))
            intent.putExtra("persist", true)
            intent.putExtra("noTransition", true)
            intent.putExtra("isRecord", true)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "img").toBundle())
        }

        fun open(context: Context, animeObject: AnimeObject) {
            val intent = Intent(context, ActivityAnime::class.java)
            intent.data = Uri.parse(animeObject.link)
            intent.putExtra("title", animeObject.name)
            intent.putExtra("aid", animeObject.aid)
            intent.putExtra("img", PatternUtil.getCover(animeObject.aid!!))
            context.startActivity(intent)
        }

        fun open(fragment: Fragment, favoriteObject: FavoriteObject, view: ImageView) {
            val intent = Intent(fragment.context, ActivityAnime::class.java)
            intent.data = Uri.parse(favoriteObject.link)
            intent.putExtra("title", favoriteObject.name)
            intent.putExtra("aid", favoriteObject.aid)
            intent.putExtra("img", PatternUtil.getCover(favoriteObject.aid!!))
            intent.putExtra("from_fav", true)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.activity!!, view, "img").toBundle())
        }

        fun open(activity: Activity, queueObject: QueueObject, view: ImageView) {
            val intent = Intent(activity, ActivityAnime::class.java)
            intent.putExtra("title", queueObject.chapter.name)
            intent.putExtra("aid", queueObject.chapter.aid)
            intent.putExtra("img", PatternUtil.getCover(queueObject.chapter.aid))
            intent.putExtra("aid_only", true)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "img").toBundle())
        }

        fun open(fragment: Fragment, animeRelated: AnimeObject.WebInfo.AnimeRelated) {
            val intent = Intent(fragment.context, ActivityAnime::class.java)
            intent.data = Uri.parse("https://animeflv.net/" + animeRelated.link!!)
            intent.putExtra("title", animeRelated.name)
            intent.putExtra("aid", animeRelated.aid)
            fragment.startActivityForResult(intent, REQUEST_CODE)
        }

        fun getSimpleIntent(context: Context, item: WEListItem): Intent {
            val intent = Intent(context, ActivityAnime::class.java)
            intent.data = Uri.parse(item.link)
            intent.putExtra("title", item.title)
            intent.putExtra("aid", item.aid)
            intent.putExtra("img", PatternUtil.getCover(item.aid))
            return intent
        }
    }
}
