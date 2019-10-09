package knf.kuma.backup.firestore.data

import androidx.annotation.Keep
import knf.kuma.database.EADB
import knf.kuma.pojos.EAObject

@Keep
data class EAData(val list: List<EAObject> = emptyList()) {
    companion object {
        fun create(): EAData = EAData(EADB.INSTANCE.eaDAO().all)
    }
}