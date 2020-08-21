package knf.kuma.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import knf.kuma.player.PlayerState

@Dao
interface PlayerStateDAO {
    @Query("SELECT * FROM playerstate WHERE title = :title")
    fun find(title: String): PlayerState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun set(state: PlayerState)
}