package knf.kuma.commons

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import es.munix.multidisplaycast.CastManager
import es.munix.multidisplaycast.interfaces.CastListener
import es.munix.multidisplaycast.interfaces.PlayStatusListener
import knf.kuma.App
import knf.kuma.achievements.AchievementManager
import knf.kuma.cast.CastCustom
import knf.kuma.cast.CastMedia
import knf.kuma.custom.ThemedControlsActivity
import org.jetbrains.annotations.Contract
import xdroid.toaster.Toaster

class CastUtil private constructor(private val context: Context) : CastListener, PlayStatusListener {
    val casting = MutableLiveData<String>()

    private var loading: Snackbar? = null

    init {
        CastManager.setInstance(CastCustom())
        CastManager.getInstance().setDiscoveryManager()
        CastManager.getInstance().setPlayStatusListener(javaClass.simpleName, this)
        CastManager.getInstance().setCastListener(javaClass.simpleName, this)
        casting.value = NO_PLAYING
    }

    fun registerActivity(activity: Activity, menu: Menu, menuId: Int) = CastManager.getInstance().registerForActivity(activity, menu, menuId)

    fun connected(): Boolean {
        return CastManager.getInstance().isConnected
        //return isConnected;
    }

    fun play(view: View, castMedia: CastMedia?) {
        try {
            if (castMedia == null) throw IllegalStateException("CastMedia must not be null")
            if (connected()) {
                if (!castMedia.url.endsWith(":" + SelfServer.HTTP_PORT))
                    SelfServer.stop()
                startLoading(view)
                setEid(castMedia.eid)
                Log.e("Cast", castMedia.url)
                CastManager.getInstance().playMedia(castMedia.url, "video/mp4", castMedia.title, castMedia.subTitle, castMedia.image)
                AchievementManager.unlock(listOf(6))
            } else {
                Toaster.toast("No hay dispositivo seleccionado")
            }
        } catch (e: Exception) {
            stopLoading()
            Toaster.toast("Error al reproducir")
        }
    }

    fun stop() {
        CastManager.getInstance().stop()
    }

    fun onDestroy() {
        loading?.safeDismiss()
        loading = null
        CastManager.getInstance().onDestroy()
    }

    override fun isConnected() {
    }

    override fun isDisconnected() {
        stopLoading()
        setEid(NO_PLAYING)
        SelfServer.stop()
    }

    private fun getLoading(view: View): Snackbar {
        return view.showSnackbar("Cargando...", duration = Snackbar.LENGTH_INDEFINITE)
    }

    private fun startLoading(view: View) {
        doOnUI {
            loading = getLoading(view)
        }
    }

    private fun stopLoading() {
        doOnUI {
            loading?.safeDismiss()
            loading = null
        }
    }

    private fun setEid(eid: String) {
        doOnUI {
            casting.setValue(eid)
        }
    }

    fun openControls() {
        context.startActivity(Intent(context, ThemedControlsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onPlayStatusChanged(playStatus: Int) {
        when (playStatus) {
            PlayStatusListener.STATUS_START_PLAYING -> {
                stopLoading()
                openControls()
            }
            PlayStatusListener.STATUS_FINISHED, PlayStatusListener.STATUS_STOPPED -> {
                stopLoading()
                setEid(NO_PLAYING)
            }
            PlayStatusListener.STATUS_NOT_SUPPORT_LISTENER -> {
                stopLoading()
                setEid(NO_PLAYING)
                Toaster.toast("Video no soportado por dispositivo")
            }
        }
    }

    override fun onPositionChanged(currentPosition: Long) {

    }

    override fun onTotalDurationObtained(totalDuration: Long) {

    }

    override fun onSuccessSeek() {

    }

    companion object {
        var NO_PLAYING = "no_play"
        private val ourInstance: CastUtil by lazy { CastUtil(App.context) }

        @Contract(pure = true)
        fun get(): CastUtil {
            return ourInstance
        }

        fun registerActivity(activity: Activity, menu: Menu, menuId: Int) = get().registerActivity(activity, menu, menuId)
    }
}
