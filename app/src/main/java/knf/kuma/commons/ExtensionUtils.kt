package knf.kuma.commons

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.aesthetic.AestheticActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Callback
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.custom.snackbar.SnackProgressBar
import knf.kuma.custom.snackbar.SnackProgressBarManager
import knf.kuma.database.CacheDB
import knh.kuma.commons.cloudflarebypass.util.ConvertUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.nield.kotlinstatistics.WeightedDice
import xdroid.toaster.Toaster
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun Toolbar.changeToolbarFont(@FontRes res: Int) {
    for (i in 0 until childCount) {
        val view = getChildAt(i)
        if (view is TextView && view.text == title) {
            view.typeface = ResourcesCompat.getFont(context, res)
            break
        }
    }
}

val String.urlFixed: String get() = if (!contains("animeflv.net")) "https://animeflv.net$this" else this

val <T>LiveData<T>.distinct: LiveData<T>
    get() = Transformations.distinctUntilChanged(this)


fun getPackage(): String {
    return if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "release" || BuildConfig.BUILD_TYPE == "playstore" || BuildConfig.BUILD_TYPE == "amazon") "knf.kuma" else "knf.kuma.${BuildConfig.BUILD_TYPE}"
}

val getUpdateDir: String
    get() = when (BuildConfig.BUILD_TYPE) {
        "debug", "release" -> "release"
        else -> BuildConfig.BUILD_TYPE
    }

fun currentTime(): Long = System.currentTimeMillis()

val ffFile: File get() = File(baseDir, "data.crypt")
val admFile: File get() = File(baseDir, BuildConfig.ADM_FILE)
val baseDir: File
    get() =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            File(Environment.getExternalStorageDirectory(), "UKIKU/backups")
        else
            App.context.filesDir

fun verifiyFF() {
    try {
        if (PrefsUtil.isFamilyFriendly && !ffFile.exists()) {
            ffFile.parentFile?.mkdirs()
            ffFile.createNewFile()
            ffFile.writeText(PrefsUtil.ffPass)
        } else if (!PrefsUtil.isFamilyFriendly && ffFile.exists()) {
            PrefsUtil.isFamilyFriendly = true
            PrefsUtil.ffPass = ffFile.readText()
            CacheDB.INSTANCE.animeDAO().nukeEcchi()
        }
    } catch (e: Exception) {
        //
    }
}

fun MaterialDialog.safeShow(func: MaterialDialog.() -> Unit): MaterialDialog {
    doOnUI {
        try {
            lifecycleOwner()
            this.func()
            this@safeShow.show()
        } catch (e: Exception) {
            //
        }
    }
    return this
}

