package knf.kuma.migration

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.thin.downloadmanager.DefaultRetryPolicy
import com.thin.downloadmanager.DownloadRequest
import com.thin.downloadmanager.DownloadStatusListenerV1
import com.thin.downloadmanager.ThinDownloadManager
import knf.kuma.commons.load
import knf.kuma.custom.GenericActivity
import knf.kuma.databinding.ActivityMaintainmentMessageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xdroid.toaster.Toaster
import java.io.File

class MaintainmentActivity: GenericActivity() {

    private val binding by lazy { ActivityMaintainmentMessageBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.hydraImage.load("https://knf-hydra.app/icon.png")
        binding.message.text = """
            Todo parece indicar que Animeflv ahora si murió, la página web sigue existiendo pero no devuelve ningún video.
            Esperé bastantes dias a ver si revivía, pero parece que no sucederá.

            UKIKU será migrado a otra fuente, esto requiere bastante trabajo manual para perder la menor cantidad posible de datos (Favoritos, Historial, Descargas, etc).
            
            Si todo sale bien quizá en una semana (o menos) se empiece a probar la migración en el grupo de Telegram.
        """.trimIndent()

        binding.hydraMessage.text = """
            Los que tienen tiempo en los grupos sabrán que existe un proyecto secundario que se estaba desarrollando, como no era prioridad al existir Animeflv, el avance de la app es lento.
            
            Es una app modular, es decir que se le pueden instalar varias fuentes (actualmente solo JKAnime), no está 100% pulida pero le puede servir a alguno para seguir viendo anime en lo que se termina de migrar UKIKU.
        """.trimIndent()

        binding.socialMessage.text = Html.fromHtml("""
            <a href="https://t.me/ukiku_group">Telegram UKIKU</a>
            <br><a href="https://t.me/hydra_app_group">Telegram Hydra</a>
            <br><a href="https://discord.gg/6hzpua6">Discord</a>
            <br><a href="https://x.com/AppsKnf">Twitter (X)</a>
        """.trimIndent(), Html.FROM_HTML_MODE_COMPACT)
        binding.socialMessage.movementMethod = LinkMovementMethod.getInstance()
        binding.download.setOnClickListener {
            lifecycleScope.launch {
                binding.download.isEnabled = false
                val file = File(getExternalFilesDir(null), "hydra.apk")
                ThinDownloadManager().add(
                    DownloadRequest("https://github.com/hydra-app/Repository/raw/refs/heads/main/main/app-release.apk".toUri())
                        .setDestinationURI(Uri.fromFile(file))
                        .setDownloadResumable(false)
                        .setRetryPolicy(DefaultRetryPolicy(5000, 3, 1f))
                        .setStatusListener(object : DownloadStatusListenerV1 {
                            override fun onDownloadComplete(downloadRequest: DownloadRequest?) {
                                lifecycleScope.launch {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE, FileProvider.getUriForFile(this@MaintainmentActivity, "${applicationContext.packageName}.fileprovider", file))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                            .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, false)
                                            .putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, packageName)
                                        startActivity(intent)
                                    } else {
                                        val intent = Intent(Intent.ACTION_VIEW)
                                            .setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                        startActivity(intent)
                                    }
                                    binding.download.isEnabled = true
                                }
                            }

                            override fun onDownloadFailed(downloadRequest: DownloadRequest?, errorCode: Int, errorMessage: String?) {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    binding.download.isEnabled = true
                                    Toaster.toast("Error al descargar app")
                                }
                            }

                            override fun onProgress(downloadRequest: DownloadRequest?, totalBytes: Long, downloadedBytes: Long, progress: Int) {

                            }
                        }))
            }
        }
        binding.exit.setOnClickListener { finish() }
    }
}