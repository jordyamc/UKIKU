package knf.kuma.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import es.munix.multidisplaycast.CastControlsActivity
import es.munix.multidisplaycast.CastManager
import es.munix.multidisplaycast.interfaces.CastListener
import es.munix.multidisplaycast.interfaces.PlayStatusListener
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.annotations.Contract
import xdroid.toaster.Toaster

class CastUtil private constructor(private val context: Context) : CastListener, PlayStatusListener {
    val casting = MutableLiveData<String>()
    private var isConnected = false

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

    fun play(context: Context, view: View, eid: String, url: String?, title: String, chapter: String, preview: String, isAid: Boolean) {
        var fUrl = url
        var fPreview = preview
        try {
            if (connected()) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("samsung_mode", false) && !url!!.endsWith(":" + SelfServer.HTTP_PORT))
                    fUrl = SelfServer.start(url, false)
                if (!url!!.endsWith(":" + SelfServer.HTTP_PORT))
                    SelfServer.stop()
                Log.e("Cast", url)
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
        CastManager.getInstance().unsetCastListener(javaClass.simpleName)
        CastManager.getInstance().unsetPlayStatusListener(javaClass.simpleName)
        CastManager.getInstance().onDestroy()
    }

    override fun isConnected() {
        isConnected = true
    }

    override fun isDisconnected() {
        isConnected = false
    }

    private fun getLoading(view: View): Snackbar {
        return view.showSnackbar("Cargando...", duration = Snackbar.LENGTH_INDEFINITE).also { loading = it }
    }

    private fun startLoading(view: View) {
        launch(UI) {
            try {
                getLoading(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopLoading() {
        launch(UI) {
            try {
                if (loading != null)
                    loading!!.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setEid(eid: String) {
        launch(UI) {
            casting.setValue(eid)
        }
    }

    fun openControls() {
        context.startActivity(Intent(context, CastControlsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onPlayStatusChanged(playStatus: Int) {
        when (playStatus) {
            PlayStatusListener.STATUS_START_PLAYING -> {
                stopLoading()
                openControls()
            }
            PlayStatusListener.STATUS_FINISHED, PlayStatusListener.STATUS_STOPPED -> setEid(NO_PLAYING)
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
