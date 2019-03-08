package knf.kuma.backup

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.LoginEvent
import com.dropbox.core.android.Auth
import knf.kuma.R
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.noCrash
import knf.kuma.commons.safeShow
import knf.kuma.commons.showSnackbar
import knf.kuma.custom.SyncItemView
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login_buttons.*
import kotlinx.android.synthetic.main.activity_login_main.*

class BackUpActivity : AppCompatActivity(), BUUtils.LoginInterface, SyncItemView.OnClick {
    private val syncItems: MutableList<SyncItemView> by lazy { arrayListOf(sync_favs, sync_history, sync_following, sync_seen) }
    private var waitingLogin = false

    private val backColor: Int
        @ColorInt
        get() {
            return when (BUUtils.type) {
                BUUtils.BUType.LOCAL -> ContextCompat.getColor(this, android.R.color.transparent)
                BUUtils.BUType.DROPBOX -> ContextCompat.getColor(this, R.color.dropbox)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!resources.getBoolean(R.bool.isTablet))
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_login)
        BUUtils.init(this, savedInstanceState == null)
        login_dropbox.setOnClickListener { onDropBoxLogin() }
        logOut.setOnClickListener { onLogOut() }
        if (BUUtils.isLogedIn) {
            setState(true)
            showColor(savedInstanceState == null)
            initSyncButtons()
        } else {
            setState(false)
        }
    }

    private fun initSyncButtons() {
        for (itemView in syncItems) {
            itemView.init(this)
        }
    }

    private fun clearSyncButtons() {
        for (itemView in syncItems) {
            itemView.clear()
        }
    }

    private fun onDropBoxLogin() {
        waitingLogin = true
        BUUtils.startClient(BUUtils.BUType.DROPBOX, false)
    }

    override fun onAction(syncItemView: SyncItemView, id: String, isBackup: Boolean) {
        noCrash {
            if (isBackup)
                BUUtils.backup(colorChanger, id, object : BUUtils.BackupInterface {
                    override fun onResponse(backupObject: BackupObject<*>?) {
                        noCrash {
                            if (backupObject == null)
                                colorChanger.showSnackbar("Error al respaldar")
                            syncItemView.enableBackup(backupObject, this@BackUpActivity)
                        }
                    }
                })
            else
                BUUtils.restoreDialog(colorChanger, id, syncItemView.bakup)
        }
    }

    private fun onLogOut() {
        MaterialDialog(this).safeShow {
            message(text = "Los datos se quedaran en el dispositivo, ¿desea continuar?")
            positiveButton(text = "continuar") {
                PreferenceManager.getDefaultSharedPreferences(this@BackUpActivity).edit().putString("auto_backup", "0").apply()
                BUUtils.logOut()
                revertColor()
                setState(false)
                clearSyncButtons()
            }
            negativeButton(text = "cancelar")
        }
    }

    override fun onLogin() {
        if (BUUtils.isLogedIn) {
            setState(true)
            showColor(true)
            initSyncButtons()
            Answers.getInstance().logLogin(LoginEvent().putMethod("Dropbox"))
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
                    val finalRadius = Math.max(bounds.width(), bounds.height())
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
            val finalRadius = Math.max(bounds.width(), bounds.height())
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

    private fun setState(isLogedIn: Boolean) {
        runOnUiThread {
            lay_main?.visibility = if (isLogedIn) View.GONE else View.VISIBLE
            lay_buttons?.visibility = if (isLogedIn) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingLogin) {
            val token = Auth.getOAuth2Token()
            if (token != null)
                BUUtils.type = BUUtils.BUType.DROPBOX
            BUUtils.setDropBoxClient(token)
        }
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, BackUpActivity::class.java))
        }
    }
}
