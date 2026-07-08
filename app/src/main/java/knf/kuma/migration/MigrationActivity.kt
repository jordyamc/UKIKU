package knf.kuma.migration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import knf.kuma.SplashActivity
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.databinding.ActivityMigrationBinding
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.pojos.av1.FavoriteAV1
import knf.kuma.tv.ui.TVMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import xdroid.toaster.Toaster
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

class MigrationActivity: GenericActivity() {

    private val binding by lazy { ActivityMigrationBinding.inflate(layoutInflater) }
    private var globalCount = 0
    private var total = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback {
            //
        }
        lifecycleScope.launch {
            if (!Network.isConnected) {
                Toaster.toastLong("Internet es necesario para iniciar migración")
                finish()
                return@launch
            }
            total = withContext(Dispatchers.IO) {
                CacheDB.INSTANCE.favsDAO().count + CacheDB.INSTANCE.recordsDAO().count + CacheDB.INSTANCE.seenDAO().count + CacheDB.INSTANCE.seeingDAO().countAll
            }
            try {
                loadDirectory()
                val definitions = loadDefinitions()
                migrateFavoritesInitial(definitions)
                migrateRecords(definitions)
                migrateSeen(definitions)
                migrateSeeing(definitions)
                withContext(Dispatchers.IO) {
                    CacheDB.INSTANCE.queueDAO().nuke()
                    BypassUtil.clear()
                }
                Toaster.toastLong("Migración finalizada")
                PrefsUtil.isAV1DataMigrated = true
                syncData {
                    all()
                }
                delay(500.milliseconds)
                if (intent.getBooleanExtra("is_tv", false)) {
                    startActivity(Intent(this@MigrationActivity, TVMain::class.java))
                } else {
                    startActivity(Intent(this@MigrationActivity, SplashActivity::class.java))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Firebase.crashlytics.recordException(e)
                Toaster.toastLong("Error al migrar")
                delay(500.milliseconds)
            }
            finish()
        }
    }

    private suspend fun setState(state: String) {
        withContext(Dispatchers.Main) {
            binding.state.text = state
        }
    }

    private suspend fun loadDirectory() {
        withContext(Dispatchers.IO) {
            if (PrefsUtil.isAV1MigrateDirectoryFinished) return@withContext
            (0..12).forEach {
                setState("Cargando directorio ${it+1}/13")
                val data = URL("https://cdn.jsdelivr.net/gh/jordyamc/UKIKU@master/static_data/migration/directory_$it.json").readText()
                val list = Gson().fromJson<List<DirectoryAV1>>(data, object :TypeToken<List<DirectoryAV1>>(){}.type)
                CacheDB.INSTANCE.directoryDAO().addAll(list)
                delay(300.milliseconds)
            }
            PrefsUtil.isAV1MigrateDirectoryFinished = true
        }
    }

    private suspend fun loadDefinitions(tries: Int = 0): JSONObject {
        return withContext(Dispatchers.IO) {
            setState("Cargando definiciones...")
            try {
                val data = URL("https://cdn.jsdelivr.net/gh/jordyamc/UKIKU@master/static_data/migration/migration-table.json").readText()
                JSONObject(data)
            } catch (e: Exception) {
                e.printStackTrace()
                if (tries < 5) {
                    delay(500.milliseconds * (tries + 1))
                    loadDefinitions(tries + 1)
                } else {
                    throw e
                }
            }
        }
    }