fun MaterialDialog.safeShow() {
    doOnUI {
        try {
            lifecycleOwner()
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
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

fun RecyclerView.verifyManager(size: Int = 115) {
    val manager = layoutManager
    if (manager is GridLayoutManager) {
        manager.spanCount = gridColumns(size)
        layoutManager = manager
    }
    setHasFixedSize(true)
}

fun <T> MutableList<T>.toArray(): Array<T> {
    return this.toArray()
}

val canGroupNotifications: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

fun gridColumns(size: Int = 115): Int {
    val metrics = App.context.resources.displayMetrics
    val dpWidht = metrics.widthPixels / metrics.density
    return (dpWidht / size).toInt()
}

fun View.showSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT, animation: Int = Snackbar.ANIMATION_MODE_FADE): Snackbar {
    return Snackbar.make(this, text, duration).apply {
        animationMode = animation
    }.also { doOnUI { it.show() } }
}

fun View.showSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT, button: String, onAction: (View) -> Unit): Snackbar {
    return Snackbar.make(this, text, duration).apply {
        animationMode = Snackbar.ANIMATION_MODE_SLIDE
        setAction(button, onAction)
    }.also { doOnUI { it.show() } }
}

fun SnackProgressBarManager.showProgressSnackbar(text: String, duration: Int = SnackProgressBarManager.LENGTH_SHORT) {
    val snackbar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, text)
            .setIsIndeterminate(true)
            .setProgressMax(100)
            .setShowProgressPercentage(false)
    doOnUI { show(snackbar, duration) }
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

fun <T : View> View.optionalBind(@IdRes res: Int): Lazy<T?> {
    @Suppress("UNCHECKED_CAST")
    return lazy { findViewById<T?>(res) }
}

fun <T : View> bind(activity: AppCompatActivity, @IdRes res: Int): Lazy<T> {
    @Suppress("UNCHECKED_CAST")
    return lazy { activity.findViewById<T>(res) }
}

fun <T : View> optionalBind(activity: AppCompatActivity, @IdRes res: Int): Lazy<T?> {
    @Suppress("UNCHECKED_CAST")
    return lazy { activity.findViewById<T?>(res) }
}

fun <T : View> Activity.optionalBind(@IdRes res: Int): Lazy<T?> {
    @Suppress("UNCHECKED_CAST")
    return lazy { findViewById<T?>(res) }
}

fun Request.execute(followRedirects: Boolean = true): Response {
    return OkHttpClient().newBuilder().apply {
        followRedirects(followRedirects)
        connectionSpecs(
            listOf(
                ConnectionSpec.CLEARTEXT, ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .allEnabledTlsVersions()
                    .allEnabledCipherSuites()
                    .build()
            )
        )
    }.build().newCall(this).execute()
}

fun Request.executeNoSSl(followRedirects: Boolean = true): Response {
    return NoSSLOkHttpClient.get().newBuilder().apply {
        followRedirects(followRedirects)
        connectionSpecs(
            listOf(
                ConnectionSpec.CLEARTEXT, ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                    .allEnabledTlsVersions()
                    .allEnabledCipherSuites()
                    .build()
            )
        )
    }.build().newCall(this).execute()
}

val safeContext: Context
    get() = App.context

val isTV: Boolean get() = App.context.resources.getBoolean(R.bool.isTv)

@ColorInt
fun Int.toColor(): Int {
    return ContextCompat.getColor(App.context, this)
}

fun ImageView.load(link: String?, callback: Callback? = null) {
    PicassoSingle.get().load(link).into(this, callback)
}

fun ImageView.load(uri: Uri?, callback: Callback? = null) {
    PicassoSingle.get().load(uri).into(this, callback)
}

fun <T> retry(numOfRetries: Int, block: () -> T): T {
    var throwable: Throwable? = null
    (1..numOfRetries).forEach {
        try {
            return block()
        } catch (e: Throwable) {
            throwable = e
        }
    }
    throw throwable!!
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

suspend fun noCrashSuspend(enableLog: Boolean = true, func: suspend () -> Unit): String? {
    return try {
        func()
        null
    } catch (e: Exception) {
        if (enableLog)
            e.printStackTrace()
        e.message
    }
}

fun noCrashException(enableLog: Boolean = true, func: () -> Unit): Exception? {
    return try {
        func()
        null
    } catch (e: Exception) {
        if (enableLog)
            e.printStackTrace()
        e
    }
}

fun noCrashExec(exec: () -> Unit = {}, func: () -> Unit) {
    try {
        func()
    } catch (e: Exception) {
        e.printStackTrace()
        exec()
    }
}

fun <T> noCrashLet(onCrash: () -> Unit = {}, func: () -> T): T? {
    return try {
        func()
    } catch (e: Exception) {
        e.printStackTrace()
        onCrash()
        null
    }
}

fun <T> noCrashLet(onCrash: T, func: () -> T): T {
    return try {
        func()
    } catch (e: Exception) {
        e.printStackTrace()
        onCrash
    }
}

fun <T> noCrashLetNullable(onCrash: T? = null, func: () -> T): T? {
    return try {
        func()
    } catch (e: Exception) {
        e.printStackTrace()
        onCrash
    }
}

fun String?.toast() {
    if (!this.isNullOrEmpty())
        Toaster.toast(this)
}

fun String?.toastLong() {
    if (!this.isNullOrEmpty())
        Toaster.toastLong(this)
}

fun doOnUI(enableLog: Boolean = true, onLog: (text: String) -> Unit = {}, func: suspend () -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
        noCrashSuspend(enableLog) {
            func()
        }?.also { onLog(it) }
    }
}

fun doOnUIException(enableLog: Boolean = true, onLog: (e: Exception) -> Unit = {}, func: () -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
        noCrashException(enableLog) {
            func()
        }?.also { onLog(it) }
    }
}

fun <T> List<Any>.transform(): List<T> = this.map { it as T }

fun <T> MutableList<T>.removeAll(vararg elements: Collection<T>) {
    elements.forEach {
        removeAll(it)
    }
}

infix fun <T : Any> Collection<T>?.notSameContent(collection: Collection<T>?) = !isSameContent(collection)

infix fun <T : Any> Collection<T>?.isSameContent(collection: Collection<T>?) =
        collection.let {
            this != null && it != null && this.size == it.size && this.containsAll(it)
        }

