package knf.kuma.achievements

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import knf.kuma.R
import knf.kuma.backup.BUUtils
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.*
import knf.kuma.custom.AchievementUnlocked
import knf.kuma.database.CacheDB
import knf.kuma.database.EADB
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.FavoriteObject
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("StaticFieldLeak")
object AchievementManager {

    private lateinit var context: Context
    private lateinit var achievementUnlocked: AchievementUnlocked
    private lateinit var completionLiveData: LiveData<List<Achievement>>
    private val achievementsDAO = CacheDB.INSTANCE.achievementsDAO()
    private val liveList = arrayListOf<LiveData<Int>>()
    private const val VERSION = 3

    fun init(context: Context) {
        this.context = context
        achievementUnlocked = AchievementUnlocked(context).apply {
            setRounded(false)
            setLarge(true)
            setDismissible(true)
        }
        preloadAchievements()
        achievementsDAO.completionListener.also { completionLiveData = it }.observeForever(Observer { it ->
            if (it.isEmpty()) return@Observer
            val list: List<Int> = it.map { it.key.toInt() }
            unlock(list)
        })
        CacheDB.INSTANCE.chaptersDAO().countLive.also { liveList.add(it) }.observeForever {
            updateCount(it, 33, 39)
        }
        CacheDB.INSTANCE.favsDAO().countLive.also { liveList.add(it) }.observeForever {
            updateCount(it, 11, 1, 2, 3, 4, 5)
        }
        CacheDB.INSTANCE.seeingDAO().countLive.also { liveList.add(it) }.observeForever {
            updateCount(it, 16, 17, 18, 19)
        }
        CacheDB.INSTANCE.seeingDAO().countCompletedLive.also { liveList.add(it) }.observeForever {
            updateCount(it, 20, 21, 22, 23)
        }
        CacheDB.INSTANCE.seeingDAO().countDroppedLive.also { liveList.add(it) }.observeForever {
            updateCount(it, 24, 25, 26, 27)
        }
        CacheDB.INSTANCE.seeingDAO().isAnimeCompleted(listOf("363", "1706", "2950", "1182", "2479", "2478")).also { liveList.add(it) }.observeForever {
            if (it == 6) unlock(38)
        }
        CacheDB.INSTANCE.seeingDAO().isAnimeCompleted(listOf("1487", "1488", "1019", "460", "1493", "1494")).also { liveList.add(it) }.observeForever {
            if (it == 6) unlock(45)
        }
    }

    private fun resetIndicator() {
        achievementUnlocked.apply {
            setRounded(false)
            setLarge(true)
            setDismissible(true)
        }
    }

