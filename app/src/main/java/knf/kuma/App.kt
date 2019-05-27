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
import com.evernote.android.job.JobManager
import es.munix.multidisplaycast.CastManager
import io.branch.referral.Branch
import io.fabric.sdk.android.Fabric
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.CastUtil
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.database.CacheDB
import knf.kuma.database.EADB
import knf.kuma.directory.DirectoryService
import knf.kuma.download.DownloadManager
import knf.kuma.download.DownloadService
import knf.kuma.download.FileAccessHelper
import knf.kuma.jobscheduler.BackupJob
import knf.kuma.jobscheduler.JobsCreator
import knf.kuma.jobscheduler.RecentsJob
import knf.kuma.jobscheduler.UpdateJob
import knf.kuma.widgets.emision.WEmissionService

class App : Application() {
    //private lateinit var appCoinsAds: AppCoinsAds

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val dirChannel = NotificationChannel(DirectoryService.CHANNEL, getString(R.string.directory_channel_title), NotificationManager.IMPORTANCE_MIN)
        dirChannel.setSound(null, AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN).setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        dirChannel.setShowBadge(false)
        manager?.createNotificationChannel(dirChannel)
        manager?.createNotificationChannel(NotificationChannel(RecentsJob.CHANNEL_RECENTS, "Capitulos recientes", NotificationManager.IMPORTANCE_HIGH))
        manager?.createNotificationChannel(NotificationChannel(DownloadService.CHANNEL, "Descargas", NotificationManager.IMPORTANCE_HIGH))
        manager?.createNotificationChannel(NotificationChannel(DownloadService.CHANNEL_ONGOING, "Descargas en progreso", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        manager?.createNotificationChannel(NotificationChannel(DownloadManager.CHANNEL_FOREGROUND, "Administrador de descargas", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
        manager?.createNotificationChannel(NotificationChannel(UpdateJob.CHANNEL, "Actualización de la app", NotificationManager.IMPORTANCE_DEFAULT))
        manager?.createNotificationChannel(NotificationChannel(WEmissionService.CHANNEL, "Actualizador de widget", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
    }

    override fun onCreate() {
        super.onCreate()
        JobManager.create(this).addJobCreator(JobsCreator())
        context = this
        Fabric.with(this,
                Crashlytics.Builder().core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build(),
                Answers()
        )
        Branch.getAutoInstance(this)
        AppCompatDelegate.setDefaultNightMode(Integer.parseInt(PrefsUtil.themeOption))
        BackupJob.checkInit()
        CastManager.register(this)
        CacheDB.init(this)
        EADB.init(this)
        EAHelper.init(this)
        CastUtil.init(this)
        DownloadManager.init()
        FileAccessHelper.init(this)
        AchievementManager.init(this)
        AppBrain.addTestDevice("6e5a4187367ad5c0")
        initAppCoins()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
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
