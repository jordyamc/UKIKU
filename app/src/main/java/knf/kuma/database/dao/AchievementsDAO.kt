package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.Achievement

@Dao
@TypeConverters(BaseConverter::class)
interface AchievementsDAO {
    @get:Query("SELECT * FROM achievement")
    val all: List<Achievement>

    @get:Query("SELECT * FROM achievement WHERE isUnlocked = 1")
    val allCompleted: List<Achievement>

    @get:Query("SELECT SUM(points) FROM achievement WHERE isUnlocked = 1")
    val totalPoints: LiveData<Int?>

    @get:Query("SELECT * FROM achievement WHERE isUnlocked = 0 AND count >= goal AND NOT goal = 0")
    val completionListener: LiveData<List<Achievement>>

    @get:Query("SELECT COUNT(*) FROM achievement")
    val totalAchievements: Int

    @get:Query("SELECT * FROM achievement WHERE isUnlocked = 1 ORDER BY time ASC")
    val completedAchievements: List<Achievement>

    @Query("SELECT * FROM achievement WHERE isUnlocked = :isUnlocked ORDER BY points ASC, name")
    fun achievementList(isUnlocked: Int): LiveData<List<Achievement>>

    @Query("SELECT * FROM achievement WHERE `key`=:key")
    fun find(key: Int): Achievement?

    @Query("SELECT * FROM achievement WHERE `key` IN (:keys)")
    fun find(vararg keys: Int): List<Achievement>

    @Query("SELECT * FROM achievement WHERE `key` IN (:keys)")
    fun find(keys: List<Int>): List<Achievement>

    @Query("SELECT isUnlocked FROM achievement WHERE `key`=:key LIMIT 1")
    fun isUnlocked(key: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg achievements: Achievement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(achievements: List<Achievement>)

    @Update
    fun update(achievements: Achievement)

    @Query("DELETE FROM achievement")
    fun nuke()
}