    private fun preloadAchievements() {
        doAsync {
            if (VERSION > PrefsUtil.achievementsVersion) {
                achievementsDAO.nuke()
                PrefsUtil.achievementsVersion = VERSION
            }
            achievementsDAO.insert(
                    Achievement(0, "Primeros pasos", "Abre la app por primera vez", points = 1000),
                    Achievement(1, "Pathetic", "Agrega 10 favoritos", points = 1000, goal = 10),
                    Achievement(2, "¿Lo estas intentando?", "Agrega 100 favoritos", 2000, goal = 100),
                    Achievement(3, "Ya nos vamos entendiendo", "Agrega 200 favoritos", 3000, goal = 200),
                    Achievement(4, "Oye tranquilo viejo", "Agrega 500 favoritos", 4000, goal = 500),
                    Achievement(5, "Estas demente parker", "Agrega 1000 favoritos", 6000, goal = 1000, isSecret = true),
                    Achievement(6, "Remoto", "Usa Cast por primera vez", points = 1000, isSecret = true),
                    Achievement(7, "Veterano", "Ten instalado Animeflv App", points = 3000, isSecret = true),
                    Achievement(8, "Iniciado", "Ten instalada la app por 3 meses", points = 2000, isSecret = true),
                    Achievement(9, "Empezando a cultivar", "Ten instalada la app por 6 meses", points = 3000, isSecret = true),
                    Achievement(10, "Feliz cumpleaños", "Ten instalada la app por 1 año", points = 6000, isSecret = true),
                    Achievement(11, "Primer amor", "Añade tu primer favorito", points = 1000, goal = 1),
                    Achievement(12, "Algo fácil para iniciar", "Inicia el misterio", points = 1000, isSecret = true),
                    Achievement(13, "El mejor escondite esta a la vista", "Descubre la secuencia", points = 2000, isSecret = true),
                    Achievement(14, "Va para el curriculum", "Descubre para que sirve US", points = 6000, isSecret = true),
                    Achievement(15, "Cuna del manga", "Encuentra Akihabara", points = 2000, isSecret = true),
                    Achievement(16, "Por algo se empieza", "Sigue 5 animes", points = 1000, goal = 5),
                    Achievement(17, "Se prendió esta mierda", "Sigue 15 animes", points = 2000, goal = 15),
                    Achievement(18, "Esto se va a descontrolar", "Sigue 40 animes", points = 3000, goal = 40),
                    Achievement(19, "Con el Rinnegan lo veo todo", "Sigue 100 animes", points = 4000, goal = 100),
                    Achievement(20, "El inicio del camino", "Marca 1 anime como completado", points = 1000, goal = 1),
                    Achievement(21, "Te está gustando?", "Marca 5 animes como completados", points = 2000, goal = 5),
                    Achievement(22, "Ya no hay vuelta atrás", "Marca 20 animes como completados", points = 3000, goal = 20),
                    Achievement(23, "Otaku", "Marca 50 animes como completados", points = 4000, goal = 50),
                    Achievement(24, "Mala elección", "Dropea 1 anime", points = 1000, goal = 1, isSecret = true),
                    Achievement(25, "Algo anda mal...", "Dropea 5 animes", points = 2000, goal = 5, isSecret = true),
                    Achievement(26, "No te gusta nada", "Dropea 15 animes", points = 3000, goal = 15, isSecret = true),
                    Achievement(27, "Antes eras chido...", "Dropea 30 animes", points = 4000, goal = 30, isSecret = true),
                    Achievement(28, "Lo has logrado!", "Completa el easter egg", points = 12000, isSecret = true),
                    Achievement(29, "Viviendo al limite", "Reproduce un episodio con poca batería", points = 2500, isSecret = true),
                    Achievement(30, "Informado", "Lee 20 noticias", points = 1500, goal = 20),
                    Achievement(31, "Vampiro", "Ve anime pasada la media noche", points = 2000, isSecret = true),
                    Achievement(32, "Estas aburrido?", "Refresca la pantalla random 15 veces", points = 2500, isSecret = true),
                    Achievement(33, "Otaku definitivo", "Ve 15k episodios", points = 15000, goal = 15000, isSecret = true),
                    Achievement(34, "Bien hecho puerco", "Agrega 10 ecchis a favoritos", points = 2000, goal = 10),
                    Achievement(35, "Viajero", "Descarga un anime completo", points = 2000),
                    Achievement(36, "Que milagro verte por aquí", "No uses la app por una semana", points = 3000, isSecret = true),
                    Achievement(37, "La aventura comienza", "Agrega un shounen a favoritos", points = 2000),
                    Achievement(38, "1.048596", "Completa toda la saga de Steins;Gate", points = 10000, isSecret = true),
                    Achievement(39, "Sabio de los 6 caminos", "Ve 5000 episodios", points = 5000, goal = 5000),
                    Achievement(40, "Que haces?", "Presiona el botón de configuracion 20 veces", points = 5000, isSecret = true),
                    Achievement(41, "Mas vale prevenir", "Respalda tus datos en la nube", points = 2000),
                    Achievement(42, "Compartiendo sabiduria", "Comparte 20 animes", points = 2000, goal = 20),
                    Achievement(43, "Boku no pico?", "Busca boku no hero", points = 2000, isSecret = true),
                    Achievement(44, "Alzheimer?", "Abre el historial 20 veces", points = 2000, goal = 20),
                    Achievement(45, "A Sam le gusta esto", "Completa todo Evangelion", points = 6000, isSecret = true)
            )
        }
    }

