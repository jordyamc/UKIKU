package knf.kuma.custom

import android.app.ActivityManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.R
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.isFullMode
import knf.kuma.commons.noCrash
import knf.kuma.commons.toastLong
import knf.kuma.directory.DirManager
import knf.kuma.directory.DirectoryService
import knf.kuma.retrofit.Repository
import knf.kuma.uagen.randomUA
import knf.kuma.videoservers.FileActions
import knf.kuma.videoservers.ServersFactory
import knf.tools.bypass.startBypass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync


open class GenericActivity : AppCompatActivity() {

    override fun onResume() {
        noCrash {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                val appIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                setTaskDescription(ActivityManager.TaskDescription(getString(R.string.app_name), appIcon, ContextCompat.getColor(this, R.color.colorPrimary)))
            }
        }
        logText("On Resume check")
        noCrash { super.onResume() }
    }

    open fun getSnackbarAnchor(): View? = null

    open fun onBypassUpdated() {

    }

    open fun forceCreation(): Boolean = false

    open fun logText(text: String) {
        Log.e("Bypass", text)
    }

    fun checkBypass() {
        if (BypassUtil.isChecking) {
            logText("Already checking")
            return
        }
        BypassUtil.isChecking = true
        doAsync(exceptionHandler = {
            it.also {
                FirebaseCrashlytics.getInstance().recordException(it)
                logText("Error: ${it.message}")
            }.message?.toastLong()
        }) {
            var flag: Int
            if ((BypassUtil.isNeededFlag().also { flag = it } >= 1 || forceCreation()).also { logText("Is needed or forced: $it") }
                    && !BypassUtil.isLoading.also { logText("Is already loading: $it") }) {
                BypassUtil.isChecking = false
                logText("Flag: $flag")
                val runBypass = {
                    lifecycleScope.launch(Dispatchers.Main) {
                        bypassLive.value = Pair(true, true)
                        BypassUtil.isLoading = true
                        startBypass(
                            4157,
                            BypassUtil.createRequest()
                        )
                    }
                }
                if (isFullMode && !PrefsUtil.isBypassWarningShown){
                    lifecycleScope.launch(Dispatchers.Main){
                        MaterialDialog(this@GenericActivity).show {
                            lifecycleOwner(this@GenericActivity)
                            title(text = "Bypass necesario")
                            message(text = "La app necesita saltarse la proteccion de animeflv asi que necesita crear un bypass, esto puede tardar varios minutos, la pantalla cambiara automaticamente una vez terminado el proceso")
                            cancelable(false)
                            positiveButton(text = "OK"){
                                PrefsUtil.isBypassWarningShown = true
                                runBypass()
                            }
                        }
                    }
                }else{
                    runBypass()
                }
            } else {
                BypassUtil.isChecking = false
                logText("Creation not needed, aborting")
                bypassLive.postValue(Pair(false, false))
                Log.e("CloudflareBypass", "Not needed")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 4157) {
            val cookiesUpdated = data?.let {
                PrefsUtil.useDefaultUserAgent = false
                PrefsUtil.userAgent = it.getStringExtra("user_agent") ?: randomUA()
                BypassUtil.saveCookies(this, it.getStringExtra("cookies") ?: "null")
            } ?: false
            BypassUtil.isLoading = false
            bypassLive.value = Pair(first = cookiesUpdated, second = false)
            Repository().reloadAllRecents()
            onBypassUpdated()
            PicassoSingle.clear()
            //ThumbsDownloader.start(this)
            if (!PrefsUtil.isDirectoryFinished) {
                lifecycleScope.launch(Dispatchers.IO) {
                    DirManager.checkPreDir()
                    DirectoryService.run(this@GenericActivity)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        BypassUtil.isLoading = false
    }

    override fun onDestroy() {
        super.onDestroy()
        ServersFactory.clear()
        FileActions.reset()
        if (forceCreation())
            bypassLive.value = Pair(false, false)
    }

    companion object {
        private val observersList = mutableMapOf<String, Observer<Pair<Boolean, Boolean>>>()
        val bypassLive: MutableLiveData<Pair<Boolean, Boolean>> = MutableLiveData()

        fun addBypassObserver(id: String, owner: LifecycleOwner, observer: Observer<Pair<Boolean, Boolean>>) {
            removeBypassObserver(id)
            observersList[id] = observer
            bypassLive.observe(owner, observer)
        }

        fun removeBypassObserver(id: String) {
            if (observersList.containsKey(id)) {
                bypassLive.removeObserver(observersList[id]!!)
                observersList.remove(id)
            }
        }
    }
}