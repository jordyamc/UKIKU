package knf.kuma.preferences

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.preference.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import knf.kuma.BuildConfig
import knf.kuma.Main
import knf.kuma.R
import knf.kuma.backup.BUUtils
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.*
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
import java.io.FileOutputStream


class ConfigurationFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (activity != null && context != null)
            launch(UI) {
                noCrash {
                    addPreferencesFromResource(R.xml.preferences)
                    preferenceManager.sharedPreferences.edit().putBoolean("daynigth_permission", Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED).apply()
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        preferenceScreen.findPreference("daynigth_permission").isEnabled = false
                    preferenceScreen.findPreference("daynigth_permission").setOnPreferenceChangeListener { _, o ->
                        val check = o as Boolean
                        if (check && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            if (ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 5587)
                            } else if (ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                preferenceManager.sharedPreferences.edit().putBoolean("daynigth_permission", true).apply()
                                preferenceScreen.findPreference("daynigth_permission").isEnabled = false
                            }
                        true
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        preferenceScreen.findPreference("custom_tone").summary = "Abrir configuración"
                    else if (FileAccessHelper.INSTANCE.toneFile.exists())
                        preferenceScreen.findPreference("custom_tone").summary = "Personalizado"
                    preferenceScreen.findPreference("custom_tone").setOnPreferenceClickListener {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            noCrash {
                                startActivity(
                                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                                .putExtra(Settings.EXTRA_CHANNEL_ID, RecentsJob.CHANNEL_RECENTS)
                                                .putExtra(Settings.EXTRA_APP_PACKAGE, this@ConfigurationFragment.context?.packageName)
                                )
                            }
                        else
                            MaterialDialog(activity!!).safeShow {
                                title(text = "Tono de notificación")
                                listItems(items = listOf("Cambiar tono", "Tono de sistema")) { _, index, _ ->
                                    when (index) {
                                        0 -> startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                                                .addCategory(Intent.CATEGORY_OPENABLE)
                                                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                .setType("audio/*"), 4784)
                                        1 -> {
                                            FileAccessHelper.INSTANCE.toneFile.safeDelete()
                                            preferenceScreen.findPreference("custom_tone").summary = "Sistema"
                                        }
                                    }
                                }
                            }
                        return@setOnPreferenceClickListener true
                    }
                    if (BUUtils.getType(activity) != BUUtils.BUType.LOCAL) {
                        if (Network.isConnected) {
                            BUUtils.init(activity!!, object : BUUtils.LoginInterface {
                                override fun onLogin() {
                                    preferenceScreen.findPreference("auto_backup").summary = "Cargando..."
                                    BUUtils.search("autobackup", object : BUUtils.SearchInterface {
                                        override fun onResponse(backupObject: BackupObject<*>?) {
                                            launch(UI) {
                                                try {
                                                    if (backupObject != null) {
                                                        val autoBackupObject = backupObject as AutoBackupObject?
                                                        if (backupObject == AutoBackupObject(activity))
                                                            preferenceScreen.findPreference("auto_backup").summary = "%s"
                                                        else
                                                            preferenceScreen.findPreference("auto_backup").summary = "Solo " + autoBackupObject!!.name
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
                        AppCompatDelegate.setDefaultNightMode((o as String).toInt())
                        PreferenceManager.getDefaultSharedPreferences(activity).edit().putString("theme_value", o).apply()
                        WEmisionProvider.update(activity!!)
                        //AestheticUtils.updateIsDarkMode(this@ConfigurationFragment.context!!, o == "2")
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
                                ContextCompat.startForegroundService(safeContext.applicationContext, Intent(safeContext.applicationContext, DirectoryUpdateService::class.java))
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
                                MaterialDialog(activity!!).safeShow {
                                    message(text = "¿Desea recrear el directorio?")
                                    positiveButton(text = "continuar") {
                                        doAsync {
                                            CacheDB.INSTANCE.animeDAO().nuke()
                                            PreferenceManager.getDefaultSharedPreferences(safeContext.applicationContext).edit().putBoolean("directory_finished", false).apply()
                                            DirectoryService.run(safeContext.applicationContext)
                                        }
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
                        EAHelper.phase == 4 ->
                            preferenceScreen.findPreference("theme_color").setOnPreferenceChangeListener { _, value ->
                                startActivity(Intent(activity, Main::class.java).putExtra("start_position", 3))
                                activity?.finish()
                                //AestheticUtils.updateAccentColor(this@ConfigurationFragment.context!!,value as String)
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lv = view.findViewById<ListView>(android.R.id.list)
        if (lv != null)
            ViewCompat.setNestedScrollingEnabled(lv, true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            preferenceScreen.findPreference("daynigth_permission").isEnabled = false
        } else {
            preferenceManager.sharedPreferences.edit().putBoolean("daynigth_permission", false).apply()
            (preferenceScreen.findPreference("daynigth_permission") as SwitchPreference).isChecked = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FileAccessHelper.SD_REQUEST && resultCode == Activity.RESULT_OK) {
            if (!FileAccessHelper.INSTANCE.isUriValid(data?.data)) {
                Toaster.toast("Directorio invalido")
                FileAccessHelper.openTreeChooser(this)
            }
        } else if (requestCode == 4784 && resultCode == Activity.RESULT_OK) {
            if (!FileAccessHelper.INSTANCE.toneFile.exists())
                FileAccessHelper.INSTANCE.toneFile.createNewFile()
            FileUtil.moveFile(
                    context?.contentResolver!!,
                    data?.data!!,
                    FileOutputStream(FileAccessHelper.INSTANCE.toneFile), false)
                    .observe(this, Observer {
                        try {
                            if (it != null) {
                                if (it.second) {
                                    if (it.first == -1) {
                                        FileAccessHelper.INSTANCE.toneFile.safeDelete()
                                        Toaster.toast("Error al copiar")
                                    } else {
                                        Toaster.toast("Tono seleccionado!")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Toaster.toast("Error al importar")
                        }
                    })
        }
    }

    private fun queryName(uri: Uri?): String? {
        return if (uri != null) {
            val returnCursor = activity?.contentResolver?.query(uri, null, null, null, null)!!
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            val name = returnCursor.getString(nameIndex)
            returnCursor.close()
            name
        } else null
    }
}