    @DrawableRes
    fun getIcon(key: Long): Int {
        return when (key) {
            0L -> R.drawable.ic_achievement_start
            in 1..5, 11L -> R.drawable.ic_achievement_fav
            6L -> R.drawable.ic_achievement_cast
            7L -> R.drawable.ic_umaru_simple
            in 8..10 -> R.drawable.ic_achievement_calendar
            in 12..15, 28L -> R.drawable.ic_achievement_egg
            in 16..19 -> R.drawable.ic_achievement_following
            in 20..23 -> R.drawable.ic_achievement_completed
            in 24..27 -> R.drawable.ic_achievement_droped
            29L -> R.drawable.ic_achievement_battery
            30L -> R.drawable.ic_achievement_news
            31L -> R.drawable.ic_achievement_vampire
            32L -> R.drawable.ic_achievement_bored
            33L, 39L -> R.drawable.ic_achievement_otaku
            34L -> R.drawable.ic_achievement_pig
            35L -> R.drawable.ic_achievement_airplane
            36L -> R.drawable.ic_achievement_sad
            37L -> R.drawable.ic_achievement_onepiece
            38L -> R.drawable.ic_achievement_clock
            40L -> R.drawable.ic_achievement_question
            41L -> R.drawable.ic_achievement_cloud
            42L -> R.drawable.ic_achievement_share
            43L -> R.drawable.ic_achievement_midoriya
            44L -> R.drawable.ic_achievement_memory
            45L -> R.drawable.ic_achievement_evangelion
            else -> R.drawable.ic_umaru_simple
        }
    }

    fun backup(ncontext: Context) {
        BUUtils.init(ncontext)
        if (BUUtils.isLogedIn)
            BUUtils.backupNUI(ncontext, "achievements", object : BUUtils.BackupInterface {
                override fun onResponse(backupObject: BackupObject<*>?) {

                }
            })
    }

    fun restore(ncontext: Context) {
        BUUtils.init(ncontext)
        if (BUUtils.isLogedIn)
            BUUtils.search(ncontext, "achievements", object : BUUtils.SearchInterface {
                override fun onResponse(backupObject: BackupObject<*>?) {
                    if (backupObject != null) {
                        doAsync {
                            CacheDB.INSTANCE.achievementsDAO().update((backupObject.data?.filterIsInstance<Achievement>()
                                    ?: arrayListOf()).filter { it.isUnlocked })
                        }
                    }
                }
            })
    }

    private fun updateCount(count: Int, vararg keys: Int) {
        doAsync {
            val list = achievementsDAO.find(*keys)
            list.forEach { it.count = count }
            achievementsDAO.update(list)
        }
    }

    private fun incrementCount(by: Int, vararg keys: Int) {
        doAsync {
            val list = achievementsDAO.find(*keys)
            list.forEach { it.count += by }
            achievementsDAO.update(list)
        }
    }

    fun isUnlocked(key: Int): Boolean {
        return achievementsDAO.isUnlocked(key)
    }

    private fun unlock(keys: Collection<Int>) {
        unlock(*keys.toIntArray())
    }

