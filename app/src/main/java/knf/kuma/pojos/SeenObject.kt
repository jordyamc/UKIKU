package knf.kuma.pojos

import android.util.Log
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import knf.kuma.commons.noCrashLetNullable
import knf.kuma.database.CacheDB
import knf.kuma.recents.RecentModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

@Keep
@Entity
data class SeenObject(@PrimaryKey val eid: String = "", val aid: String = "", val number: String = "")