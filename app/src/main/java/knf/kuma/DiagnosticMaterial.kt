package knf.kuma

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError
import knf.kuma.ads.AdsUtils
import knf.kuma.ads.SubscriptionReceiver
import knf.kuma.backup.Backups
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import knf.kuma.custom.StateView
import knf.kuma.custom.StateViewMaterial
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirectoryService
import knf.kuma.directory.DirectoryUpdateService
import knf.tools.bypass.startBypass
import kotlinx.android.synthetic.main.layout_diagnostic_material.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.math.BigDecimal
import java.math.RoundingMode

class DiagnosticMaterial : GenericActivity() {

    private val networkStatus by lazy { NetworkStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(R.layout.layout_diagnostic_material)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Diagnóstico"
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        startTests()
    }

    private fun startTests() {
        runNetworkTests()
        //runInternetTest()
        runDirectoryTest()
        runMemoryTest()
        runBackupTest()
    }

    private suspend fun runMainTest() {
        val startTime = System.currentTimeMillis()
        val responseCode = try {
            val response = Jsoup.connect(BypassUtil.testLink).timeout(0).execute()
            response.body()
            response.statusCode()
        } catch (e: HttpStatusException) {
            e.statusCode
        }
        networkStatus.mainResult = responseCode
        val loadingTime = System.currentTimeMillis() - startTime
        withContext(Dispatchers.Main) {
            codeState.load(
                responseCode.toString(), when (responseCode) {
                    200 -> StateView.STATE_OK
                    503 -> StateView.STATE_WARNING
                    else -> StateView.STATE_ERROR
                }
            )
            timeoutState.load(
                "$loadingTime ms", when {
                    loadingTime < 1000 -> StateView.STATE_OK
                    loadingTime < 2000 -> StateView.STATE_WARNING
                    else -> StateView.STATE_ERROR
                }
            )
            generalState.load(when {
                responseCode == 200 && loadingTime < 1000 -> "Correcto"
                responseCode == 503 -> "Cloudflare activado"
                responseCode == 403 -> "Bloqueado por animeflv"
                loadingTime > 1000 -> "Página lenta"
                else -> "Desconocido"
            }, when {
                responseCode == 200 && loadingTime < 1000 -> StateView.STATE_OK.also {
                    info.visibility = View.GONE
                }
                responseCode == 503 || responseCode == 403 || loadingTime > 1000 -> StateView.STATE_WARNING.also {
                    info.visibility = View.VISIBLE
                }
                else -> StateView.STATE_ERROR.also { info.visibility = View.GONE }
            })
            info.setOnClickListener {
                when {
                    networkStatus.mainResult == 503 -> show503Info()
                    networkStatus.mainResult == 403 -> show403Info()
                    loadingTime > 1000 -> showTimeoutInfo()
                }
            }
        }
        networkStatus.isMainTestExecuted = true
    }

    private suspend fun runBypassTest() {
        try {
            Jsoup.connect(BypassUtil.testLink).followRedirects(true).timeout(0).execute()
            bypassState.load("No se necesita")
            withContext(Dispatchers.Main) { bypassRecreate.visibility = View.GONE }
        } catch (e: HttpStatusException) {
            withContext(Dispatchers.Main) {
                bypassRecreate.apply {
                    visibility = View.VISIBLE
                    onClick {
                        startBypass(
                            5546, BypassUtil.testLink,
                            lastUA = PrefsUtil.userAgent,
                            showReload = AdsUtils.remoteConfigs.getBoolean("bypass_show_reload"),
                            useFocus = isTV,
                            maxTryCount = AdsUtils.remoteConfigs.getLong("bypass_max_tries")
                                .toInt(),
                            reloadOnCaptcha = AdsUtils.remoteConfigs.getBoolean("bypass_skip_captcha"),
                            clearCookiesAtStart = true,
                            useDialog = AdsUtils.remoteConfigs.getBoolean("bypass_use_dialog"),
                            dialogStyle = AdsUtils.remoteConfigs.getLong("bypass_dialog_style")
                                .toInt()
                        )
                    }
                }
            }
            try {
                jsoupCookies(BypassUtil.testLink).timeout(0).get()
                bypassState.load("Valido", StateView.STATE_OK)
                if (networkStatus.isMainTestExecuted && networkStatus.mainResult in listOf(
                        403,
                        503
                    )
                ) {
                    withContext(Dispatchers.Main) {
                        codeState.load("200", StateView.STATE_OK)
                        generalState.load("Bypass activo", StateView.STATE_OK)
                        info.isVisible = false
                    }
                }
            } catch (e: HttpStatusException) {
                when (e.statusCode) {
                    503 -> bypassState.load("Caducado", StateView.STATE_WARNING)
                    else -> bypassState.load(
                        "Error en página: HTTP ${e.statusCode}",
                        StateView.STATE_ERROR
                    )
                }
            }
            loadBypassInfo()
        } catch (e: Exception) {
            e.printStackTrace()
            bypassState.load("Error en página: ${e.message}", StateView.STATE_ERROR)
        }
        networkStatus.isBypassTestExecuted = true
    }

    private fun runNetworkTests() {
        lifecycleScope.launch(Dispatchers.IO) {
            runMainTest()
            runBypassTest()
        }
    }

    private fun loadBypassInfo() {
        doAsync {
            val document = Jsoup.connect("https://check-ip.com/").get()
            ipState.load(document.select("span#your-ip").text())
            val country = document.select("span[data-field=ipv6]").text()
            if (country == "Peru") {
                countryState.load("$country - VPN necesario", StateView.STATE_ERROR)
            } else {
                countryState.load(country, StateView.STATE_OK)
            }
        }
        clearanceState.apply {
            val data = BypassUtil.getClearance(this@DiagnosticMaterial)
            if (data.isNotEmpty())
                load(data)
        }
        cfduidState.apply {
            val data = BypassUtil.getCFDuid(this@DiagnosticMaterial)
            if (data.isNotEmpty())
                load(data)
        }
        userAgentState.apply {
            load(BypassUtil.userAgent)
        }
    }