    fun unlock(vararg keys: Int) {
        doAsync {
            val list = mutableListOf<Achievement>()
            keys.forEach {
                noCrash {
                    if (!achievementsDAO.isUnlocked(it) && !isTV) {
                        val achievement = achievementsDAO.find(it)
                        if (achievement != null) {
                            Answers.getInstance().logCustom(CustomEvent("Achievement").putCustomAttribute("code", it))
                            list.add(achievement.apply {
                                isUnlocked = true
                                time = System.currentTimeMillis()
                            })
                        }
                    }
                }
            }
            achievementsDAO.update(list)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
                val achievementList = mutableListOf<AchievementUnlocked.AchievementData>()
                list.forEach { achievementList.add(it.achievementData(context)) }
                resetIndicator()
                doOnUI {
                    achievementUnlocked.show(*achievementList.toTypedArray())
                }
            } else
                for (achievement in list)
                    Toaster.toast("${achievement.name} | Desbloqueado")
        }
    }

    fun onAppStart() {
        doAsync {
            val list = mutableListOf<Int>()
            list.add(0)
            if (PrefsUtil.firstStart == 0L)
                PrefsUtil.firstStart = Calendar.getInstance().timeInMillis
            val time = (Calendar.getInstance().timeInMillis - PrefsUtil.firstStart)
            if (time >= 7776000000) list.add(8)
            if (time >= 15552000000) list.add(9)
            if (time >= 31104000000) list.add(10)
            if (Calendar.getInstance().timeInMillis - PrefsUtil.lastStart >= 604800000)
                list.add(36)
            PrefsUtil.lastStart = Calendar.getInstance().timeInMillis
            if (EADB.INSTANCE.eaDAO().isUnlocked(0)) list.add(12)
            if (EADB.INSTANCE.eaDAO().isUnlocked(1)) list.add(13)
            if (EADB.INSTANCE.eaDAO().isUnlocked(2)) list.add(14)
            if (EADB.INSTANCE.eaDAO().isUnlocked(3)) list.add(15)
            if (EAHelper.isPart0Unlocked && EAHelper.isPart1Unlocked && EAHelper.isPart2Unlocked && EAHelper.isPart3Unlocked)
                list.add(28)
            AchievementManager.unlock(list)
        }
    }

    fun onPhaseUnlocked(phase: Int) {
        when (phase) {
            0 -> unlock(12)
            1 -> unlock(13)
            2 -> unlock(14)
            3 -> unlock(15)
        }
        if (EAHelper.isPart0Unlocked && EAHelper.isPart1Unlocked && EAHelper.isPart2Unlocked && EAHelper.isPart3Unlocked)
            unlock(28)
    }

    fun onNewsOpened() {
        incrementCount(1, 30)
    }

    fun onBackup() {
        unlock(41)
    }

    fun onShare() {
        incrementCount(1, 42)
    }

    fun onSearch(query: String) {
        when (query.toLowerCase()) {
            "boku no hero" -> unlock(43)
        }
    }

    fun onRecordsOpened() {
        incrementCount(1, 44)
    }

    fun onFavAdded(fav: FavoriteObject) {
        doAsync {
            if (CacheDB.INSTANCE.animeDAO().hasGenre(fav.aid, "Ecchi".like))
                incrementCount(1, 34)
            if (CacheDB.INSTANCE.animeDAO().hasGenre(fav.aid, "Shounen".like))
                unlock(37)
        }
    }

    private val String.like: String get() = "%$this%"

    fun onPlayQueue(count: Int) {
        if (count == 0)
            return
        incrementCount(count - 1, 33)
        onPlayChapter()
    }

    fun onPlayChapter() {
        doAsync {
            noCrash {
                val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val batteryScale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = batteryLevel / batteryScale.toFloat()
                val isLivingAtLimit = (batteryPct * 100).toInt() <= 10 &&
                        batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) != BatteryManager.BATTERY_STATUS_CHARGING &&
                        batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) != BatteryManager.BATTERY_STATUS_FULL
                if (isLivingAtLimit)
                    unlock(29)
                val timeFormat = SimpleDateFormat("HH", Locale.getDefault())
                val current = timeFormat.format(Calendar.getInstance().time).toInt()
                if (current in 0..3)
                    unlock(31)
            }.toast()
        }
    }

}