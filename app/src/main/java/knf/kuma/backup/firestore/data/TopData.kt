package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.commons.PrefsUtil

@Keep
data class TopData(val uid: String = "", val name: String = "", val number: Int = 0, val forced: Boolean = false) {
    companion object {
        fun create() = FirestoreManager.user?.let {
            TopData(it.uid, it.displayName ?: "Anónimo", PrefsUtil.userRewardedVideoCount)
        }
                ?: TopData(PrefsUtil.instanceUuid, PrefsUtil.instanceName, PrefsUtil.userRewardedVideoCount)
    }
}