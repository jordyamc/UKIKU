package knf.kuma.download

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import knf.kuma.commons.EAHelper
import knf.kuma.commons.doOnUI
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.safeShow
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.NotificationObj
import knf.kuma.videoservers.ServersFactory
import org.jetbrains.anko.doAsync

class DownloadDialogActivity : GenericActivity() {

    private lateinit var downloadObject: DownloadObject

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getThemeDialog())
        super.onCreate(savedInstanceState)
        title = " "
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setFinishOnTouchOutside(false)
        val dialog = MaterialDialog(this).safeShow {
            message(text = "Obteniendo informacion...")
            cancelable(false)
            cancelOnTouchOutside(false)
        }
        doAsync {
            try {
                val recent = CacheDB.INSTANCE.recentAV1DAO().findByEid(intent.getIntExtra("eid", 0))
                    ?: throw IllegalStateException("Recent no found")
                downloadObject = recent.asDownload()
                doOnUI {
                    dialog.safeDismiss()
                    try {
                        showSelectDialog()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                finish()
            }
        }

    }

    private fun showSelectDialog() {
        MaterialDialog(this).safeShow {
            listItems(items = listOf("Descarga", "Streaming")) { _, index, _ ->
                ServersFactory.start(this@DownloadDialogActivity, intent.dataString
                        ?: "", downloadObject, index == 1, object : ServersFactory.ServersInterface {
                    override fun onFinish(started: Boolean, success: Boolean) {
                        if (success)
                            removeNotification()
                        finish()
                    }

                    override fun onCast(url: String?) {

                    }

                    override fun onProgressIndicator(boolean: Boolean) {

                    }

                    override fun getView(): View? {
                        return null
                    }
                })
            }
            setOnCancelListener { finish() }
        }
    }

    private fun removeNotification() {
        if (intent.getBooleanExtra("notification", false))
            sendBroadcast(NotificationObj.fromIntent(intent).getBroadcast(this))
    }
}
