package knf.kuma.database

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import knf.kuma.database.dao.EaDAO
import knf.kuma.pojos.EAObject

@Database(entities = [EAObject::class], version = 1)
abstract class EADB : RoomDatabase() {

    abstract fun eaDAO(): EaDAO

    companion object {
        lateinit var INSTANCE: EADB

        fun init(context: Context) {
            INSTANCE = Room.databaseBuilder(context, EADB::class.java, "ee-db")
                    .allowMainThreadQueries()
                    .build()
        }
    }
}
