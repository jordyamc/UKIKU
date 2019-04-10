package knf.kuma.changelog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.Crashlytics
import knf.kuma.R
import knf.kuma.changelog.objects.Changelog
import knf.kuma.commons.EAHelper
import knf.kuma.commons.doOnUI
import knf.kuma.commons.safeShow
import knf.kuma.custom.GenericActivity
import knf.kuma.jobscheduler.DirUpdateJob
import kotlinx.android.synthetic.main.recycler_changelog.*
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import xdroid.toaster.Toaster
import java.io.BufferedReader
import java.io.InputStreamReader

class ChangelogActivity : GenericActivity() {

    private val changelog: Changelog
        @Throws(Exception::class)
        get() = if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("changelog_load", true)) {
            Changelog(Jsoup.parse(xml, "", Parser.xmlParser()))
        } else {
            Changelog(Jsoup.connect("https://raw.githubusercontent.com/jordyamc/UKIKU/master/app/src/main/assets/changelog.xml").parser(Parser.xmlParser()).get())
        }

    private val xml: String?
        get() {
            var xmlString: String? = null
            val am = assets
            try {
                val reader = BufferedReader(InputStreamReader(am.open("changelog.xml")))
                val sb = StringBuilder()
                var mLine: String? = reader.readLine()
                while (mLine != null) {
                    sb.append(mLine)
                    mLine = reader.readLine()
                }
                reader.close()
                xmlString = sb.toString()
            } catch (e1: Exception) {
                e1.printStackTrace()
            }

            return xmlString
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_changelog)
        toolbar.title = "Changelog"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        doAsync {
            try {
                val changelog = changelog
                progress.post { progress.visibility = View.GONE }
                recycler.post { recycler.adapter = ReleaseAdapter(changelog) }
            } catch (e: Exception) {
                Crashlytics.logException(e)
                Toaster.toast("Error al cargar changelog")
                finish()
            }
        }
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, ChangelogActivity::class.java))
        }

        fun check(activity: AppCompatActivity) {
            doAsync {
                try {
                    val cCode = PreferenceManager.getDefaultSharedPreferences(activity).getInt("version_code", 0)
                    val pCode = PackageInfoCompat.getLongVersionCode(activity.packageManager.getPackageInfo(activity.packageName, 0)).toInt()
                    if (pCode > cCode && cCode != 0) {
                        runWVersion(pCode)
                        doOnUI {
                            MaterialDialog(activity).safeShow {
                                message(text = "Nueva versión, ¿Leer Changelog?")
                                positiveButton(text = "Leer") {
                                    open(activity)
                                    PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", pCode).apply()
                                }
                                negativeButton(text = "Omitir") { PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", pCode).apply() }
                                setOnCancelListener { PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", pCode).apply() }
                            }
                        }
                    } else
                        PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", pCode).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun runWVersion(code: Int) {
            when (code) {
                36 -> DirUpdateJob.runNow()
            }
        }
    }
}
