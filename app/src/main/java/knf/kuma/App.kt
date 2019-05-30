package knf.kuma

/*import com.asf.appcoins.sdk.ads.AppCoinsAds
import com.asf.appcoins.sdk.ads.AppCoinsAdsBuilder*/
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.appbrain.AppBrain
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.core.CrashlyticsCore
import es.munix.multidisplaycast.CastManager
import io.branch.referral.Branch
import io.fabric.sdk.android.Fabric
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.PrefsUtil
import knf.kuma.directory.DirectoryService
import knf.kuma.download.DownloadManager
import knf.kuma.download.DownloadService
import knf.kuma.jobscheduler.BackUpWork
import knf.kuma.jobscheduler.RecentsWork
import knf.kuma.jobscheduler.UpdateWork
import knf.kuma.widgets.emision.WEmissionService
import org.jetbrains.anko.doAsync

class App : Application() {
    //private lateinit var appCoinsAds: AppCoinsAds

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val dirChannel = NotificationChannel(DirectoryService.CHANNEL, getString(R.string.directory_channel_title), NotificationManager.IMPORTANCE_MIN)
        dirChannel.setSound(null, AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN).setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        dirChannel.setShowBadge(false)
        manager?.createNotificationChannel(dirChannel)
        manager?.createNotificationChannel(NotificationChannel(RecentsWork.CHANNEL_RECENTS, "Capitulos recientes", NotificationManager.IMPORTANCE_HIGH))
        manager?.createNotificationChannel(NotificationChannel(DownloadService.CHANNEL, "Descargas", NotificationManager.IMPORTANCE_HIGH))
        manager?.createNotificationChannel(NotificationChannel(DownloadService.CHANNEL_ONGOING, "Descargas en progreso", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        manager?.createNotificationChannel(NotificationChannel(DownloadManager.CHANNEL_FOREGROUND, "Administrador de descargas", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
        manager?.createNotificationChannel(NotificationChannel(UpdateWork.CHANNEL, "Actualización de la app", NotificationManager.IMPORTANCE_DEFAULT))
        manager?.createNotificationChannel(NotificationChannel(WEmissionService.CHANNEL, "Actualizador de widget", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        AppCompatDelegate.setDefaultNightMode(Integer.parseInt(PrefsUtil.themeOption))
        BackUpWork.checkInit()
        CastManager.register(this)
        AchievementManager.init(this)
        AppBrain.addTestDevice("6e5a4187367ad5c0")
        initAppCoins()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannels()
        doAsync {
            Fabric.with(context,
                    Crashlytics.Builder().core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build(),
                    Answers()
            )
            Branch.getAutoInstance(context)
        }
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
