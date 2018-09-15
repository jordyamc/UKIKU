package knf.kuma.preferences

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.util.Log
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.BuildConfig
import knf.kuma.Main
import knf.kuma.R
import knf.kuma.backup.BUUtils
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.EAHelper
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.safeShow
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirectoryService
import knf.kuma.directory.DirectoryUpdateService
import knf.kuma.download.DownloadManager
import knf.kuma.download.FileAccessHelper
import knf.kuma.jobscheduler.BackupJob
import knf.kuma.jobscheduler.DirUpdateJob
import knf.kuma.jobscheduler.RecentsJob
import knf.kuma.pojos.AutoBackupObject
import knf.kuma.widgets.emision.WEmisionProvider
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster

class ConfigurationFragment : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launch(UI) {
            addPreferencesFromResource(R.xml.preferences)
            preferenceManager.sharedPreferences.edit().putBoolean("daynigth_permission", Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED).apply()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                preferenceScreen.findPreference("daynigth_permission").isEnabled = false
            preferenceScreen.findPreference("daynigth_permission").setOnPreferenceChangeListener { _, o ->
                val check = o as Boolean
                if (check && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 5587)
                    } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        preferenceManager.sharedPreferences.edit().putBoolean("daynigth_permission", true).apply()
                        preferenceScreen.findPreference("daynigth_permission").isEnabled = false
                    }
                true
            }
            if (BUUtils.getType(activity) != BUUtils.BUType.LOCAL) {
                if (Network.isConnected) {
                    BUUtils.init(activity, object : BUUtils.LoginInterface {
                        override fun onLogin() {
                            preferenceScreen.findPreference("auto_backup").summary = "Cargando..."
                            BUUtils.search("autobackup", object : BUUtils.SearchInterface {
                                override fun onResponse(backupObject: BackupObject<*>?) {
                                    launch(UI) {
                                        try {
                                            if (backupObject != null) {
                                                val autobackupObject = backupObject as AutoBackupObject?
                                                if (backupObject == AutoBackupObject(activity))
                                                    preferenceScreen.findPreference("auto_backup").summary = "%s"
                                                else
                                                    preferenceScreen.findPreference("auto_backup").summary = "Solo " + autobackupObject!!.name
                                            } else {
                                                preferenceManager.sharedPreferences.edit().putString("auto_backup", "0").apply()
                                                preferenceScreen.findPreference("auto_backup").summary = "%s"
                                            }
                                            preferenceScreen.findPreference("auto_backup").isEnabled = true
                                        } catch (e: Exception) {
                                            preferenceScreen.findPreference("auto_backup").summary = "Error al buscar archivo"
                                        }
                                    }
                                }
                            })
                        }
                    }, true)
                } else {
                    preferenceScreen.findPreference("auto_backup").summary = "Sin internet"
                }
            } else {
                preferenceScreen.findPreference("auto_backup").summary = "Sin cuenta para respaldos"
            }
            preferenceScreen.findPreference("auto_backup").setOnPreferenceChangeListener { _, o ->
                BackupJob.reSchedule(Integer.valueOf(o as String))
                BUUtils.backup(AutoBackupObject(activity), object : BUUtils.AutoBackupInterface {
                    override fun onResponse(backupObject: AutoBackupObject?) {
                        if (backupObject != null)
                            Log.e("Backup override", backupObject.name)
                    }
                })
                true
            }
            preferenceScreen.findPreference("download_type").setOnPreferenceChangeListener { _, o ->
                if (o == "1" && !FileAccessHelper.INSTANCE.canDownload(this@ConfigurationFragment, o as String))
                    Toaster.toast("Por favor selecciona la raiz de tu SD")
                true
            }
            if (PrefsUtil.downloaderType == 0) {
                preferenceScreen.findPreference("max_parallel_downloads").isEnabled = false
                preferenceScreen.findPreference("buffer_size").isEnabled = true
            } else {
                preferenceScreen.findPreference("max_parallel_downloads").isEnabled = true
                preferenceScreen.findPreference("buffer_size").isEnabled = false
            }
            preferenceScreen.findPreference("downloader_type").setOnPreferenceChangeListener { _, o ->
                if (o == "0") {
                    preferenceScreen.findPreference("max_parallel_downloads").isEnabled = false
                    preferenceScreen.findPreference("buffer_size").isEnabled = true
                } else {
                    preferenceScreen.findPreference("max_parallel_downloads").isEnabled = true
                    preferenceScreen.findPreference("buffer_size").isEnabled = false
                }
                true
            }
            preferenceScreen.findPreference("theme_option").setOnPreferenceChangeListener { _, o ->
                AppCompatDelegate.setDefaultNightMode(Integer.parseInt(o as String))
                PreferenceManager.getDefaultSharedPreferences(activity).edit().putString("theme_value", o).apply()
                WEmisionProvider.update(activity)
                activity.recreate()
                true
            }
            preferenceScreen.findPreference("recents_time").setOnPreferenceChangeListener { _, o ->
                preferenceScreen.findPreference("notify_favs").isEnabled = "0" != o
                RecentsJob.reSchedule(Integer.valueOf(o as String) * 15)
                true
            }
            preferenceScreen.findPreference("dir_update_time").setOnPreferenceChangeListener { _, o ->
                DirUpdateJob.reSchedule(Integer.valueOf(o as String) * 15)
                true
            }
            preferenceScreen.findPreference("dir_update").setOnPreferenceClickListener {
                try {
                    if (!DirectoryUpdateService.isRunning && !DirectoryService.isRunning)
                        ContextCompat.startForegroundService(activity.applicationContext, Intent(activity.applicationContext, DirectoryUpdateService::class.java))
                    else if (DirectoryUpdateService.isRunning)
                        Toaster.toast("Ya se esta actualizando")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                false
            }
            preferenceScreen.findPreference("dir_destroy").setOnPreferenceClickListener { _ ->
                try {
                    if (!DirectoryUpdateService.isRunning && !DirectoryService.isRunning)
                        MaterialDialog(activity).safeShow {
                            message(text = "¿Desea recrear el directorio?")
                            positiveButton(text = "continuar") {
                                CacheDB.INSTANCE.animeDAO().nuke()
                                PreferenceManager.getDefaultSharedPreferences(activity.applicationContext).edit().putBoolean("directory_finished", false).apply()
                                DirectoryService.run(activity.applicationContext)
                            }
                            negativeButton(text = "cancelar")
                        }
                    else if (DirectoryService.isRunning)
                        Toaster.toast("Ya se esta creando")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                false
            }
            when {
                EAHelper.phase == 4 -> preferenceScreen.findPreference("theme_color").setOnPreferenceChangeListener { _, _ ->
                    startActivity(Intent(activity, Main::class.java).putExtra("start_position", 3))
                    activity.finish()
                    true
                }
                EAHelper.phase == 0 -> {
                    val category = preferenceScreen.findPreference("category_design") as PreferenceCategory
                    category.removePreference(preferenceScreen.findPreference("theme_color"))
                    val pref = Preference(activity)
                    pref.title = "Color de tema"
                    pref.summary = "Resuelve el secreto para desbloquear"
                    pref.setIcon(R.drawable.ic_palette)
                    pref.setOnPreferenceClickListener {
                        Toaster.toast(EAHelper.eaMessage)
                        true
                    }
                    category.addPreference(pref)
                }
                else -> {
                    preferenceScreen.findPreference("theme_color").summary = "Resuelve el secreto para desbloquear"
                    preferenceScreen.findPreference("theme_color").isEnabled = false
                }
            }
            preferenceScreen.findPreference("hide_chaps").setOnPreferenceChangeListener { _, o ->
                if (!FileAccessHelper.NOMEDIA_CREATING) {
                    FileAccessHelper.INSTANCE.checkNoMedia(o as Boolean)
                    true
                } else {
                    (preferenceScreen.findPreference("hide_chaps") as SwitchPreference).isChecked = !(o as Boolean)
                    false
                }
            }
            preferenceScreen.findPreference("max_parallel_downloads").setOnPreferenceChangeListener { _, o ->
                DownloadManager.setParallelDownloads(o as String)
                true
            }
            if (BuildConfig.DEBUG) {
                preferenceScreen.findPreference("reset_recents").setOnPreferenceClickListener {
                    doAsync {
                        CacheDB.INSTANCE.recentsDAO().clear()
                        RecentsJob.run()
                    }
                    true
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lv = view.findViewById<ListView>(android.R.id.list)
        if (lv != null)
            ViewCompat.setNestedScrollingEnabled(lv, true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            preferenceScreen.findPreference("daynigth_permission").isEnabled = false
        } else {
            preferenceManager.sharedPreferences.edit().putBoolean("daynigth_permission", false).apply()
            (preferenceScreen.findPreference("daynigth_permission") as SwitchPreference).isChecked = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == FileAccessHelper.SD_REQUEST && resultCode == Activity.RESULT_OK) {
            if (!FileAccessHelper.INSTANCE.isUriValid(data.data!!)) {
                Toaster.toast("Directorio invalido")
                FileAccessHelper.openTreeChooser(this)
            }
        }
    }
}
