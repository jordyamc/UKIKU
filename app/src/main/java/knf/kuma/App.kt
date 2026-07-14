package knf.kuma

/*import com.asf.appcoins.sdk.ads.AppCoinsAds
import com.asf.appcoins.sdk.ads.AppCoinsAdsBuilder*/
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import androidx.work.WorkManager
import es.munix.multidisplaycast.CastManager
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.AllSSLOkHttpClient
import knf.kuma.commons.PrefsUtil
import knf.kuma.download.DownloadManager
import knf.kuma.download.DownloadService
import knf.kuma.jobscheduler.BackUpWork
import knf.kuma.jobscheduler.RecentsWork
import knf.kuma.jobscheduler.UpdateWork
import knf.kuma.widgets.emision.WEmissionService
import okhttp3.OkHttp

class App : Application(), Configuration.Provider {
    //private lateinit var appCoinsAds: AppCoinsAds

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannels() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        manager?.createNotificationChannel(
            NotificationChannel(
                RecentsWork.CHANNEL_RECENTS,
                "Capitulos recientes",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        manager?.createNotificationChannel(
            NotificationChannel(
                DownloadService.CHANNEL,
                "Descargas",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        manager?.createNotificationChannel(
            NotificationChannel(
                DownloadService.CHANNEL_ONGOING,
                "Descargas en progreso",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) })
        manager?.createNotificationChannel(
            NotificationChannel(
                DownloadManager.CHANNEL_FOREGROUND,
                "Administrador de descargas",
                NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) })
        manager?.createNotificationChannel(
            NotificationChannel(
                UpdateWork.CHANNEL,
                "Actualización de la app",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager?.createNotificationChannel(
            NotificationChannel(
                WEmissionService.CHANNEL,
                "Actualizador de widget",
                NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) })
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        context = this
        if (!PrefsUtil.isFetchDBReset) {
            PrefsUtil.isFetchDBReset = true
            deleteDatabase("LibGlobalFetchLib.db")
        }
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(this, Configuration.Builder().build())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName)
            }
        }
        OkHttp.initialize(this)
        AppCompatDelegate.setDefaultNightMode(PrefsUtil.themeOption.toInt())
        AllSSLOkHttpClient.enableTLS()
        BackUpWork.checkInit()
        CastManager.register(this)
        AchievementManager.init(this)
        initAppCoins()
        createChannels()
    }

    private fun initAppCoins() {
        /*appCoinsAds= AppCoinsAdsBuilder()
                .withDebug(BuildConfig.DEBUG)
                .createAdvertisementSdk(this)
                .also { it.init(this) }*/
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
    }
}
