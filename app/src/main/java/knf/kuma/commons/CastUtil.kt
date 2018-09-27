package knf.kuma.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import es.munix.multidisplaycast.CastManager
import es.munix.multidisplaycast.interfaces.CastListener
import es.munix.multidisplaycast.interfaces.PlayStatusListener
import knf.kuma.custom.ThemedControlsActivity
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.annotations.Contract
import xdroid.toaster.Toaster

class CastUtil private constructor(private val context: Context) : CastListener, PlayStatusListener {
    val casting = MutableLiveData<String>()

    private var loading: Snackbar? = null

    init {
        CastManager.getInstance().setDiscoveryManager()
        CastManager.getInstance().setPlayStatusListener(javaClass.simpleName, this)
        CastManager.getInstance().setCastListener(javaClass.simpleName, this)
        casting.value = NO_PLAYING
    }

    fun connected(): Boolean {
        return CastManager.getInstance().isConnected!!
        //return isConnected;
    }

    fun play(context: Context?, view: View, eid: String, url: String?, title: String, chapter: String, preview: String, isAid: Boolean) {
        var fUrl = url
        var fPreview = preview
        try {
            if (connected()) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("force_local_cast", false) && !url!!.endsWith(":" + SelfServer.HTTP_PORT))
                    fUrl = SelfServer.start(url, false)
                if (!url!!.endsWith(":" + SelfServer.HTTP_PORT))
                    SelfServer.stop()
                Log.e("Cast", fUrl)
                startLoading(view)
                setEid(eid)
                if (isAid)
                    fPreview = "https://animeflv.net/uploads/animes/thumbs/$preview.jpg"
                CastManager.getInstance().playMedia(fUrl, "video/mp4", title, chapter, fPreview)
            } else {
                Toaster.toast("No hay dispositivo seleccionado")
            }
        } catch (e: Exception) {
            stopLoading()
            Toaster.toast("Error al reproducir")
        }

    }

    fun onDestroy() {
        loading?.safeDismiss()
        loading = null
        CastManager.getInstance().onDestroy()
    }

    override fun isConnected() {

    }

    override fun isDisconnected() {
        setEid(NO_PLAYING)
    }

    private fun getLoading(view: View): Snackbar {
        return view.showSnackbar("Cargando...", duration = Snackbar.LENGTH_INDEFINITE)
    }

    private fun startLoading(view: View) {
        launch(UI) {
            loading = getLoading(view)
        }
    }

    private fun stopLoading() {
        launch(UI) {
            loading?.safeDismiss()
            loading = null
        }
    }

    private fun setEid(eid: String) {
        launch(UI) {
            casting.setValue(eid)
        }
    }

    fun openControls() {
        context.startActivity(Intent(context, ThemedControlsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onPlayStatusChanged(playStatus: Int) {
        when (playStatus) {
            PlayStatusListener.STATUS_START_PLAYING -> {
                Log.e("Status", "On start playing")
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
        Log.e("Duration", totalDuration.toString())
    }

    override fun onSuccessSeek() {

    }

    companion object {
        var NO_PLAYING = "no_play"
        @SuppressLint("StaticFieldLeak")
        private lateinit var ourInstance: CastUtil

        fun init(context: Context) {
            ourInstance = CastUtil(context)
        }

        @Contract(pure = true)
        fun get(): CastUtil {
            return ourInstance
        }
    }
}
