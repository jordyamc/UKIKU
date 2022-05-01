package knf.kuma.preferences

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.preference.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.Main
import knf.kuma.R
import knf.kuma.ads.AdsUtils
import knf.kuma.backup.Backups
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.commons.*
import knf.kuma.custom.PreferenceFragmentCompat
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirManager
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
import org.jetbrains.anko.support.v4.toast
import xdroid.toaster.Toaster
import java.io.FileOutputStream
import kotlin.contracts.ExperimentalContracts


class ConfigurationFragment : PreferenceFragmentCompat() {

    companion object {
        private const val keyCustomTone = "custom_tone"
        private const val keyAutoBackup = "auto_backup"
        private const val keyMaxParallelDownloads = "max_parallel_downloads"
        private const val keyBufferSize = "buffer_size"
        private const val keyThemeColor = "theme_color"
        private const val keyAchievementsPermissions = "achievements_permissions"
        private const val keyAdsEnabled = "ads_enabled_new"
    }

    private var uaChangeListener: UAChangeListener? = null

    override fun onAttach(activity: Activity) {
        uaChangeListener = activity as? UAChangeListener
        super.onAttach(activity)
    }

    @OptIn(ExperimentalContracts::class)
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (activity != null && context != null)
            doOnUI {
                addPreferencesFromResource(R.xml.preferences)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    preferenceScreen.findPreference<Preference>(keyCustomTone)?.summary = "Abrir configuración"
                else if (FileAccessHelper.toneFile.exists())
                    preferenceScreen.findPreference<Preference>(keyCustomTone)?.summary = "Personalizado"
                if (!DesignUtils.isFlat)
                    preferenceScreen.findPreference<ListPreference>("recentActionType")?.isVisible = false
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
                                                preferenceScreen.findPreference<Preference>(
                                                    keyAutoBackup
                                                )?.summary = "%s"
                                            else
                                                preferenceScreen.findPreference<Preference>(
                                                    keyAutoBackup
                                                )?.summary = "Solo " + autoBackupObject.name
                                            if (autoBackupObject.value == null)
                                                GlobalScope.launch(Dispatchers.Main) {
                                                    Backups.createService()?.backup(
                                                        AutoBackupObject(App.context),
                                                        Backups.keyAutoBackup
                                                    )
                                                    preferenceScreen.findPreference<Preference>(
                                                        keyAutoBackup
                                                    )?.summary = "%s"
                                                }
                                            else
                                                preferenceManager.sharedPreferences?.edit()
                                                    ?.putString(
                                                        keyAutoBackup,
                                                        autoBackupObject.value
                                                    )?.apply()
                                        } else {
                                            preferenceScreen.findPreference<Preference>(keyAutoBackup)?.summary = "%s (NE)"
                                        }
                                        preferenceScreen.findPreference<Preference>(keyAutoBackup)?.isEnabled = true
                                    } catch (e: Exception) {
                                        FirebaseCrashlytics.getInstance().recordException(e)
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
                        Toaster.toastLong("Por favor selecciona la raiz del almacenamiento")
                        true
                    }
                } else {
                    preferenceScreen.removePreferenceRecursively("download_type_q")
                    preferenceScreen.findPreference<Preference>("download_type")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                        if (newValue == "1" && !FileAccessHelper.canDownload(
                                this@ConfigurationFragment,
                                newValue as? String
                            )
                        )
                            Toaster.toast("Por favor selecciona la raiz de tu SD")
                        else
                            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                                .putString("tree_uri", null).apply()
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
                preferenceScreen.findPreference<SwitchPreference>("ads_enabled_new")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    if (BuildConfig.DEBUG || PrefsUtil.isSubscriptionEnabled) return@OnPreferenceChangeListener true
                    if (newValue == false) {
                        context?.let { FirestoreManager.doSignOut(it) }
                        Backups.type = Backups.Type.NONE
                    }
                    true
                }
                preferenceScreen.findPreference<SwitchPreference>("family_friendly_enabled")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                    if (newValue == true) {
                        activity?.let {
                            MaterialDialog(it).safeShow {
                                title(text = "Configurar contraseña")
                                input { _, inputText ->
                                    MaterialDialog(it).safeShow {
                                        title(text = "Repetir contraseña")
                                        input { _, input ->
                                            if (input.toString() == inputText.toString())
                                                doOnUI(onLog = {
                                                    PrefsUtil.isFamilyFriendly = false
                                                    preferenceScreen.findPreference<SwitchPreference>("family_friendly_enabled")?.isChecked = false
                                                    toast("Error al encriptar")
                                                }) {
                                                    val encrypted = input.toString().encryptOrThrow()
                                                    check(encrypted.decrypt() == input.toString())
                                                    PrefsUtil.ffPass = encrypted
                                                    val file = ffFile
                                                    file.parentFile?.mkdirs()
                                                    if (!file.exists())
                                                        file.createNewFile()
                                                    file.writeText(encrypted)
                                                    doAsync { CacheDB.INSTANCE.animeDAO().nukeEcchi() }
                                                }
                                            else {
                                                toast("Las contraseñas no coinciden")
                                                PrefsUtil.isFamilyFriendly = false
                                                preferenceScreen.findPreference<SwitchPreference>("family_friendly_enabled")?.isChecked = false
                                            }
                                        }
                                        getInputLayout().boxBackgroundColor = Color.TRANSPARENT
                                        getInputField().setBackgroundColor(Color.TRANSPARENT)
                                        onCancel {
                                            PrefsUtil.isFamilyFriendly = false
                                            preferenceScreen.findPreference<SwitchPreference>("family_friendly_enabled")?.isChecked = false
                                        }
                                    }
                                }
                                getInputLayout().boxBackgroundColor = Color.TRANSPARENT
                                getInputField().setBackgroundColor(Color.TRANSPARENT)
                                onCancel {
                                    PrefsUtil.isFamilyFriendly = false
                                    preferenceScreen.findPreference<SwitchPreference>("family_friendly_enabled")?.isChecked = false
                                }
                            }
                        }
                    } else {
                        activity?.let {
                            MaterialDialog(it).safeShow {
                                title(text = "Ingresa contraseña")
                                input { _, input ->
                                    doOnUI(onLog = {
                                        PrefsUtil.isFamilyFriendly = true
                                        preferenceScreen.findPreference<SwitchPreference>("family_friendly_enabled")?.isChecked = true
                                        toast("Error al desencriptar")
                                    }) {
                                        val file = ffFile
                                        if (file.exists() && !admFile.exists()) {
                                            val text = file.readText()
                                            val decrypt = text.decrypt()
                                            if (decrypt == input.toString()) {
                                                PrefsUtil.ffPass = ""
                                                file.delete()
                                                DirectoryUpdateService.run(context)
                                            } else {
                                                PrefsUtil.isFamilyFriendly = true
                                                preferenceScreen.findPreference<SwitchPreference>("family_friendly_enabled")?.isChecked = true
                                                Toaster.toast("Contraseña incorrecta")
                                            }
                                        } else if (admFile.exists()) {
                                            PrefsUtil.ffPass = ""
                                            file.delete()
                                            DirectoryUpdateService.run(context)
                                        } else {
                                            if (PrefsUtil.ffPass != "") {
                                                val decrypt = PrefsUtil.ffPass.decrypt()
                                                if (decrypt == null || decrypt != input.toString()) {
                                                    file.createNewFile()
                                                    file.writeText(PrefsUtil.ffPass)
                                                    PrefsUtil.isFamilyFriendly = true
                                                    preferenceScreen.findPreference<SwitchPreference>("family_friendly_enabled")?.isChecked = true
                                                    Toaster.toast("Contraseña incorrecta")
                                                } else {
                                                    PrefsUtil.ffPass = ""
                                                    DirectoryUpdateService.run(context)
                                                }
                                            }
                                        }
                                    }
                                }
                                getInputLayout().boxBackgroundColor = Color.TRANSPARENT
                                getInputField().setBackgroundColor(Color.TRANSPARENT)
                                onCancel {
                                    PrefsUtil.isFamilyFriendly = true
                                    preferenceScreen.findPreference<SwitchPreference>("family_friendly_enabled")?.isChecked = true
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
                preferenceScreen.findPreference<Preference>("security_blocking_firestore")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    FirestoreManager.start()
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
                    preferenceScreen.removePreference(preferenceScreen.findPreference("group_notifications")!!)
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
                                            DirManager.checkPreDir()
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
                when (EAHelper.phase) {
                    4 ->
                        preferenceScreen.findPreference<Preference>(keyThemeColor)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                            startActivity(Intent(activity, Main::class.java).putExtra("start_position", 3))
                            activity?.finish()
                            true
                        }
                    0 -> {
                        val category =
                            preferenceScreen.findPreference("category_design") as? PreferenceCategory
                        category?.removePreference(preferenceScreen.findPreference(keyThemeColor)!!)
                        val pref = Preference(requireContext())
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
                    (preferenceScreen.findPreference(keyAchievementsPermissions) as? SwitchPreference)?.apply {
                        isChecked = true
                        isEnabled = false
                    }
                else if (!Settings.canDrawOverlays(this@ConfigurationFragment.context)) {
                    (preferenceScreen.findPreference(keyAchievementsPermissions) as? SwitchPreference)?.apply {
                        isChecked = false
                        isEnabled = true
                    }
                }
                preferenceScreen.findPreference<Preference>(keyAchievementsPermissions)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
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
                preferenceScreen.findPreference<SwitchPreference>(keyAdsEnabled)?.apply {
                    isChecked = PrefsUtil.isAdsEnabled
                    isEnabled = PrefsUtil.isSubscriptionEnabled || !AdsUtils.remoteConfigs.getBoolean("ads_forced")
                    if (!isEnabled)
                        summary = "Estas en una prueba temporal!"
                    setOnPreferenceChangeListener { _, newValue ->
                        preferenceScreen.findPreference<Preference>("ads_settings")?.isEnabled = newValue == true
                        true
                    }
                }
                preferenceScreen.findPreference<Preference>("ads_settings")?.apply {
                    isEnabled = PrefsUtil.isAdsEnabled
                    setOnPreferenceClickListener {
                        startActivity(Intent(requireContext(), AdsPreferenceActivity::class.java))
                        true
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
