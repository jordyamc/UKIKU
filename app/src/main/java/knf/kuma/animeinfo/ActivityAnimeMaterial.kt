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
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.showRandomInterstitial
import knf.kuma.animeinfo.img.ActivityImgFull
import knf.kuma.animeinfo.viewholders.AnimeActivityMaterialHolder
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.CastUtil
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.doOnUI
import knf.kuma.commons.noCrash
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.ExplorerObject
import knf.kuma.pojos.NotificationObj
import knf.kuma.pojos.QueueObject
import knf.kuma.pojos.SeeingObject
import knf.kuma.pojos.av1.Chapter
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.pojos.av1.FavoriteAV1
import knf.kuma.pojos.av1.Genre
import knf.kuma.pojos.av1.Recommended
import knf.kuma.pojos.av1.Record
import knf.kuma.pojos.av1.Relation
import knf.kuma.recommended.RankType
import knf.kuma.recommended.RecommendHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.sdk27.coroutines.onLongClick
import org.jetbrains.anko.toast
import xdroid.toaster.Toaster

class ActivityAnimeMaterial : GenericActivity(), AnimeActivityMaterialHolder.Interface {
    private var isEdited = false
    private val viewModel: AnimeViewModel by viewModels()
    private val holder: AnimeActivityMaterialHolder by lazy { AnimeActivityMaterialHolder(this) }
    private var favoriteObject: FavoriteAV1? = null
    private val dao = CacheDB.INSTANCE.favoriteAV1DAO()
    private var chapters: List<Chapter> = ArrayList()
    private var genres: List<Genre> = ArrayList()
    private val aidOnly get() = intent?.getBooleanExtra(keyAidOnly, false) ?: false
    private val isMaterial get() = intent?.getBooleanExtra(keyMaterial, true) ?: true

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getThemeNA())
        super.onCreate(savedInstanceState)
        try {
            setContentView(if (isMaterial) R.layout.activity_anime_info_material else R.layout.activity_anime_info)
        } catch (e: InflateException) {
            setContentView(R.layout.activity_anime_info_nwv)
        }
        setSupportActionBar(holder.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        holder.toolbar.setNavigationOnClickListener { closeActivity() }
        if (aidOnly)
            viewModel.init(intent.getStringExtra(keyAid)?.toInt()?: -1)
        else
            viewModel.init(intent.dataString, intent.getBooleanExtra(keyPersist, true))
        if (intent.getBooleanExtra(keyNotification, false))
            sendBroadcast(NotificationObj.fromIntent(intent).getBroadcast(this@ActivityAnimeMaterial))
        onBackPressedDispatcher.addCallback(this) { closeActivity() }
        load()
        checkBypass()
        showRandomInterstitial(this)
    }

    private fun load() {
        lifecycleScope.launch {
            viewModel.infoFlow.drop(1).collectLatest { animeObject ->
                if (animeObject != null) {
                    doOnUI {
                        chapters = animeObject.chapters
                        genres = animeObject.genres
                        favoriteObject = animeObject.asFavorite().apply {
                            holder.imageView.onLongClick(returnValue = true) {
                                launch(Dispatchers.IO) {
                                    val isFav = dao.isFav(aid)
                                    if (isFav) {
                                        withContext(Dispatchers.Main) { holder.setFABState(false) }
                                        dao.delete(this@apply)
                                        RecommendHelper.registerAll(genres, RankType.UNFAV)
                                        doOnUI { toast("Removido de favoritos") }
                                    } else {
                                        withContext(Dispatchers.Main) { holder.setFABState(true) }
                                        dao.addFav(this@apply)
                                        RecommendHelper.registerAll(genres, RankType.FAV)
                                        AchievementManager.onFavAdded(genres)
                                        doOnUI { toast("Añadido a favoritos") }
                                    }
                                    syncData { favs() }
                                }
                            }
                            launch {
                                dao.isFavFlow(aid).collectLatest {
                                    holder.setFABState(it)
                                }
                            }
                        }
                        holder.setTitle(animeObject.name)
                        holder.loadImg(animeObject.imageUrl) {
                            startActivity(
                                Intent(this@ActivityAnimeMaterial, ActivityImgFull::class.java)
                                    .setData(animeObject.imageUrl.toUri())
                                    .putExtra(keyTitle, animeObject.name),
                                ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    this@ActivityAnimeMaterial,
                                    holder.imageView,
                                    "img"
                                ).toBundle()
                            )
                        }
                        lifecycleScope.launch(Dispatchers.Main) {
                            holder.setFABState(withContext(Dispatchers.IO) { dao.isFav(favoriteObject?.aid ?: 0) })
                            holder.showFAB()
                        }
                        invalidateOptionsMenu()
                        RecommendHelper.registerAll(genres, RankType.CHECK)
                    }
                } else {
                    Toaster.toast("Error al cargar información del anime")
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    private fun setResult() {
        isEdited = true
    }

    override fun onFabClicked(actionButton: FloatingActionButton) {
        lifecycleScope.launch(Dispatchers.Main) {
            setResult()
            favoriteObject?.let {
                val isFav = withContext(Dispatchers.IO) { dao.isFav(it.aid) }
                if (isFav) {
                    holder.setFABState(false)
                    withContext(Dispatchers.IO) { dao.delete(it) }
                    RecommendHelper.registerAll(genres, RankType.UNFAV)
                } else {
                    holder.setFABState(true)
                    withContext(Dispatchers.IO) { dao.addFav(it) }
                    RecommendHelper.registerAll(genres, RankType.FAV)
                    AchievementManager.onFavAdded(genres)
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
                viewModel.reload(intent.dataString, intent.getBooleanExtra(keyPersist, true))
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
                    .putExtra(Intent.EXTRA_TEXT, favoriteObject?.name + "\n" + favoriteObject?.animeUrl()), "Compartir"))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        noCrash {
            if (requestCode == FileAccessHelper.SD_REQUEST && resultCode == RESULT_OK) {
                val validation = FileAccessHelper.isUriValid(data?.data)
                if (!validation.isValid) {
                    Toaster.toast("Directorio invalido: $validation")
                    FileAccessHelper.openTreeChooser(this)
                }
            }
        }
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
        const val keyMaterial = "material"
        private const val keyNotification = "notification"
        private const val sharedImg = "img"

        @JvmOverloads
        fun open(fragment: Fragment, animeObject: Recommended, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, DesignUtils.infoClass)
            intent.data = animeObject.animeUrl.toUri()
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, animeObject.imageUrl)
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: FragmentActivity, animeObject: DirectoryAV1Min, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            if (fragment.isDestroyed) return
            val intent = Intent(fragment, DesignUtils.infoClass)
            intent.data = animeObject.animeUrl.toUri()
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, animeObject.imageUrl)
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(fragment, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, animeObject: Relation, view: ImageView, persist: Boolean = true, animate: Boolean = true) {
            val activity = fragment.activity ?: return
            val intent = Intent(fragment.context, DesignUtils.infoClass)
            intent.data = animeObject.animeUrl.toUri()
            intent.putExtra(keyTitle, animeObject.name)
            intent.putExtra(keyAid, animeObject.aid)
            intent.putExtra(keyImg, animeObject.imageUrl)
            intent.putExtra(keyPersist, persist)
            intent.putExtra(keyNoTransition, !animate)
            fragment.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(fragment: Fragment, dirObject: DirectoryAV1Min, persist: Boolean = false) {
            fragment.startActivity(Intent(fragment.requireContext(),DesignUtils.infoClass).apply {
                data = dirObject.animeUrl.toUri()
                putExtra(keyTitle, dirObject.name)
                putExtra(keyImg, dirObject.imageUrl)
                putExtra(keyPersist, persist)
            })
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

        fun open(activity: Activity, record: Record, view: ImageView) {
            val intent = Intent(activity, DesignUtils.infoClass)
            intent.data = record.animeUrl().toUri()
            intent.putExtra(keyTitle, record.name)
            intent.putExtra(keyAid, record.aid)
            intent.putExtra(keyImg, record.imageUrl())
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

        fun open(activity: Activity, queueObject: QueueObject, view: ImageView) {
            val intent = Intent(activity, DesignUtils.infoClass)
            intent.putExtra(keyTitle, queueObject.chapter.name)
            intent.putExtra(keyAid, queueObject.chapter.aid)
            intent.putExtra(keyImg, PatternUtil.getCover(queueObject.chapter.aid))
            intent.putExtra(keyAidOnly, true)
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, sharedImg).toBundle())
        }

        fun open(context: Context, url: String) {
            val intent = Intent(context, DesignUtils.infoClass)
            intent.data = url.toUri()
            context.startActivity(intent)
        }
    }
}
