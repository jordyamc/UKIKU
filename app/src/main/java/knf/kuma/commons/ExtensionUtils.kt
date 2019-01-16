package knf.kuma.commons

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.AnimationDrawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.aesthetic.AestheticActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.animeinfo.viewholders.AnimeActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import xdroid.toaster.Toaster
import java.io.File

fun Toolbar.changeToolbarFont() {
    for (i in 0 until childCount) {
        val view = getChildAt(i)
        if (view is TextView && view.text == title) {
            view.typeface = ResourcesCompat.getFont(context, R.font.audiowide)
            break
        }
    }
}

fun getPackage(): String {
    return if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "release" || BuildConfig.BUILD_TYPE == "playstore") "knf.kuma" else "knf.kuma.${BuildConfig.BUILD_TYPE}"
}

val getUpdateDir: String
    get() = when (BuildConfig.BUILD_TYPE) {
        "debug", "release" -> "release"
        else -> BuildConfig.BUILD_TYPE
    }

fun MaterialDialog.safeShow(func: MaterialDialog.() -> Unit): MaterialDialog {
    try {
        this.func()
        doOnUI {
            try {
                this@safeShow.show()
            } catch (e: Exception) {
                //
            }
        }
    } catch (exception: Exception) {
        //
    }
    return this
}

fun MaterialDialog.safeShow() {
    doOnUI {
        try {
            this@safeShow.show()
        } catch (e: Exception) {
            //
        }
    }
}

fun MaterialDialog.safeDismiss() {
    try {
        dismiss()
    } catch (exception: Exception) {
        //
    }
}

fun Snackbar.safeDismiss() {
    try {
        dismiss()
    } finally {
        //
    }
}

operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

fun RecyclerView.verifyManager(size: Int = 115) {
    val manager = layoutManager
    if (manager is GridLayoutManager) {
        manager.spanCount = gridColumns(size)
        layoutManager = manager
    }
}

fun gridColumns(size: Int = 115): Int {
    val metrics = App.context.resources.displayMetrics
    val dpWidht = metrics.widthPixels / metrics.density
    return (dpWidht / size).toInt()
}

fun View.showSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
    return Snackbar.make(this, text, duration).also { doOnUI { it.show() } }
}

fun View.createSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
    return Snackbar.make(this, text, duration)
}

val Int.asPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun AestheticActivity.setDefaults() {
    AestheticUtils.setDefaults(applicationContext)
}

fun <T : View> Activity.bind(@IdRes res: Int): Lazy<T> {
    @Suppress("UNCHECKED_CAST")
    return lazy { findViewById<T>(res) }
}

fun <T : View> View.bind(@IdRes res: Int): Lazy<T> {
    @Suppress("UNCHECKED_CAST")
    return lazy { findViewById<T>(res) }
}

fun <T : View> AnimeActivityHolder.bind(activity: AppCompatActivity, @IdRes res: Int): Lazy<T> {
    @Suppress("UNCHECKED_CAST")
    return lazy { activity.findViewById<T>(res) }
}

fun <T : View> AnimeActivityHolder.optionalBind(activity: AppCompatActivity, @IdRes res: Int): Lazy<T?> {
    @Suppress("UNCHECKED_CAST")
    return lazy { activity.findViewById<T?>(res) }
}

fun <T : View> Activity.optionalBind(@IdRes res: Int): Lazy<T?> {
    @Suppress("UNCHECKED_CAST")
    return lazy { findViewById<T?>(res) }
}

fun Request.execute(): Response {
    return OkHttpClient().newBuilder().build().newCall(this).execute()
}

val PreferenceFragmentCompat.safeContext: Context
    get() = context!!.applicationContext

val isTV: Boolean get() = App.context.resources.getBoolean(R.bool.isTv)

@ColorInt
fun Int.toColor(): Int {
    return ContextCompat.getColor(App.context, this)
}

fun noCrash(enableLog: Boolean = true, func: () -> Unit): String? {
    return try {
        func()
        null
    } catch (e: Exception) {
        if (enableLog)
            e.printStackTrace()
        e.message
    }
}

fun String?.toast() {
    if (!this.isNullOrEmpty())
        Toaster.toast(this)
}

fun doOnUI(func: () -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
        noCrash(true) {
            func()
        }
    }
}

fun <T> MutableList<T>.removeAll(vararg elements: Collection<T>) {
    elements.forEach {
        removeAll(it)
    }
}

infix fun <T> Collection<T>?.notSameContent(collection: Collection<T>?) =
        collection.let {
            !(this != null && it != null && this.size == it.size && this.containsAll<T>(it))
        }

infix fun <T> Collection<T>?.isSameContent(collection: Collection<T>?) =
        collection.let {
            this != null && it != null && this.size == it.size && this.containsAll<T>(it)
        }

fun NotificationCompat.Builder.create(func: NotificationCompat.Builder.() -> Unit): NotificationCompat.Builder {
    this.func()
    return this
}

fun File.safeDelete(log: Boolean = false) {
    try {
        delete()
    } catch (e: Exception) {
        if (log)
            e.printStackTrace()
    }
}

fun Any?.isNull(): Boolean {
    return this == null
}

fun Any?.notNull(): Boolean {
    return this != null
}

@UiThread
fun ImageView.setAnimatedResource(@DrawableRes res: Int) {
    setImageResource(res)
    val animated = drawable as AnimationDrawable
    animated.callback = this
    animated.setVisible(true, true)
    animated.start()
}

fun isHostValid(hostName: String): Boolean {
    if (BuildConfig.DEBUG)
        Log.e("Hostname", hostName)
    return when (hostName) {
        "fex.net",
        "api.crashlytics.com",
        "e.crashlytics.com",
        "cdn.myanimelist.net",
        "settings.crashlytics.com",
        "somoskudasai.com",
        "animeflv.net",
        "github.com",
        "raw.githubusercontent.com",
        "cdn.animeflv.net",
        "m.animeflv.net",
        "s1.animeflv.net",
        "streamango.com",
        "ok.ru",
        "www.rapidvideo.com",
        "www.yourupload.com" -> true
        else -> isVideoHostName(hostName)
    }
}

private fun isVideoHostName(hostName: String): Boolean {
    return when {
        hostName.contains("google.com") -> true
        hostName.contains("fex.net") ||
                hostName.contains("content-na.drive.amazonaws.com") ||
                hostName.contains("mediafire") ||
                hostName.contains("black-raspberry.fruithosted.net") ||
                hostName.contains("mp4upload.com") ||
                hostName.contains("storage.googleapis.com") ||
                hostName.contains("safety.playercdn.net") ||
                hostName.contains("vidcache.net") -> true
        else -> false
    }
}