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
import com.google.firebase.analytics.FirebaseAnalytics
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.backup.framework.BackupService
import knf.kuma.backup.framework.DropBoxService
import knf.kuma.backup.framework.LocalService
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import knf.kuma.custom.SyncItemView
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login_buttons.*
import kotlinx.android.synthetic.main.activity_login_firestore.*
import kotlinx.android.synthetic.main.activity_login_main.*
import kotlin.contracts.ExperimentalContracts
import kotlin.math.max

class BackUpActivity : GenericActivity(), SyncItemView.OnClick {
    private val syncItems: MutableList<SyncItemView> by lazy { arrayListOf(sync_favs, sync_history, sync_following, sync_seen, sync_seen_new) }
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

    @OptIn(ExperimentalContracts::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!resources.getBoolean(R.bool.isTablet))
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_login)
        service = Backups.createService()
        login_dropbox.setOnClickListener { onDropBoxLogin() }
        if (!PrefsUtil.isAdsEnabled && !BuildConfig.DEBUG && !admFile.exists() && !PrefsUtil.isSubscriptionEnabled) {
            login_firestore.isEnabled = false
            ads_required.visibility = View.VISIBLE
        } else if (PrefsUtil.isAdsEnabled && Network.isAdsBlocked && !BuildConfig.DEBUG && !admFile.exists() && !PrefsUtil.isSubscriptionEnabled) {
            login_firestore.isEnabled = false
            ads_required.text = "Anuncios bloqueados por host"
            ads_required.visibility = View.VISIBLE
        } else if (!PrefsUtil.isSecurityUpdated && PrefsUtil.spProtectionEnabled && PrefsUtil.spErrorType != null) {
            login_firestore.isEnabled = false
            ads_required.text =
                "Proveedor de seguridad no pudo ser actualizado (${PrefsUtil.spErrorType})"
            ads_required.visibility = View.VISIBLE
        }
        FirestoreManager.start()
        login_firestore.setOnClickListener { onFirestoreLogin() }
        login_local.setOnClickListener { onLocalLogin() }
        logOut.setOnClickListener { onLogOut() }
        logOutFirestore.setOnClickListener { onLogOut() }
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
        staticSyncAchievements.suscribe(this, FirestoreManager.achievementsLiveData)
        staticSyncEA.suscribe(this, FirestoreManager.eaLiveData)
        staticSyncFavs.suscribe(this, FirestoreManager.favsLiveData)
        staticSyncGenres.suscribe(this, FirestoreManager.genresLiveData)
        staticSyncHistory.suscribe(this, FirestoreManager.historyLiveData)
        staticSyncQueue.suscribe(this, FirestoreManager.queueLiveData)
        staticSyncSeeing.suscribe(this, FirestoreManager.seeingLiveData)
        staticSyncSeen.suscribe(this, FirestoreManager.seenLiveData)
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
                Backups.backup(colorChanger, service, id) {
                    noCrash {
                        if (it == null)
                            colorChanger.showSnackbar("Error al respaldar")
                        syncItemView.enableBackup(it, this@BackUpActivity)
                    }
                }
            else
                Backups.restoreDialog(this, colorChanger, id, syncItemView.backupObj)
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
            colorChanger.showSnackbar("Error al iniciar sesión")
        }
        waitingLogin = false
    }

    private fun showColor(animate: Boolean) {
        colorChanger.post {
            try {
                colorChanger.setBackgroundColor(backColor)
                if (animate) {
                    val bounds = Rect()
                    colorChanger.getDrawingRect(bounds)
                    val centerX = bounds.centerX()
                    val centerY = bounds.centerY()
                    val finalRadius = max(bounds.width(), bounds.height())
                    val animator = ViewAnimationUtils.createCircularReveal(colorChanger, centerX, centerY, 0f, finalRadius.toFloat())
                    animator.duration = 1000
                    animator.interpolator = AccelerateDecelerateInterpolator()
                    colorChanger.visibility = View.VISIBLE
                    animator.start()
                } else {
                    colorChanger.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun revertColor() {
        colorChanger.post {
            val bounds = Rect()
            colorChanger.getDrawingRect(bounds)
            val centerX = bounds.centerX()
            val centerY = bounds.centerY()
            val finalRadius = max(bounds.width(), bounds.height())
            val animator = ViewAnimationUtils.createCircularReveal(colorChanger, centerX, centerY, finalRadius.toFloat(), 0f)
            animator.duration = 1000
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    colorChanger?.visibility = View.INVISIBLE
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
            lay_main?.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
            when (Backups.type) {
                Backups.Type.LOCAL, Backups.Type.DROPBOX -> {
                    lay_buttons?.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
                    lay_firestore?.visibility = View.GONE
                }
                Backups.Type.FIRESTORE -> {
                    lay_firestore?.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
                    lay_buttons?.visibility = View.GONE
                }
                Backups.Type.NONE -> {
                    lay_buttons?.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
                    lay_firestore?.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
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
                FirebaseAnalytics.getInstance(App.context).logEvent(
                    FirebaseAnalytics.Event.LOGIN,
                    Bundle().apply { putString(FirebaseAnalytics.Param.METHOD, "Dropbox") })
            }
            onLogin()
        }
    }


    @OptIn(ExperimentalContracts::class)
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
