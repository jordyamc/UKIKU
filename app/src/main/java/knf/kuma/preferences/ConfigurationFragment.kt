package knf.kuma.preferences

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.crashlytics.android.Crashlytics
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.Main
import knf.kuma.R
import knf.kuma.backup.Backups
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.commons.*
import knf.kuma.custom.PreferenceFragmentCompat
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirectoryService
import knf.kuma.directory.DirectoryUpdateService
import knf.kuma.download.DownloadManager
import knf.kuma.download.FileAccessHelper
import knf.kuma.jobscheduler.BackUpWork
import knf.kuma.jobscheduler.DirUpdateWork
import knf.kuma.jobscheduler.RecentsWork
import knf.kuma.pojos.AutoBackupObject
import knf.kuma.widgets.emision.WEmisionProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.io.FileOutputStream


class ConfigurationFragment : PreferenceFragmentCompat() {

    companion object {
        private const val keyDaynigthPermission = "daynigth_permission"
        private const val keyCustomTone = "custom_tone"
        private const val keyAutoBackup = "auto_backup"
        private const val keyMaxParallelDownloads = "max_parallel_downloads"
        private const val keyBufferSize = "buffer_size"
        private const val keyThemeColor = "theme_color"
        private const val keyArchievementsPermissions = "achievements_permissions"
    }

    private var uaChangeListener: UAChangeListener? = null

