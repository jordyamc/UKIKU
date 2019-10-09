package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.CacheDB
import knf.kuma.pojos.Achievement

@Keep
data class AchievementsData(val list: List<Achievement> = emptyList()) {
    companion object {
        fun create(): AchievementsData = AchievementsData(CacheDB.INSTANCE.achievementsDAO().all)
    }
}