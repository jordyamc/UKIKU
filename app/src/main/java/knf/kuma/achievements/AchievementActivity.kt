package knf.kuma.achievements

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.custom.AchievementUnlocked
import knf.kuma.database.CacheDB
import knf.kuma.pojos.Achievement
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.text.NumberFormat
import java.util.*

class AchievementActivity : AppCompatActivity() {

    private val toolbar: Toolbar by bind(R.id.toolbar)
    private val tabs: TabLayout by bind(R.id.tabs)
    private val pager: ViewPager by bind(R.id.pager)
    private val progress: CircularProgressBar by bind(R.id.progress)
    private val level: TextView by bind(R.id.level)
    private val countDown: TextView by bind(R.id.countdown)
    private val cardView: CardView by bind(R.id.sheet)
    private val icon: ImageView by bind(R.id.achievement_icon)
    private val xpIndicator: TextView by bind(R.id.achievement_xp)
    private val state: TextView by bind(R.id.achievement_state)
    private val progressIndicator: View by bind(R.id.progress_indicator)
    private val progressBar: ProgressBar by bind(R.id.progress_bar)
    private val progressText: TextView by bind(R.id.progress_text)
    private val progressIndText: TextView by bind(R.id.progress_ind_text)
    private val name: TextView by bind(R.id.achievement_name)
    private val description: TextView by bind(R.id.achievement_description)
    private lateinit var bottomSheet: BottomSheetBehavior<CardView>

    private val levelCalculator = LevelCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievement_profile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.title = "Logros"
        toolbar.setNavigationOnClickListener { onBackPressed() }
        bottomSheet = BottomSheetBehavior.from(cardView)
        bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
        pager.offscreenPageLimit = 2
        pager.adapter = AchievementsPagerAdapter(supportFragmentManager) {
            onMoreInfo(it)
        }
        tabs.setupWithViewPager(pager)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!PrefsUtil.isAchievementsOmited && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
            MaterialDialog(this).safeShow {
                message(text = "Para mostrar una mejor animacion al desbloquear logros, la app necesita un permiso especial, ¿Deseas activarlo?")
                positiveButton(text = "Activar") {
                    try {
                        startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).setData(Uri.parse("package:${getPackage()}")), 5879)
                    } catch (e: Exception) {
                        Toaster.toast("No se pudo abrir la configuracion")
                    }
                }
                negativeButton(text = "Omitir") {
                    PrefsUtil.isAchievementsOmited = true
                }
            }
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        CacheDB.INSTANCE.achievementsDAO().totalPoints.observe(this, Observer {
            levelCalculator.calculate(it ?: 0)
            if (levelCalculator.level != 40) {
                progress.progressMax = levelCalculator.max.toFloat()
                progress.progress = levelCalculator.progress.toFloat()
                progressIndText.visibility = View.VISIBLE
                countDown.text = "${NumberFormat.getNumberInstance(Locale.US).format(levelCalculator.toLvlUp)} XP"
            } else {
                progress.progressMax = 100f
                progress.progress = 100f
                progressIndText.visibility = View.GONE
                countDown.text = "MAXIMO NIVEL"
            }
            level.text = levelCalculator.level.toString()
        })
    }

    @SuppressLint("SetTextI18n")
    private fun onMoreInfo(achievement: Achievement) {
        icon.setImageResource(achievement.usableIcon())
        xpIndicator.text = "${NumberFormat.getNumberInstance(Locale.US).format(achievement.points)} XP"
        state.text = achievement.getState()
        if (!achievement.isSecret && !achievement.isUnlocked && achievement.goal > 0) {
            progressIndicator.visibility = View.VISIBLE
            progressBar.apply {
                max = achievement.goal
                progress = achievement.count
            }
            progressText.text = "${achievement.count} / ${achievement.goal}"
        } else progressIndicator.visibility = View.GONE
        name.text = achievement.usableName()
        description.text = achievement.usableDescription()
        bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN)
            if (bottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                val rect = Rect()
                cardView.getGlobalVisibleRect(rect)
                return if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
                    true
                } else
                    super.dispatchTouchEvent(ev)
            }
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        if (bottomSheet.state == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
        else
            super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5879) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                Toaster.toast("Logros mejorados!")
                val achievementUnlocked = AchievementUnlocked(this).apply {
                    setRounded(false)
                    setLarge(true)
                    setDismissible(true)
                }
                doAsync {
                    val list = CacheDB.INSTANCE.achievementsDAO().completedAchievements
                    val achievementList = mutableListOf<AchievementUnlocked.AchievementData>()
                    list.forEach { achievementList.add(it.achievementData(this@AchievementActivity)) }
                    doOnUI {
                        achievementUnlocked.show(*achievementList.toTypedArray())
                    }
                }
            } else
                Toaster.toast("Permiso no concedido")
        }
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, AchievementActivity::class.java))
        }
    }
}