    override fun onAttach(activity: Activity) {
        uaChangeListener = activity as? UAChangeListener
        super.onAttach(activity)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (activity != null && context != null)
            doOnUI {
                addPreferencesFromResource(R.xml.preferences)
                preferenceManager.sharedPreferences.edit().putBoolean(keyDaynigthPermission, Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED).apply()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    preferenceScreen.findPreference<Preference>(keyDaynigthPermission)?.isEnabled = false
                preferenceScreen.findPreference<Preference>(keyDaynigthPermission)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    val check = newValue as? Boolean
                    if (check == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        if (ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 5587)
                        } else if (ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            preferenceManager.sharedPreferences.edit().putBoolean(keyDaynigthPermission, true).apply()
                            preferenceScreen.findPreference<Preference>(keyDaynigthPermission)?.isEnabled = false
                        }
                    true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    preferenceScreen.findPreference<Preference>(keyCustomTone)?.summary = "Abrir configuración"
                else if (FileAccessHelper.toneFile.exists())
                    preferenceScreen.findPreference<Preference>(keyCustomTone)?.summary = "Personalizado"
                preferenceScreen.findPreference<Preference>(keyCustomTone)?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        noCrash {
                            startActivity(
                                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_CHANNEL_ID, RecentsWork.CHANNEL_RECENTS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, this@ConfigurationFragment.context?.packageName)
                            )
                        }
                    else
                        activity?.let {
                            MaterialDialog(it).safeShow {
                                title(text = "Tono de notificación")
                                listItems(items = listOf("Cambiar tono", "Tono de sistema")) { _, index, _ ->
                                    when (index) {
                                        0 -> startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                                                .addCategory(Intent.CATEGORY_OPENABLE)
                                                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                .setType("audio/*"), 4784)
                                        1 -> {
                                            FileAccessHelper.toneFile.safeDelete()
                                            preferenceScreen.findPreference<Preference>(keyCustomTone)?.summary = "Sistema"
                                        }
                                    }
                                }
                            }
                        }
                    true
                }
                if (Backups.type == Backups.Type.DROPBOX) {
                    if (Network.isConnected) {
                        activity?.let {
                            preferenceScreen.findPreference<Preference>(keyAutoBackup)?.summary = "Cargando..."
                            Backups.search(null, Backups.keyAutoBackup) {
                                doOnUI {
                                    try {
                                        val autoBackupObject = it as? AutoBackupObject
                                        if (autoBackupObject != null) {
                                            if (autoBackupObject == AutoBackupObject(activity))
                                                preferenceScreen.findPreference<Preference>(keyAutoBackup)?.summary = "%s"
                                            else
                                                preferenceScreen.findPreference<Preference>(keyAutoBackup)?.summary = "Solo " + autoBackupObject.name
                                            if (autoBackupObject.value == null)
                                                GlobalScope.launch(Dispatchers.Main) {
                                                    Backups.createService()?.backup(AutoBackupObject(App.context), Backups.keyAutoBackup)
                                                    preferenceScreen.findPreference<Preference>(keyAutoBackup)?.summary = "%s"
                                                }
                                            else
                                                preferenceManager.sharedPreferences.edit().putString(keyAutoBackup, autoBackupObject.value).apply()
                                        } else {
                                            preferenceScreen.findPreference<Preference>(keyAutoBackup)?.summary = "%s (NE)"
                                        }
                                        preferenceScreen.findPreference<Preference>(keyAutoBackup)?.isEnabled = true
                                    } catch (e: Exception) {
                                        Crashlytics.logException(e)
                                        preferenceScreen.findPreference<Preference>(keyAutoBackup)?.summary = "Error al buscar archivo: ${e.message}"
                                        preferenceScreen.findPreference<Preference>(keyAutoBackup)?.isEnabled = true
                                    }
                                }
                            }
                        }
                    } else {
                        preferenceScreen.findPreference<Preference>(keyAutoBackup)?.summary = "Sin internet"
                    }
                } else if (Backups.type == Backups.Type.NONE) {
                    preferenceScreen.findPreference<Preference>(keyAutoBackup)?.summary = "Sin cuenta para respaldos"
                } else {
                    preferenceScreen.findPreference<Preference>(keyAutoBackup)?.isVisible = false
                }
                preferenceScreen.findPreference<Preference>(keyAutoBackup)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    BackUpWork.reSchedule(Integer.valueOf((newValue as? String) ?: "0"))
                    GlobalScope.launch(Dispatchers.Main) {
                        Backups.createService()?.backup(AutoBackupObject(App.context, (newValue as? String)
                                ?: "0"), Backups.keyAutoBackup)
                        preferenceScreen.findPreference<Preference>(keyAutoBackup)?.summary = "%s"
                    }
                    true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    preferenceScreen.removePreferenceRecursively("download_type")
                    val preferenceDownloads = preferenceScreen.findPreference<Preference>("download_type_q")
                    preferenceDownloads?.summary = PrefsUtil.storageType
                    preferenceDownloads?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        FileAccessHelper.openTreeChooser(this@ConfigurationFragment)
                        Toaster.toast("Por favor selecciona la raiz del almacenamiento")
                        true
                    }
                } else {
                    preferenceScreen.removePreferenceRecursively("download_type_q")
                    preferenceScreen.findPreference<Preference>("download_type")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                        if (newValue == "1" && !FileAccessHelper.canDownload(this@ConfigurationFragment, newValue as? String))
                            Toaster.toast("Por favor selecciona la raiz de tu SD")
                        else
                            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("tree_uri", null).apply()
                        true
                    }
                }
                if (PrefsUtil.downloaderType == 0) {
                    preferenceScreen.findPreference<Preference>(keyMaxParallelDownloads)?.isEnabled = false
                    preferenceScreen.findPreference<Preference>(keyBufferSize)?.isEnabled = true
                } else {
                    preferenceScreen.findPreference<Preference>(keyMaxParallelDownloads)?.isEnabled = true
                    preferenceScreen.findPreference<Preference>(keyBufferSize)?.isEnabled = false
                }
                preferenceScreen.findPreference<Preference>("downloader_type")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    if (newValue == "0") {
                        preferenceScreen.findPreference<Preference>(keyMaxParallelDownloads)?.isEnabled = false
                        preferenceScreen.findPreference<Preference>(keyBufferSize)?.isEnabled = true
                    } else {
                        preferenceScreen.findPreference<Preference>(keyMaxParallelDownloads)?.isEnabled = true
                        preferenceScreen.findPreference<Preference>(keyBufferSize)?.isEnabled = false
                    }
                    true
                }
                preferenceScreen.findPreference<Preference>("default_useragent")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(1000)
                        uaChangeListener?.onUAChange()
                    }
                    true
                }
                preferenceScreen.findPreference<Preference>("theme_option")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    AppCompatDelegate.setDefaultNightMode(((newValue as? String) ?: "0").toInt())
                    PreferenceManager.getDefaultSharedPreferences(safeContext).edit().putString("theme_value", newValue.toString()).apply()
                    WEmisionProvider.update(safeContext)
                    activity?.recreate()
                    true
                }
                preferenceScreen.findPreference<SwitchPreference>("ads_enabled")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    if (newValue == false && !BuildConfig.DEBUG) {
                        context?.let { FirestoreManager.doSignOut(it) }
                        Backups.type = Backups.Type.NONE
                    }
                    true
                }
                preferenceScreen.findPreference<SwitchPreference>("family_friendly")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                    if (newValue == true) {
                        activity?.let {
                            MaterialDialog(it).safeShow {
                                title(text = "Configurar contraseña")
                                input { _, input ->
                                    doOnUI {
                                        val crypted = input.toString().encrypt(BuildConfig.CIPHER_PWD)
                                        PrefsUtil.ffPass = crypted
                                        val file = ffFile
                                        if (!file.exists())
                                            file.createNewFile()
                                        file.writeText(crypted)
                                        doAsync { CacheDB.INSTANCE.animeDAO().nukeEcchi() }
                                    }
                                }
                                onCancel {
                                    PrefsUtil.isFamilyFriendly = false
                                    preferenceScreen.findPreference<SwitchPreference>("family_friendly")?.isChecked = false
                                }
                            }
                        }
                    } else {
                        activity?.let {
                            MaterialDialog(it).safeShow {
                                title(text = "Ingresa contraseña")
                                input { _, input ->
                                    doOnUI {
                                        val file = ffFile
                                        if (file.exists()) {
                                            val text = file.readText()
                                            val decrypt = text.decrypt(BuildConfig.CIPHER_PWD)
                                            if (decrypt == input.toString()) {
                                                PrefsUtil.ffPass = ""
                                                file.delete()
                                                DirectoryUpdateService.run(context)
                                            } else {
                                                PrefsUtil.isFamilyFriendly = true
                                                preferenceScreen.findPreference<SwitchPreference>("family_friendly")?.isChecked = true
                                                Toaster.toast("Contraseña incorrecta")
                                            }
                                        } else {
                                            val decrypt = PrefsUtil.ffPass.decrypt(BuildConfig.CIPHER_PWD)
                                            if (decrypt != input.toString()) {
                                                file.createNewFile()
                                                file.writeText(decrypt)
                                                PrefsUtil.isFamilyFriendly = true
                                                preferenceScreen.findPreference<SwitchPreference>("family_friendly")?.isChecked = true
                                                Toaster.toast("Contraseña incorrecta")
                                            } else {
                                                PrefsUtil.ffPass = ""
                                                DirectoryUpdateService.run(context)
                                            }
                                        }
                                    }
                                }
                                onCancel {
                                    PrefsUtil.isFamilyFriendly = true
                                    preferenceScreen.findPreference<SwitchPreference>("family_friendly")?.isChecked = true
                                }
                            }
                        }
                    }
                    true
                }
                preferenceScreen.findPreference<Preference>("recents_time")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    preferenceScreen.findPreference<Preference>("notify_favs")?.isEnabled = "0" != newValue
                    RecentsWork.reSchedule(newValue.toString().toInt() * 15)
                    true
                }
                preferenceScreen.findPreference<Preference>("dir_update_time")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    DirUpdateWork.reSchedule(newValue.toString().toInt() * 15)
                    true
                }
                preferenceScreen.findPreference<Preference>("dir_update")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    try {
                        if (!DirectoryUpdateService.isRunning && !DirectoryService.isRunning)
                            DirectoryUpdateService.run(App.context)
                        else if (DirectoryUpdateService.isRunning)
                            Toaster.toast("Ya se esta actualizando")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    false
                }
                if (!canGroupNotifications)
                    preferenceScreen.removePreference(preferenceScreen.findPreference("group_notifications"))
                preferenceScreen.findPreference<Preference>("dir_destroy")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    try {
                        if (!DirectoryUpdateService.isRunning && !DirectoryService.isRunning)
                            activity?.let { safe ->
                                MaterialDialog(safe).safeShow {
                                    message(text = "¿Desea recrear el directorio?")
                                    positiveButton(text = "continuar") {
                                        doAsync {
                                            CacheDB.INSTANCE.animeDAO().nuke()
                                            PrefsUtil.isDirectoryFinished = false
                                            DirectoryService.run(safeContext)
                                        }
                                    }
                                    negativeButton(text = "cancelar")
                                }
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
                        preferenceScreen.findPreference<Preference>(keyThemeColor)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                            startActivity(Intent(activity, Main::class.java).putExtra("start_position", 3))
                            activity?.finish()
                            true
                        }
                    EAHelper.phase == 0 -> {
                        val category = preferenceScreen.findPreference("category_design") as? PreferenceCategory
                        category?.removePreference(preferenceScreen.findPreference(keyThemeColor))
                        val pref = Preference(activity)
                        pref.title = "Color de tema"
                        pref.summary = "Resuelve el secreto para desbloquear"
                        pref.setIcon(R.drawable.ic_palette)
                        pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            Toaster.toast(EAHelper.eaMessage)
                            true
                        }
                        category?.addPreference(pref)
                    }
                    else -> {
                        preferenceScreen.findPreference<Preference>(keyThemeColor)?.summary = "Resuelve el secreto para desbloquear"
                        preferenceScreen.findPreference<Preference>(keyThemeColor)?.isEnabled = false
                    }
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this@ConfigurationFragment.context))
                    (preferenceScreen.findPreference(keyArchievementsPermissions) as? SwitchPreference)?.apply {
                        isChecked = true
                        isEnabled = false
                    }
                else if (!Settings.canDrawOverlays(this@ConfigurationFragment.context)) {
                    (preferenceScreen.findPreference(keyArchievementsPermissions) as? SwitchPreference)?.apply {
                        isChecked = false
                        isEnabled = true
                    }
                }
                preferenceScreen.findPreference<Preference>(keyArchievementsPermissions)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).setData(Uri.parse("package:${getPackage()}")), 5879)
                    } catch (e: ActivityNotFoundException) {
                        Toaster.toast("No se pudo abrir la configuracion")
                    }
                    true
                }
                preferenceScreen.findPreference<Preference>("hide_chaps")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    if (!FileAccessHelper.NOMEDIA_CREATING) {
                        FileAccessHelper.checkNoMedia(newValue as? Boolean == true)
                        true
                    } else {
                        (preferenceScreen.findPreference("hide_chaps") as? SwitchPreference)?.isChecked = newValue as? Boolean != true
                        false
                    }
                }
                preferenceScreen.findPreference<Preference>("max_parallel_downloads")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    DownloadManager.setParallelDownloads(newValue as? String)
                    true
                }
                preferenceScreen.findPreference<SwitchPreference>("remember_server")?.apply {
                    val lastServer = PrefsUtil.lastServer
                    if (lastServer.isNull())
                        isEnabled = false
                    else {
                        summary = lastServer
                        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                            if (newValue as? Boolean == false) {
                                PrefsUtil.lastServer = null
                                preference.summary = null
                                preference.isEnabled = false
                            }
                            true
                        }
                    }
                }
                if (BuildConfig.DEBUG) {
                    preferenceScreen.findPreference<Preference>("reset_recents")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        doAsync {
                            CacheDB.INSTANCE.recentsDAO().clear()
                            RecentsWork.run()
                        }
                        true
                    }
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ListView>(android.R.id.list)?.let {
            ViewCompat.setNestedScrollingEnabled(it, true)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            preferenceScreen.findPreference<Preference>(keyDaynigthPermission)?.isEnabled = false
        } else {
            preferenceManager.sharedPreferences.edit().putBoolean(keyDaynigthPermission, false).apply()
            (preferenceScreen.findPreference(keyDaynigthPermission) as? SwitchPreference)?.isChecked = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        noCrash {
            if (requestCode == FileAccessHelper.SD_REQUEST && resultCode == Activity.RESULT_OK) {
                val validation = FileAccessHelper.isUriValid(data?.data)
                if (!validation.isValid) {
                    Toaster.toast("Directorio invalido: $validation")
                    FileAccessHelper.openTreeChooser(this)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    preferenceScreen.findPreference<Preference>("download_type_q")?.summary = PrefsUtil.storageType
            } else if (requestCode == 4784 && resultCode == Activity.RESULT_OK) {
                if (!FileAccessHelper.toneFile.exists())
                    FileAccessHelper.toneFile.createNewFile()
                FileUtil.moveFile(
                        safeContext.contentResolver,
                        data?.data,
                        FileOutputStream(FileAccessHelper.toneFile), false)
                        .observe(this, Observer {
                            try {
                                if (it != null) {
                                    if (it.second) {
                                        if (it.first == -1) {
                                            FileAccessHelper.toneFile.safeDelete()
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
            } else if (requestCode == 5879) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context))
                    (preferenceScreen.findPreference("achievements_permissions") as? SwitchPreference)?.apply {
                        isChecked = true
                        isEnabled = false
                    }
                else
                    (preferenceScreen.findPreference("achievements_permissions") as? SwitchPreference)?.apply {
                        isChecked = false
                        isEnabled = true
                    }
            }
        }
    }

    interface UAChangeListener {
        fun onUAChange()
    }
}
