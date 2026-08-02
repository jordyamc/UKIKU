package knf.kuma.emision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import knf.kuma.commons.JsExtractor
import knf.kuma.commons.Network
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.DirectoryAV1Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class EmissionViewModel: ViewModel() {
    val emissionFlow = MutableStateFlow(emptyMap<Int, List<DirectoryAV1Calendar>>())

    init {
        viewModelScope.launch {
            emissionFlow.value = create()
        }
    }

    private suspend fun create(): Map<Int, List<DirectoryAV1Calendar>> {
        val list = if (Network.isConnected) {
            val result = JsExtractor.processLink("https://animeav1.com/horario")
            if (result != null) {
                val local = fromDB()
                val tList = mutableListOf<DirectoryAV1Calendar>()
                for (i in 0 until result.length()) {
                    tList.add(DirectoryAV1Calendar.fromJson(result.getJSONObject(i)))
                }
                tList.forEach { n ->
                    val saved = local.find { l -> n.aid == l.aid }
                    if (saved != null) {
                        n.isHidden = saved.isHidden
                    }
                }
                CacheDB.INSTANCE.calendarBlacklistDAO().update(tList)
                tList
            } else {
                fromDB()
            }
        } else {
            fromDB()
        }
        val grouped = list.sortedBy { it.name }.groupBy { it.day }
        return (1..7).associateWith { grouped[it] ?: emptyList() }
    }

    private suspend fun fromDB(): List<DirectoryAV1Calendar> {
        return CacheDB.INSTANCE.calendarBlacklistDAO().all()
    }

}