fun NotificationCompat.Builder.create(func: NotificationCompat.Builder.() -> Unit): NotificationCompat.Builder {
    this.func()
    return this
}

fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean = false, context: Context = this.context): View = LayoutInflater.from(context).inflate(layout, this, attachToRoot)

fun inflate(context: Context, @LayoutRes layout: Int, attachToRoot: Boolean = false): View = LayoutInflater.from(context).inflate(layout, null, attachToRoot)

suspend fun asyncInflate(context: Context, @LayoutRes layout: Int, attachToRoot: Boolean = false): View = withContext(Dispatchers.Main) {
    suspendCoroutine<View> {
        AsyncLayoutInflater(context).inflate(layout, null) { view, _, _ ->
            it.resume(view)
        }
    }
}

fun File.safeDelete(log: Boolean = false) {
    try {
        delete()
    } catch (e: Exception) {
        if (log)
            e.printStackTrace()
    }
}

fun RecyclerView.removeAllDecorations() {
    while (itemDecorationCount > 0)
        removeItemDecorationAt(0)
}

fun Any?.isNull(): Boolean {
    return this == null
}

fun Any?.notNull(): Boolean {
    return this != null
}

fun String.r(from: String, to: String) = replace(from, to)

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun Context.findLifecycleOwner(): LifecycleOwner? {
    var context = this
    while (context is ContextWrapper) {
        if (context is LifecycleOwner) {
            return context
        }
        context = context.baseContext
    }
    return null
}

@UiThread
fun ImageView.setAnimatedResource(@DrawableRes res: Int) {
    setImageResource(res)
    val animated = drawable as AnimationDrawable
    animated.callback = this
    animated.setVisible(true, true)
    animated.start()
}

fun FloatingActionButton.forceHide() {
    val params = layoutParams as? CoordinatorLayout.LayoutParams
    params?.behavior = null
    requestLayout()
    visibility = View.GONE
}

fun pagedConfig(size: Int): PagedList.Config = PagedList.Config.Builder()
        .setPageSize(size)
        .setEnablePlaceholders(PrefsUtil.usePlaceholders)
        .build()

fun jsoupCookies(url: String?, followRedirects: Boolean = true): Connection =
        Jsoup.connect(url)
                .cookies(BypassUtil.getMapCookie(App.context))
                .userAgent(BypassUtil.userAgent)
                .timeout(PrefsUtil.timeoutTime.toInt() * 1000)
                .followRedirects(followRedirects)

fun jsoupCookiesDir(url: String?, useCookies: Boolean): Connection =
        Jsoup.connect(url).apply {
            if (useCookies)
                if (PrefsUtil.useDefaultUserAgent && !PrefsUtil.alwaysGenerateUA) {
                    cookies(ConvertUtil.List2Map(PrefsUtil.dirCookies))
                } else {
                    cookies(BypassUtil.getMapCookie(App.context))
                }
            if (PrefsUtil.useDefaultUserAgent && !PrefsUtil.alwaysGenerateUA)
                userAgent(PrefsUtil.userAgentDir)
            else
                userAgent(PrefsUtil.userAgent)
            timeout(PrefsUtil.timeoutTime.toInt() * 1000)
            followRedirects(true)
        }

fun okHttpCookies(url: String, method: String = "GET"): Request = Request.Builder().apply {
    url(url)
    method(method, if (method == "POST") "".toRequestBody("text/plain".toMediaType()) else null)
    header("User-Agent", BypassUtil.userAgent)
    header("Cookie", BypassUtil.getStringCookie(App.context))
}.build()

fun okHttpDocument(url: String): Document = Jsoup.parse(okHttpCookies(url).execute(true).use {
    if (it.isSuccessful)
        it.body?.string()
    else
        throw IllegalStateException("Response error Url: ${it.request.url}, code: ${it.code}")
})

fun isHostValid(hostName: String): Boolean {
    if (validateAds(hostName)) return true
    return when (hostName) {
        "fex.net",
        "api.crashlytics.com",
        "e.crashlytics.com",
        "reports.crashlytics.com",
        "sdk-android.ad.smaato.net",
        "cdn.myanimelist.net",
        "myanimelist.cdn-dena.com",
        "settings.crashlytics.com",
        "somoskudasai.com",
        "animeflv.net",
        "m.animeflv.net",
        "github.com",
        "raw.githubusercontent.com",
        "cdn.animeflv.net",
        "s1.animeflv.net",
        "streamango.com",
        "ok.ru",
        "www.rapidvideo.com",
        "nuclient-verification.herokuapp.com",
        "worldvideodownload.com",
        "okvid.download",
        "www.yourupload.com" -> true
        else -> isVideoHostName(hostName)
    }.also { if (!it) Log.e("Hostname", "Not verified: $hostName") }
}

