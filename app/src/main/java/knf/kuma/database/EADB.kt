package knf.kuma.database

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import knf.kuma.App
import knf.kuma.database.dao.EaDAO
import knf.kuma.pojos.EAObject

@Database(entities = [EAObject::class], version = 1)
abstract class EADB : RoomDatabase() {

    abstract fun eaDAO(): EaDAO

    companion object {
        val INSTANCE: EADB by lazy { init(App.context) }

        private fun init(context: Context): EADB =
                Room.databaseBuilder(context, EADB::class.java, "ee-db")
                        .allowMainThreadQueries()
                        .build()
    }
}
