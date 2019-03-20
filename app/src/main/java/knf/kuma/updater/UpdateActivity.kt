package knf.kuma.updater

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.core.content.FileProvider
import com.crashlytics.android.Crashlytics
import com.thin.downloadmanager.DownloadRequest
import com.thin.downloadmanager.DownloadStatusListenerV1
import com.thin.downloadmanager.ThinDownloadManager
import knf.kuma.R
import knf.kuma.commons.doOnUI
import knf.kuma.commons.getUpdateDir
import knf.kuma.custom.GenericActivity
import knf.kuma.download.DownloadManager
import kotlinx.android.synthetic.main.activity_updater.*
import xdroid.toaster.Toaster
import java.io.File

class UpdateActivity : GenericActivity() {

    private val update: File
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            File(filesDir, "update.apk")
        else
            File(downloadsDir, "update.apk")

    private val downloadsDir: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_updater)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        download.setOnClickListener { install() }
        if (savedInstanceState == null) {
            val animationDrawable = rel_back.background as AnimationDrawable
            animationDrawable.setEnterFadeDuration(2500)
            animationDrawable.setExitFadeDuration(2500)
            animationDrawable.start()
            progress.max = 100
            progress.isIndeterminate = true
            showCard()
        }
    }

    private fun showCard() {
        card.post {
            val bounds = Rect()
            card.getDrawingRect(bounds)
            val centerX = bounds.centerX()
            val centerY = bounds.centerY()
            val finalRadius = Math.max(bounds.width(), bounds.height())

            val anim = ViewAnimationUtils.createCircularReveal(card, centerX, centerY, 0f, finalRadius.toFloat())
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    start()
                }

                override fun onAnimationCancel(animation: Animator) {

                }

                override fun onAnimationRepeat(animation: Animator) {

                }
            })
            card.visibility = View.VISIBLE
            anim.start()
        }
    }

    private fun start() {
        val file = update
        if (file.exists())
            file.delete()
        ThinDownloadManager().add(DownloadRequest(Uri.parse("https://github.com/jordyamc/UKIKU/raw/master/app/$getUpdateDir/app-$getUpdateDir.apk"))
                .setDestinationURI(Uri.fromFile(file))
                .setDownloadResumable(false)
                .setStatusListener(object : DownloadStatusListenerV1 {
                    override fun onDownloadComplete(downloadRequest: DownloadRequest?) {
                        prepareForIntall()
                    }

                    override fun onDownloadFailed(downloadRequest: DownloadRequest?, errorCode: Int, errorMessage: String?) {
                        Log.e("Update Error", "Code: $errorCode Message: $errorMessage")
                        Toaster.toast("Error al actualizar: $errorMessage")
                        Crashlytics.logException(IllegalStateException("Update failed\nCode: $errorCode Message: $errorMessage"))
                        finish()
                    }

                    override fun onProgress(downloadRequest: DownloadRequest?, totalBytes: Long, downloadedBytes: Long, progress: Int) {
                        setDownProgress(progress)
                    }
                }))
    }

    private fun install() {
        DownloadManager.pauseAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE, FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", update))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, false)
                    .putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, packageName)
            startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.fromFile(update), "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivity(intent)
        }
        finish()
    }

    private fun setDownProgress(p: Int) {
        doOnUI {
            try {
                with(progress) {
                    isIndeterminate = false
                    progress = p
                }
                progress_text.text = String.format("%d%%", p)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun prepareForIntall() {
        setDownProgress(100)
        val fadein = AnimationUtils.loadAnimation(this, R.anim.fadein)
        fadein.duration = 1000
        val fadeout = AnimationUtils.loadAnimation(this, R.anim.fadeout)
        fadeout.duration = 1000
        progress_text.post {
            with(progress_text) {
                visibility = View.INVISIBLE
                startAnimation(fadeout)
            }
        }
        download.post {
            with(download) {
                visibility = View.VISIBLE
                startAnimation(fadein)
            }
        }
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, UpdateActivity::class.java))
        }
    }
}
