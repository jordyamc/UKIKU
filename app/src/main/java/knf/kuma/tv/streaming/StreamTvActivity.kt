package knf.kuma.tv.streaming

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import knf.kuma.commons.EAHelper
import knf.kuma.commons.doOnUI
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import knf.kuma.tv.TVBaseActivity
import knf.kuma.tv.TVServersFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.doAsync
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
class StreamTvActivity : TVBaseActivity() {

    private lateinit var downloadObject: DownloadObject
    private var serversFactory: TVServersFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getThemeDialog())
        super.onCreate(savedInstanceState)
        title = " "
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setFinishOnTouchOutside(false)

        doAsync {
            try {
                val data = intent.data
                if (data != null) {
                    val chapter = AnimeObject.WebInfo.AnimeChapter
                        .fromData(
                            data.getQueryParameter("aid"),
                            data.getQueryParameter("chapter"),
                            data.getQueryParameter("eid"),
                            data.getQueryParameter("url"),
                            data.getQueryParameter("name")
                        )
                    downloadObject = DownloadObject.fromChapter(chapter, false)
                    doOnUI {
                        try {
                            TVServersFactory.start(
                                this@StreamTvActivity,
                                chapter.link,
                                chapter,
                                object : TVServersFactory.ServersInterface {
                                    override fun onReady(serversFactory: TVServersFactory) {
                                        this@StreamTvActivity.serversFactory = serversFactory
                                    }

                                    override fun onFinish(started: Boolean, success: Boolean) {
                                        finish()
                                    }
                                })
                        } catch (e: Exception) {
                            e.printStackTrace()
                            finish()
                        }
                    }
                } else {
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                finish()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (data != null)
                if (resultCode == Activity.RESULT_OK) {
                    val bundle = data.extras
                    if (requestCode == TVServersFactory.REQUEST_CODE_MULTI)
                        serversFactory?.analyzeMulti(bundle?.getInt("position", 0) ?: 0)
                    else {
                        if (bundle?.getBoolean("is_video_server", false) == true)
                            serversFactory?.analyzeOption(bundle.getInt("position", 0))
                        else
                            serversFactory?.analyzeServer(bundle?.getInt("position", 0) ?: 0)
                    }
                } else if (resultCode == Activity.RESULT_CANCELED && data.extras?.getBoolean(
                        "is_video_server",
                        false
                    ) == true
                )
                    serversFactory?.showServerList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