    private suspend fun migrateFavoritesInitial(definitions: JSONObject) {
        withContext(Dispatchers.IO) {
            CacheDB.INSTANCE.favsDAO().allRaw.forEach {
                try {
                    if (definitions.has(it.aid)) {
                        val migrated = CacheDB.INSTANCE.directoryDAO().findByAid(definitions.getString(it.aid).toInt())
                        if (migrated != null) {
                            CacheDB.INSTANCE.favoriteAV1DAO().addFav(migrated.asFavorite(it.category?: FavoriteAV1.CATEGORY_NONE))
                            CacheDB.INSTANCE.favsDAO().deleteFav(it)
                            globalCount++
                            setState("Migrando... $globalCount/$total")
                            val seeing = CacheDB.INSTANCE.seeingDAO().findByAid(it.aid)
                            if (seeing != null) {
                                val organizer = migrated.asOrganizer(seeing.state)
                                CacheDB.INSTANCE.organizerDAO().add(organizer)
                                CacheDB.INSTANCE.seeingDAO().remove(seeing)
                                globalCount++
                                setState("Migrando... $globalCount/$total")
                            }
                            val records = CacheDB.INSTANCE.recordsDAO().findByAid(it.aid)
                            if (records.isNotEmpty()) {
                                records.forEach { record ->
                                    try {
                                        val number = record.chapter.trim().substringAfterLast(" ").toDouble()
                                        val chapter = migrated.chapters.find { chapter ->
                                            chapter.number == number
                                        }
                                        if (chapter != null) {
                                            CacheDB.INSTANCE.recordAV1DAO().addChapter(chapter.asRecord(migrated, record.date))
                                        } else {
                                            Log.e("Migration", "Drop record: ${it.aid} {${record.chapter}} - No chapter found")
                                        }
                                        CacheDB.INSTANCE.recordsDAO().delete(record)
                                        globalCount++
                                        setState("Migrando... $globalCount/$total")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Log.e("Migration", "Drop record: ${it.aid} {${record.chapter}} - ${e.message}")
                                        CacheDB.INSTANCE.recordsDAO().delete(record)
                                        globalCount++
                                        setState("Migrando... $globalCount/$total")
                                    }
                                }
                            }
                            val seens = CacheDB.INSTANCE.seenDAO().getAllByAid(it.aid)
                            if (seens.isNotEmpty()) {
                                seens.forEach { seen ->
                                    try {
                                        val number = seen.number.trim().substringAfterLast(" ").toDouble()
                                        val chapter = migrated.chapters.find { chapter ->
                                            chapter.number == number
                                        }
                                        if (chapter != null) {
                                            CacheDB.INSTANCE.recordAV1DAO().addChapterIgnore(chapter.asRecord(migrated))
                                        }
                                        CacheDB.INSTANCE.seenDAO().delete(seen)
                                        globalCount++
                                        setState("Migrando... $globalCount/$total")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Log.e("Migration", "Drop seen: ${it.aid} {${seen.number}} - ${e.message}")
                                        CacheDB.INSTANCE.seenDAO().delete(seen)
                                        globalCount++
                                        setState("Migrando... $globalCount/$total")
                                    }
                                }
                            }
                        } else {
                            Log.e("Migration", "Drop fav: ${it.aid} - No directory found")
                            CacheDB.INSTANCE.favsDAO().deleteFav(it)
                            globalCount++
                            globalCount += CacheDB.INSTANCE.recordsDAO().deleteAllAid(it.aid)
                            globalCount += CacheDB.INSTANCE.seenDAO().deleteAllAid(it.aid)
                            setState("Migrando... $globalCount/$total")
                        }
                    } else {
                        Log.e("Migration", "Drop fav: ${it.aid} - No definition found")
                        CacheDB.INSTANCE.favsDAO().deleteFav(it)
                        globalCount++
                        globalCount += CacheDB.INSTANCE.recordsDAO().deleteAllAid(it.aid)
                        globalCount += CacheDB.INSTANCE.seenDAO().deleteAllAid(it.aid)
                        setState("Migrando... $globalCount/$total")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Migration", "Drop fav: ${it.aid} - ${e.message}")
                    CacheDB.INSTANCE.favsDAO().deleteFav(it)
                    globalCount++
                    setState("Migrando... $globalCount/$total")
                }
            }
        }
    }

    private suspend fun migrateRecords(definitions: JSONObject) {
        withContext(Dispatchers.IO) {
            CacheDB.INSTANCE.recordsDAO().allAid.forEach { aid ->
                if (definitions.has(aid)) {
                    val migrated = CacheDB.INSTANCE.directoryDAO().findByAid(definitions.getString(aid).toInt())
                    if (migrated != null) {
                        CacheDB.INSTANCE.recordsDAO().findByAid(aid).forEach { record ->
                            try {
                                val number = record.chapter.trim().substringAfterLast(" ").toDouble()
                                val chapter = migrated.chapters.find { chapter ->
                                    chapter.number == number
                                }
                                if (chapter != null) {
                                    CacheDB.INSTANCE.recordAV1DAO().addChapter(chapter.asRecord(migrated, record.date))
                                }
                                CacheDB.INSTANCE.recordsDAO().delete(record)
                                globalCount++
                                setState("Migrando... $globalCount/$total")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("Migration", "Drop record: ${record.aid} {${record.chapter}} - ${e.message}")
                                CacheDB.INSTANCE.recordsDAO().delete(record)
                                globalCount++
                                setState("Migrando... $globalCount/$total")
                            }
                        }
                    } else {
                        Log.e("Migration", "Drop records: $aid - No directory found")
                        globalCount += CacheDB.INSTANCE.recordsDAO().deleteAllAid(aid)
                        setState("Migrando... $globalCount/$total")
                    }
                } else {
                    Log.e("Migration", "Drop records: $aid - No definition found")
                    globalCount += CacheDB.INSTANCE.recordsDAO().deleteAllAid(aid)
                    setState("Migrando... $globalCount/$total")
                }
            }
        }
    }

    private suspend fun migrateSeen(definitions: JSONObject) {
        withContext(Dispatchers.IO) {
            val lastDate = CacheDB.INSTANCE.recordAV1DAO().lastDate?: System.currentTimeMillis()
            CacheDB.INSTANCE.seenDAO().allAid.forEach { aid ->
                if (definitions.has(aid)) {
                    val migrated = CacheDB.INSTANCE.directoryDAO().findByAid(definitions.getString(aid).toInt())
                    if (migrated != null) {
                        CacheDB.INSTANCE.seenDAO().getAllByAid(aid).forEach { seen ->
                            try {
                                val number = seen.number.trim().substringAfterLast(" ").toDouble()
                                val chapter = migrated.chapters.find { chapter ->
                                    chapter.number == number
                                }
                                if (chapter != null) {
                                    CacheDB.INSTANCE.recordAV1DAO().addChapter(chapter.asRecord(migrated, lastDate))
                                }
                                CacheDB.INSTANCE.seenDAO().delete(seen)
                                globalCount++
                                setState("Migrando... $globalCount/$total")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("Migration", "Drop seen: ${seen.aid} {${seen.number}} - ${e.message}")
                                CacheDB.INSTANCE.seenDAO().delete(seen)
                                globalCount++
                                setState("Migrando... $globalCount/$total")
                            }
                        }
                    } else {
                        Log.e("Migration", "Drop seen: $aid - No directory found")
                        globalCount += CacheDB.INSTANCE.seenDAO().deleteAllAid(aid)
                        setState("Migrando... $globalCount/$total")
                    }
                } else {
                    Log.e("Migration", "Drop seen: $aid - No definition found")
                    globalCount += CacheDB.INSTANCE.seenDAO().deleteAllAid(aid)
                    setState("Migrando... $globalCount/$total")
                }
            }
        }
    }

    private suspend fun migrateSeeing(definitions: JSONObject) {
        withContext(Dispatchers.IO) {
            CacheDB.INSTANCE.seeingDAO().allRaw.forEach { seeing ->
                try {
                    if (definitions.has(seeing.aid)) {
                        val migrated = CacheDB.INSTANCE.directoryDAO().findByAid(definitions.getString(seeing.aid).toInt())
                        if (migrated != null) {
                            val organizer = migrated.asOrganizer(seeing.state)
                            CacheDB.INSTANCE.organizerDAO().add(organizer)
                        }
                        CacheDB.INSTANCE.seeingDAO().remove(seeing)
                        globalCount++
                        setState("Migrando... $globalCount/$total")
                    } else {
                        CacheDB.INSTANCE.seeingDAO().remove(seeing)
                        globalCount++
                        setState("Migrando... $globalCount/$total")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Migration", "Drop: ${seeing.aid} - ${e.message}")
                    CacheDB.INSTANCE.seeingDAO().remove(seeing)
                    globalCount++
                    setState("Migrando... $globalCount/$total")
                }
            }
        }
    }
}