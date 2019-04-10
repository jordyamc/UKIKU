package knf.kuma.slices

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.slice.Slice
import androidx.slice.SliceProvider
import androidx.slice.builders.*
import knf.kuma.Main
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PatternUtil
import knf.kuma.pojos.AnimeObject
import java.util.*

class AnimeSlice : SliceProvider() {

    /**
     * Instantiate any required objects. Return true if the provider was successfully created,
     * false otherwise.
     */

    private lateinit var launcherIcon: IconCompat

    private fun getBitmap(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    override fun onCreateSliceProvider(): Boolean {
        return try {
            context?.let { launcherIcon = IconCompat.createWithBitmap(getBitmap(Objects.requireNonNull<Drawable>(ContextCompat.getDrawable(it, R.mipmap.ic_launcher)))) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    }

    /**
     * Converts URL to content URI (i.e. content://knf.kuma.slices...)
     */
    override fun onMapIntentToUri(intent: Intent?): Uri {
        // Note: implementing this is only required if you plan on catching URL requests.
        // This is an example solution.
        var uriBuilder: Uri.Builder = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
        if (intent == null) return uriBuilder.build()
        val data = intent.data
        if (data != null && data.path != null) {
            val path = data.path?.replace("/", "")
            uriBuilder = uriBuilder.path(path)
        }
        val context = context
        if (context != null) {
            uriBuilder = uriBuilder.authority(context.packageName)
        }
        return uriBuilder.build()
    }

    /**
     * Construct the Slice and bind data if available.
     */
    override fun onBindSlice(sliceUri: Uri): Slice? {
        val context = context ?: return null
        val path = sliceUri.path
        if (path != null && path.startsWith("/anime/")) {
            when {
                AnimeLoad.QUERY != path -> {
                    AnimeLoad.QUERY = path
                    context.sendBroadcast(Intent(context, AnimeLoad::class.java))
                    return list(context, sliceUri, ListBuilder.INFINITY) {
                        row {
                            title = "Buscando animes..."
                            primaryAction = createActivityAction(null, null)
                        }
                    }
                }
                AnimeLoad.LIST.isEmpty() -> return list(context, sliceUri, ListBuilder.INFINITY) {
                    row {
                        title = "No se encontraron animes"
                        primaryAction = createActivityAction(null, path.replace("/anime/", "").trim())
                    }
                }
                else -> {
                    return list(context, sliceUri, ListBuilder.INFINITY) {
                        setAccentColor(ContextCompat.getColor(context, EAHelper.getThemeColor()))
                        val animeObjects = AnimeLoad.LIST
                        if (animeObjects.isNotEmpty()) {
                            header {
                                title = "UKIKU"
                                summary = (if (animeObjects.size < 5) "Solo " else "Más de ") + animeObjects.size + " animes encontrados"
                            }
                            animeObjects.forEach {
                                row {
                                    title = it.name
                                    subtitle = it.genresString
                                    primaryAction = createOpenAnimeAction(it)
                                    setTitleItem(it.icon, ListBuilder.SMALL_IMAGE)
                                }
                            }
                            addAction(createActivityAction(null, path.replace("/anime/", "").trim()))
                        } else {
                            row {
                                title = "No se encontraron animes"
                                primaryAction = createActivityAction(null, null)
                            }
                        }
                    }
                }
            }
        } else
            return null
    }

    private fun createActivityAction(animeObject: AnimeObject?, query: String?): SliceAction {
        return if (animeObject == null)
            if (query != null) {
                SliceAction.create(
                        PendingIntent.getActivity(
                                context, 0,
                                Intent(context, Main::class.java)
                                        .putExtra("start_position", 4)
                                        .putExtra("search_query", query), PendingIntent.FLAG_CANCEL_CURRENT
                        ),
                        AnimeLoad.searchIcon,
                        ListBuilder.ICON_IMAGE,
                        "Abrir busqueda"
                )
            } else {
                SliceAction.create(
                        PendingIntent.getActivity(
                                context, 0, Intent(context, Main::class.java), 0
                        ),
                        launcherIcon,
                        ListBuilder.SMALL_IMAGE,
                        "Abrir App"
                )
            }
        else
            SliceAction.create(
                    PendingIntent.getActivity(
                            context, animeObject.key, Intent(context, ActivityAnime::class.java)
                            .setData(Uri.parse(animeObject.link))
                            .putExtra("title", animeObject.name)
                            .putExtra("aid", animeObject.aid)
                            .putExtra("img", PatternUtil.getCover(animeObject.aid)), PendingIntent.FLAG_CANCEL_CURRENT
                    ),
                    IconCompat.createWithResource(context, R.drawable.ic_open),
                    ListBuilder.ICON_IMAGE,
                    "Abrir anime"
            )
    }

    private fun createOpenAnimeAction(animeObject: AnimeSliceObject?): SliceAction {
        return SliceAction.create(
                PendingIntent.getActivity(
                        context, animeObject?.key ?: 0, Intent(context, ActivityAnime::class.java)
                        .setData(Uri.parse(animeObject?.link ?: ""))
                        .putExtra("title", animeObject?.name ?: "")
                        .putExtra("aid", animeObject?.aid ?: "")
                        .putExtra("img", PatternUtil.getCover(animeObject?.aid
                                ?: "")), PendingIntent.FLAG_CANCEL_CURRENT
                ),
                AnimeLoad.openIcon,
                ListBuilder.ICON_IMAGE,
                "Abrir anime"
        )
    }

    /**
     * Slice has been pinned to external process. Subscribe to data source if necessary.
     */
    override fun onSlicePinned(sliceUri: Uri?) {
        // When data is received, call context.contentResolver.notifyChange(sliceUri, null) to
        // trigger AnimeSlice#onBindSlice(Uri) again.
    }

    /**
     * Unsubscribe from data source if necessary.
     */
    override fun onSliceUnpinned(sliceUri: Uri?) {
        // Remove any observers if necessary to avoid memory leaks.
    }
}
