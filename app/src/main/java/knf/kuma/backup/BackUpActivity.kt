package knf.kuma.backup

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.dropbox.core.android.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.backup.framework.BackupService
import knf.kuma.backup.framework.DropBoxService
import knf.kuma.backup.framework.LocalService
import knf.kuma.commons.EAHelper
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.admFile
import knf.kuma.commons.noCrash
import knf.kuma.commons.safeShow
import knf.kuma.commons.showSnackbar
import knf.kuma.custom.GenericActivity
import knf.kuma.custom.SyncItemView
import knf.kuma.databinding.ActivityLoginBinding
import kotlin.math.max

class BackUpActivity : GenericActivity(), SyncItemView.OnClick {
    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private val syncItems: MutableList<SyncItemView> by lazy {
        with(binding.layButtons) {
            arrayListOf(syncFavs, syncHistory, syncFollowing, syncSeen, syncSeenNew)
        }
    }
    private var service: BackupService? = null
    private var waitingLogin = false

    private val backColor: Int
        @ColorInt
        get() {
            return when (Backups.type) {
                Backups.Type.NONE -> ContextCompat.getColor(this, android.R.color.transparent)
                Backups.Type.LOCAL -> ContextCompat.getColor(this, EAHelper.getThemeColorLight())
                Backups.Type.DROPBOX -> ContextCompat.getColor(this, R.color.dropbox)
                Backups.Type.FIRESTORE -> ContextCompat.getColor(this, R.color.firestore)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!resources.getBoolean(R.bool.isTablet))
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(binding.root)
        service = Backups.createService()
        binding.layMain.loginDropbox.setOnClickListener { onDropBoxLogin() }
        if (!PrefsUtil.isAdsEnabled && !BuildConfig.DEBUG && !admFile.exists() && !PrefsUtil.isSubscriptionEnabled) {
            binding.layMain.loginFirestore.isEnabled = false
            binding.layMain.adsRequired.visibility = View.VISIBLE
        } else if (PrefsUtil.isAdsEnabled && Network.isAdsBlocked && !BuildConfig.DEBUG && !admFile.exists() && !PrefsUtil.isSubscriptionEnabled) {
            binding.layMain.loginFirestore.isEnabled = false
            binding.layMain.adsRequired.text = "Anuncios bloqueados por host"
            binding.layMain.adsRequired.visibility = View.VISIBLE
        } else if (!PrefsUtil.isSecurityUpdated && PrefsUtil.spProtectionEnabled && PrefsUtil.spErrorType != null) {
            binding.layMain.loginFirestore.isEnabled = false
            binding.layMain.adsRequired.text =
                "Proveedor de seguridad no pudo ser actualizado (${PrefsUtil.spErrorType})"
            binding.layMain.adsRequired.visibility = View.VISIBLE
        }
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            FirestoreManager.start()
            binding.layMain.loginFirestore.setOnClickListener { onFirestoreLogin() }
        } else {
            binding.layMain.loginFirestore.isEnabled = false
            binding.layMain.adsRequired.text = "Google Play Services no disponibles"
            binding.layMain.adsRequired.visibility = View.VISIBLE
        }
        binding.layMain.loginLocal.setOnClickListener { onLocalLogin() }
        binding.layButtons.logOut.setOnClickListener { onLogOut() }
        binding.layFirestore.logOutFirestore.setOnClickListener { onLogOut() }
        when {
            service?.isLoggedIn == true -> {
                setState(true)
                showColor(savedInstanceState == null)
                initSyncButtons()
            }
            FirestoreManager.isLoggedIn -> {
                setState(true)
                showColor(savedInstanceState == null)
                initFirestoreSync()
            }
            else -> setState(false)
        }
    }

    private fun initSyncButtons() {
        for (itemView in syncItems) {
            itemView.init(service, this)
        }
    }

    private fun initFirestoreSync() {
        AchievementManager.onBackup()
        binding.layFirestore.staticSyncAchievements.suscribe(this, FirestoreManager.achievementsLiveData)
        binding.layFirestore.staticSyncEA.suscribe(this, FirestoreManager.eaLiveData)
        binding.layFirestore.staticSyncFavs.suscribe(this, FirestoreManager.favsLiveData)
        binding.layFirestore.staticSyncGenres.suscribe(this, FirestoreManager.genresLiveData)
        binding.layFirestore.staticSyncHistory.suscribe(this, FirestoreManager.historyLiveData)
        binding.layFirestore.staticSyncQueue.suscribe(this, FirestoreManager.queueLiveData)
        binding.layFirestore.staticSyncSeeing.suscribe(this, FirestoreManager.seeingLiveData)
        binding.layFirestore.staticSyncSeen.suscribe(this, FirestoreManager.seenLiveData)
    }

    private fun clearSyncButtons() {
        for (itemView in syncItems) {
            itemView.clear()
        }
    }

    private fun onDropBoxLogin() {
        waitingLogin = true
        service = DropBoxService().also { it.logIn() }
    }

    private fun onFirestoreLogin() {
        FirestoreManager.doLogin(this)
    }

    private fun onLocalLogin() {
        MaterialDialog(this).safeShow {
            message(text = "Los datos se quedarán en la memoria, no se podrá sincronizar datos entre dispositivos, usar este método?")
            positiveButton(text = "usar") {
                service = LocalService().also {
                    it.start()
                    it.logIn()
                }
                onLogin()
            }
            negativeButton(text = "cancelar")
        }
    }

    override fun onAction(syncItemView: SyncItemView, id: String, isBackup: Boolean) {
        noCrash {
            if (isBackup)
                Backups.backup(binding.colorChanger, service, id) {
                    noCrash {
                        if (it == null)
                            binding.colorChanger.showSnackbar("Error al respaldar")
                        syncItemView.enableBackup(it, this@BackUpActivity)
                    }
                }
            else
                Backups.restoreDialog(this, binding.colorChanger, id, syncItemView.backupObj)
        }
    }

    private fun onLogOut() {
        MaterialDialog(this).safeShow {
            message(text = "Los datos no respaldados podrian ser perdidos al borrar la app, ¿desea continuar?")
            positiveButton(text = "continuar") {
                if (Backups.type == Backups.Type.FIRESTORE) {
                    FirestoreManager.doSignOut(this@BackUpActivity)
                    Backups.type = Backups.Type.NONE
                    revertColor()
                    setState(false)
                } else {
                    PreferenceManager.getDefaultSharedPreferences(this@BackUpActivity).edit().putString("auto_backup", "0").apply()
                    service?.logOut()
                    service = null
                    Backups.type = Backups.Type.NONE
                    revertColor()
                    setState(false)
                    clearSyncButtons()
                }
            }
            negativeButton(text = "cancelar")
        }
    }

    private fun onLogin() {
        if (service?.isLoggedIn == true || FirestoreManager.isLoggedIn) {
            setState(true)
            showColor(true)
            initSyncButtons()
        } else if (waitingLogin) {
            binding.colorChanger.showSnackbar("Error al iniciar sesión")
        }
        waitingLogin = false
    }

    private fun showColor(animate: Boolean) {
        binding.colorChanger.post {
            try {
                binding.colorChanger.setBackgroundColor(backColor)
                if (animate) {
                    val bounds = Rect()
                    binding.colorChanger.getDrawingRect(bounds)
                    val centerX = bounds.centerX()
                    val centerY = bounds.centerY()
                    val finalRadius = max(bounds.width(), bounds.height())
                    val animator = ViewAnimationUtils.createCircularReveal(binding.colorChanger, centerX, centerY, 0f, finalRadius.toFloat())
                    animator.duration = 1000
                    animator.interpolator = AccelerateDecelerateInterpolator()
                    binding.colorChanger.visibility = View.VISIBLE
                    animator.start()
                } else {
                    binding.colorChanger.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun revertColor() {
        binding.colorChanger.post {
            val bounds = Rect()
            binding.colorChanger.getDrawingRect(bounds)
            val centerX = bounds.centerX()
            val centerY = bounds.centerY()
            val finalRadius = max(bounds.width(), bounds.height())
            val animator = ViewAnimationUtils.createCircularReveal(binding.colorChanger, centerX, centerY, finalRadius.toFloat(), 0f)
            animator.duration = 1000
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    binding.colorChanger.visibility = View.INVISIBLE
                }

                override fun onAnimationCancel(animation: Animator) {

                }

                override fun onAnimationRepeat(animation: Animator) {

                }
            })
            animator.start()
        }
    }

    private fun setState(isLoggedIn: Boolean) {
        runOnUiThread {
            binding.layMain.root.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
            when (Backups.type) {
                Backups.Type.LOCAL, Backups.Type.DROPBOX -> {
                    binding.layButtons.root.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
                    binding.layFirestore.root.visibility = View.GONE
                }
                Backups.Type.FIRESTORE -> {
                    binding.layFirestore.root.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
                    binding.layButtons.root.visibility = View.GONE
                }
                Backups.Type.NONE -> {
                    binding.layButtons.root.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
                    binding.layFirestore.root.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingLogin) {
            val token = Auth.getOAuth2Token()
            if (service is DropBoxService && service?.logIn(token) == true) {
                Backups.type = Backups.Type.DROPBOX
            }
            onLogin()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (FirestoreManager.handleLogin(this, requestCode, resultCode, data)) {
            initFirestoreSync()
        }
        onLogin()
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, BackUpActivity::class.java))
        }
    }
}