    private fun runInternetTest() {
        doAsync {
            SpeedTestSocket().apply {
                addSpeedTestListener(object : ISpeedTestListener {
                    override fun onCompletion(report: SpeedTestReport?) {
                        report?.let { downState.load(formatBigDecimal(it.transferRateOctet)) }
                    }

                    override fun onProgress(percent: Float, report: SpeedTestReport?) {
                        report?.let { downState.load(formatBigDecimal(it.transferRateOctet)) }
                    }

                    override fun onError(speedTestError: SpeedTestError?, errorMessage: String?) {
                        downState.load("Error: ${errorMessage ?: ""}", StateViewMaterial.STATE_ERROR)
                    }
                })
                startDownload("https://speed.hetzner.de/100MB.bin")
            }
        }
        doAsync {
            SpeedTestSocket().apply {
                addSpeedTestListener(object : ISpeedTestListener {
                    override fun onCompletion(report: SpeedTestReport?) {
                        report?.let { upState.load(formatBigDecimal(it.transferRateOctet)) }
                    }

                    override fun onProgress(percent: Float, report: SpeedTestReport?) {
                        report?.let { upState.load(formatBigDecimal(it.transferRateOctet)) }
                    }

                    override fun onError(speedTestError: SpeedTestError?, errorMessage: String?) {
                        upState.load("Error: ${errorMessage ?: ""}", StateViewMaterial.STATE_ERROR)
                    }
                })
                startUpload("http://bouygues.testdebit.info/ul/", 5000000)
            }
        }
    }

    private fun formatBigDecimal(bigDecimal: BigDecimal): String {
        var decimal = bigDecimal.movePointLeft(3)
        val unit = when {
            decimal >= BigDecimal.valueOf(1000000) -> {
                decimal = decimal.movePointLeft(6)
                "Gb/s"
            }
            decimal >= BigDecimal.valueOf(1000) -> {
                decimal = decimal.movePointLeft(3)
                "Mb/s"
            }
            else -> "Kb/s"
        }
        return "${decimal.setScale(1, RoundingMode.HALF_UP)}$unit~"
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
        val dirs = getExternalFilesDirs(null).toList().filterNotNull()
        noCrash {
            internalState.load(getAvailable(dirs[0].freeSpace))
        }
        noCrash {
            if (dirs.size > 1)
                externalState.load(getAvailable(dirs[1].freeSpace))
        }
    }

    private fun getAvailable(size: Long): String {
        return Formatter.formatFileSize(this, size)
    }

    private fun runBackupTest() {
        uuid.text = FirestoreManager.uid ?: "Solo firestore"
        GlobalScope.launch(Dispatchers.IO) {
            if (PrefsUtil.isSubscriptionEnabled) {
                val status = SubscriptionReceiver.checkStatus(PrefsUtil.subscriptionToken
                        ?: "")
                if (status.isActive) {
                    if (status.isActive)
                        subscriptionState.load("Activa")
                    else
                        subscriptionState.load("Activa pero no renovada")
                } else
                    subscriptionState.load("Cancelada o inexistente")
            } else
                subscriptionState.load("No suscrito")
        }
        backupState.load(when (Backups.type) {
            Backups.Type.DROPBOX -> "Dropbox"
            Backups.Type.FIRESTORE -> "Firestore"
            Backups.Type.LOCAL -> "Local"
            else -> "Sin respaldos"
        })
        if (Backups.type != Backups.Type.NONE)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5546)
            runNetworkTests()
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, DiagnosticMaterial::class.java))
        }
    }

    private data class NetworkStatus(
        var isMainTestExecuted: Boolean = false,
        var isBypassTestExecuted: Boolean = false,
        var mainResult: Int = -1,
    )

    class FullBypass : GenericActivity() {
        private val overlay: View by lazy { find(R.id.overlay) as View }
        private val logText: TextView by lazy { find(R.id.logText) as TextView }
        private val fab: FloatingActionButton by lazy { find(R.id.fab) as FloatingActionButton }
        private var isOpened = false
        private var isFinishPending = false
        private val builder = StringBuilder("Initializing log...\n")
        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(EAHelper.getTheme())
            super.onCreate(savedInstanceState)
            try {
                setContentView(R.layout.activity_webview)
            } catch (e: Exception) {
                setContentView(R.layout.activity_webview_nwv)
            }
            logText.movementMethod = ScrollingMovementMethod()
            fab.setOnClickListener {
                if (isOpened && isFinishPending)
                    finish()
                else if (isOpened) {
                    isOpened = false
                    overlay.visibility = View.GONE
                    logText.visibility = View.GONE
                    fab.setImageResource(R.drawable.ic_terminal)
                } else {
                    isOpened = true
                    overlay.visibility = View.VISIBLE
                    logText.visibility = View.VISIBLE
                    fab.setImageResource(R.drawable.ic_close)
                }
            }
            logText("On Create check")
            checkBypass()
        }

        override fun forceCreation(): Boolean = true

        //override fun getSnackbarAnchor(): View? = find(R.id.coordinator)

        override fun onBypassUpdated() {
            if (isOpened)
                isFinishPending = true
            else
                finish()
        }

        override fun logText(text: String) {
            super.logText(text)
            builder.apply {
                append(text)
                append("\n")
            }
            lifecycleScope.launch(Dispatchers.Main) { logText.text = builder.toString() }
        }
    }
}