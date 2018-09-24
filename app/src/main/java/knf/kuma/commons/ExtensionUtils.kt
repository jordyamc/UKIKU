package knf.kuma.commons

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.aesthetic.AestheticActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.animeinfo.viewholders.AnimeActivityHolder
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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
    return if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "release") "knf.kuma" else "knf.kuma.${BuildConfig.BUILD_TYPE}"
}

val getUpdateDir: String
    get() = when (BuildConfig.BUILD_TYPE) {
        "debug", "release" -> "release"
        else -> BuildConfig.BUILD_TYPE
    }

fun MaterialDialog.safeShow(func: MaterialDialog.() -> Unit): MaterialDialog {
    try {
        this.func()
        launch(UI) {
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
    launch(UI) {
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

fun View.showSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
    return Snackbar.make(this, text, duration).also { launch(UI) { it.show() } }
}

fun View.createSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
    return Snackbar.make(this, text, duration)
}

fun View.createIndeterminateSnackbar(text: String, show: Boolean = true): Snackbar {
    val snackbar = Snackbar.make(this, text, Snackbar.LENGTH_INDEFINITE)
    val progressBar = ProgressBar(context).also {
        it.setPadding(10.asPx, 10.asPx, 10.asPx, 10.asPx)
        it.isIndeterminate = true
    }
    (snackbar.view.findViewById<View>(com.google.android.material.R.id.snackbar_text).parent as ViewGroup).addView(progressBar, 0)
    if (show) launch(UI) { snackbar.show() }
    return snackbar
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

@ColorInt
fun Int.resolveColor(context: Context): Int {
    return ContextCompat.getColor(context, this)
}

fun noCrash(enableLog: Boolean = true, func: () -> Unit) {
    try {
        func()
    } catch (e: Exception) {
        if (enableLog)
            e.printStackTrace()
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