private fun validateAds(hostName: String): Boolean {
    listOf(
            "android",
            "doubleclick",
            "invitemedia.com",
            "media.admob.com",
            "gstatic",
            "google",
            "goo.gl",
            "gvtl",
            "gvt2",
            "urchin",
            "gkecnapps",
            "youtube",
            "youtu.be",
            "yt.be",
            "ytimg",
            "g.co",
            "ggpht",
            "gkecnapps",
            "appbrain",
            "apptornado",
            "startappservice",
            "criteo",
            "appcoachs"
    ).forEach { if (hostName.contains(it)) return true }
    return false
}

private fun isVideoHostName(hostName: String): Boolean {
    return when {
        hostName.contains("google.com") -> true
        hostName.contains("fex.net") ||
                hostName.contains("content-na.drive.amazonaws.com") ||
                hostName.contains("mediafire") ||
                hostName.contains("fruithosted.net") ||
                hostName.contains("mp4upload.com") ||
                hostName.contains("storage.googleapis.com") ||
                hostName.contains("playercdn.net") ||
                hostName.contains("vidcache.net") ||
                hostName.contains("fembed.com") ||
                hostName.contains("leasewebcdn.me") ||
                hostName.contains("fvs.io") -> true
        else -> false
    }
}

fun <T> diceOf(default: T? = null, mapCreator: MutableMap<T, Double>.() -> Unit): T {
    val map = mutableMapOf<T, Double>()
    mapCreator(map)
    if (default != null && map.isEmpty()) return default
    return WeightedDice(map).roll()
}

inline var View.isVisibleAnimate: Boolean
    get() = isVisible
    set(value) {
        isVisible = value
        startAnimation(AnimationUtils.loadAnimation(context, if (value) R.anim.fadein else R.anim.fadeout))
    }

fun View.onClickMenu(
        @MenuRes menu: Int, showIcons: Boolean = true,
        hideItems: () -> List<Int> = { emptyList() },
        onItemClicked: (item: MenuItem) -> Unit = {}
) = this.onClick { popUpMenu(this@onClickMenu, menu, showIcons, hideItems, onItemClicked) }

@SuppressLint("RestrictedApi")
fun popUpMenu(
        view: View,
        @MenuRes menu: Int, showIcons: Boolean = true,
        hideItems: () -> List<Int> = { emptyList() },
        onItemClicked: (item: MenuItem) -> Unit = {}
) = doOnUI {
    PopupMenu(view.context, view).apply {
        inflate(menu)
        setOnMenuItemClickListener {
            onItemClicked.invoke(it)
            true
        }
        hideItems().forEach {
            getMenu().findItem(it).isVisible = false
        }
        if (getMenu() is MenuBuilder)
            (getMenu() as MenuBuilder).setOptionalIconsVisible(showIcons)
    }.show()
}

fun AppCompatActivity.setSurfaceBars() {
    val surfaceColor = getSurfaceColor()
    window.apply {
        statusBarColor = surfaceColor
        navigationBarColor = surfaceColor
    }
}

fun AppCompatActivity.getSurfaceColor(): Int {
    return TypedValue().apply {
        theme.resolveAttribute(R.attr.colorSurface, this, true)
    }.data
}

fun Fragment.getSurfaceColor(): Int {
    return (requireActivity() as AppCompatActivity).getSurfaceColor()
}

fun String.resolveRedirection(tryCount: Int = 0): String =
    try {
        jsoupCookies(this).execute().url().toString()
    } catch (e: HttpStatusException) {
        if (tryCount >= 3)
            this
        else
            resolveRedirection(tryCount + 1)
    }

val isFullMode: Boolean get() = BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "release"

private fun isIntentResolved(ctx: Context, intent: Intent): Boolean {
    return ctx.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
}

fun isMIUI(ctx: Context): Boolean {
    return isIntentResolved(
        ctx,
        Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT)
    )
            || isIntentResolved(
        ctx,
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        )
    )
            || isIntentResolved(
        ctx, Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").addCategory(
            Intent.CATEGORY_DEFAULT
        )
    )
            || isIntentResolved(
        ctx,
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.powercenter.PowerSettings"
            )
        )
    )
}