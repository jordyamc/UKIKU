package knf.kuma.updater

import androidx.activity.addCallback
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.R
import knf.kuma.commons.getUpdateDir
import knf.kuma.commons.safeShow
import knf.kuma.custom.GenericActivity
import knf.kuma.databinding.ActivityUpdaterBinding
import knf.kuma.download.DownloadManagerCentral
import java.io.File
import androidx.core.net.toUri

class UpdateActivity : GenericActivity() {

    private val binding by lazy { ActivityUpdaterBinding.inflate(layoutInflater) }
    private val updaterViewModel: UpdaterViewModel by viewModels()
    private val update: File by lazy { File.createTempFile("update", ".apk", filesDir) }
    private var isUpdateDownloaded = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        binding.download.setOnClickListener { install() }
        binding.retry.setOnClickListener {
            prepareForStart()
            start()
        }
        binding.progress.max = 100
        val animationDrawable = binding.relBack.background as AnimationDrawable
        if (!animationDrawable.isRunning) {
            animationDrawable.setEnterFadeDuration(2500)
            animationDrawable.setExitFadeDuration(2500)
            animationDrawable.start()
        }
        onBackPressedDispatcher.addCallback(this) {
            if (intent.getBooleanExtra("canExit", true)) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
        start()
    }

    private fun start() {
        updaterViewModel.start(update, "https://github.com/jordyamc/UKIKU/raw/master/app/$getUpdateDir/app-$getUpdateDir.apk")
            .observe(this, Observer {
                when (it.first) {
                    UpdaterType.TYPE_IDLE -> {
                        binding.progress.isIndeterminate = true
                    }

                    UpdaterType.TYPE_PROGRESS -> {
                        setDownProgress(it.second as Int)
                    }

                    UpdaterType.TYPE_ERROR -> {
                        prepareForRetry()
                        onShowAlternativeDownload()
                    }

                    UpdaterType.TYPE_COMPLETED -> {
                        isUpdateDownloaded = true
                        binding.progress.progress = 100
                        binding.progressText.text = "100%"
                        prepareForInstall()
                    }
                }
            })
    }

    override fun onStart() {
        super.onStart()
        if (!intent.getBooleanExtra("canExit", true)) {
            MaterialDialog(this).safeShow {
                message(text = "Parece que la versión ${intent.getStringExtra("version")} está disponible, es obligatoria")
                positiveButton(text = "ok")
            }
        }
    }

    private fun install() {
        DownloadManagerCentral.pauseAll()
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE, FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", update))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, false)
            .putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, packageName)
        startActivity(intent)
    }

    private fun setDownProgress(p: Int) {
        try {
            binding.progress.apply {
                isIndeterminate = false
                progress = p
            }
            binding.progressText.text = String.format("%d%%", p)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun prepareForInstall() {
        setDownProgress(100)
        val fadein = AnimationUtils.loadAnimation(this, R.anim.fadein)
        fadein.duration = 1000
        val fadeout = AnimationUtils.loadAnimation(this, R.anim.fadeout)
        fadeout.duration = 1000
        binding.progressText.post {
            with(binding.progressText) {
                visibility = View.INVISIBLE
                startAnimation(fadeout)
            }
        }
        binding.download.post {
            with(binding.download) {
                visibility = View.VISIBLE
                startAnimation(fadein)
            }
        }
    }

    private fun prepareForStart() {
        setDownProgress(100)
        val fadein = AnimationUtils.loadAnimation(this, R.anim.fadein)
        fadein.duration = 1000
        val fadeout = AnimationUtils.loadAnimation(this, R.anim.fadeout)
        fadeout.duration = 1000
        binding.retry.post {
            with(binding.retry) {
                visibility = View.INVISIBLE
                startAnimation(fadeout)
            }
        }
        binding.progressText.post {
            with(binding.progressText) {
                visibility = View.VISIBLE
                startAnimation(fadein)
            }
        }
    }

    private fun prepareForRetry() {
        setDownProgress(100)
        val fadein = AnimationUtils.loadAnimation(this, R.anim.fadein)
        fadein.duration = 1000
        val fadeout = AnimationUtils.loadAnimation(this, R.anim.fadeout)
        fadeout.duration = 1000
        binding.progressText.post {
            with(binding.progressText) {
                visibility = View.INVISIBLE
                startAnimation(fadeout)
            }
        }
        binding.retry.post {
            with(binding.retry) {
                visibility = View.VISIBLE
                startAnimation(fadein)
            }
        }
    }

    private fun onShowAlternativeDownload() {
        MaterialDialog(this).safeShow {
            title(text = "¿Error al actualizar?")
            message(text = "Puedes descargar la actualización desde la pagina web oficial!")
            positiveButton(text = "Descargar") {
                startActivity(Intent(Intent.ACTION_VIEW, "https://ukiku.app".toUri()))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isUpdateDownloaded) {
            onShowAlternativeDownload()
        }
    }

    companion object {
        fun start(context: Context, canExit: Boolean, version: String) {
            context.startActivity(Intent(context, UpdateActivity::class.java).apply {
                putExtra("canExit", canExit)
                putExtra("version", version)
            })
        }
    }
}
