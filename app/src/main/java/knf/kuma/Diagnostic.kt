package knf.kuma

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StatFs
import android.text.format.Formatter
import android.view.View
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.backup.BUUtils
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import knf.kuma.custom.StateView
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirectoryService
import knf.kuma.directory.DirectoryUpdateService
import knf.kuma.download.FileAccessHelper
import kotlinx.android.synthetic.main.layout_diagnostic.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup

class Diagnostic : GenericActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_diagnostic)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Diagnóstico"
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        startTests()
    }

    private fun startTests() {
        runMainTest()
        runBypassTest()
        runDirectoryTest()
        runMemoryTest()
        runBackupTest()
    }

    private fun runMainTest() {
        doAsync {
            val startTime = System.currentTimeMillis()
            val responseCode = try {
                val response = Jsoup.connect("https://animeflv.net/").timeout(0).execute()
                response.body()
                response.statusCode()
            } catch (e: HttpStatusException) {
                e.statusCode
            }
            val loadingTime = System.currentTimeMillis() - startTime
            doOnUI {
                codeState.load(responseCode.toString(), when (responseCode) {
                    200 -> StateView.STATE_OK
                    503 -> StateView.STATE_WARNING
                    else -> StateView.STATE_ERROR
                })
                timeoutState.load("$loadingTime ms", when {
                    loadingTime < 10000 -> StateView.STATE_OK
                    loadingTime < 20000 -> StateView.STATE_WARNING
                    else -> StateView.STATE_ERROR
                })
                generalState.load(when {
                    responseCode == 200 && loadingTime < 10000 -> "Correcto"
                    responseCode == 503 -> "Cloudflare activado"
                    responseCode == 403 -> "Bloqueado por proveedor"
                    loadingTime > 10000 -> "Página lenta"
                    else -> "Desconocido"
                }, when {
                    responseCode == 200 && loadingTime < 10000 -> StateView.STATE_OK.also { info.visibility = View.GONE }
                    responseCode == 503 || responseCode == 403 || loadingTime > 10000 -> StateView.STATE_WARNING.also { info.visibility = View.VISIBLE }
                    else -> StateView.STATE_ERROR.also { info.visibility = View.GONE }
                })
                info.onClick {
                    when {
                        responseCode == 503 -> show503Info()
                        responseCode == 403 -> show403Info()
                        loadingTime > 10000 -> showTimeoutInfo()
                    }
                }
            }
        }
    }

    private fun runBypassTest() {
        doAsync {
            try {
                Jsoup.connect("https://animeflv.net/").timeout(0).execute()
                bypassState.load("No se necesita")
            } catch (e: HttpStatusException) {
                try {
                    jsoupCookies("https://animeflv.net/").timeout(0).get()
                    bypassState.load("Valido", StateView.STATE_OK)
                } catch (e: HttpStatusException) {
                    when (e.statusCode) {
                        503 -> bypassState.load("Caducado", StateView.STATE_WARNING)
                        else -> bypassState.load("Error en página: HTTP ${e.statusCode}", StateView.STATE_ERROR)
                    }
                }
                loadBypassInfo()
            } catch (e: Exception) {
                e.printStackTrace()
                bypassState.load("Error en página: ${e.message}", StateView.STATE_ERROR)
            }
        }
    }

    private fun loadBypassInfo() {
        clearanceState.apply {
            val data = BypassUtil.getClearance(this@Diagnostic)
            if (!data.isEmpty())
                load(data)
        }
        cfduidState.apply {
            val data = BypassUtil.getCFDuid(this@Diagnostic)
            if (!data.isEmpty())
                load(data)
        }
        userAgentState.apply {
            load(BypassUtil.userAgent)
        }
    }

    private fun runDirectoryTest() {
        dirState.load(when {
            PrefsUtil.isDirectoryFinished && !DirectoryUpdateService.isRunning -> "Completo"
            PrefsUtil.isDirectoryFinished && DirectoryUpdateService.isRunning -> "Actualizando"
            !PrefsUtil.isDirectoryFinished && DirectoryService.isRunning -> "Creando"
            else -> "Incompleto"
        })
        CacheDB.INSTANCE.animeDAO().countLive.observe(this, Observer {
            dirTotalState.load(it.toString())
        })
    }

    private fun runMemoryTest() {
        internalState.load(getAvailable(FileAccessHelper.INSTANCE.internalRoot.path))
        FileAccessHelper.INSTANCE.externalRoot?.path?.let { externalState.load(getAvailable(it)) }
    }

    private fun getAvailable(path: String): String {
        val stat = StatFs(path)
        return Formatter.formatFileSize(this, stat.blockSizeLong * stat.availableBlocksLong)
    }

    private fun runBackupTest() {
        backupState.load(when (BUUtils.getType(this)) {
            BUUtils.BUType.DROPBOX -> "Dropbox"
            else -> "Sin respaldos"
        })
        if (BUUtils.getType(this) != BUUtils.BUType.LOCAL)
            lastBackupState.load(PrefsUtil.lastBackup)
    }

    private fun show503Info() {
        MaterialDialog(this).safeShow {
            title(text = "HTTP 503")
            message(text = "Animeflv tiene el cloudflare activado, la app crea un bypass para funcionar normalmente")
        }
    }

    private fun show403Info() {
        MaterialDialog(this).safeShow {
            title(text = "HTTP 403")
            message(text = "Tu proveedor de internet bloquea la conexión con Animeflv, reinicia tu modem!")
        }
    }

    private fun showTimeoutInfo() {
        MaterialDialog(this).safeShow {
            title(text = "Timeout")
            message(text = "La página de Animeflv carga muy lento, modifica la espera de conexión desde configuración")
        }
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, Diagnostic::class.java))
        }
    }
}