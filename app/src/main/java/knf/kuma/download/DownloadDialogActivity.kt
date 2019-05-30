package knf.kuma.download

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.NotificationObj
import knf.kuma.videoservers.ServersFactory
import org.jetbrains.anko.doAsync
import java.util.regex.Pattern

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
                val document = jsoupCookies(intent.dataString).get()
                val name = PatternUtil.fromHtml(document.select("nav.Brdcrmb.fa-home a[href^=/anime/]").first().text())
                lateinit var aid: String
                val eid = extract(intent.dataString, "^.*/(\\d+)/.*$")
                lateinit var num: String
                val matcher = Pattern.compile("var (.*) = (\\d+);").matcher(document.html())
                while (matcher.find()) {
                    when (matcher.group(1)) {
                        "anime_id" -> aid = matcher.group(2)
                        "episode_number" -> num = matcher.group(2)
                    }
                }
                val chapter = AnimeObject.WebInfo.AnimeChapter(Integer.parseInt(aid), "Episodio $num", eid, intent.dataString
                        ?: "", name, aid)
                downloadObject = DownloadObject.fromChapter(chapter, false)
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

    private fun extract(st: String?, regex: String): String {
        val matcher = Pattern.compile(regex).matcher(st)
        matcher.find()
        return matcher.group(1)
    }

    private fun removeNotification() {
        if (intent.getBooleanExtra("notification", false))
            sendBroadcast(NotificationObj.fromIntent(intent).getBroadcast(this))
    }
}
