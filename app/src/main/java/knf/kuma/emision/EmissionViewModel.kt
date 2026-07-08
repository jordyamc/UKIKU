package knf.kuma.emision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import knf.kuma.commons.JsExtractor
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
        val result = JsExtractor.processLink("https://animeav1.com/horario") ?: return emptyMap()
        val list = mutableListOf<DirectoryAV1Calendar>()
        for (i in 0 until result.length()) {
            list.add(DirectoryAV1Calendar.fromJson(result.getJSONObject(i)))
        }
        val grouped = list.sortedBy { it.name }.groupBy { it.day }
        return (1..7).associateWith { grouped[it] ?: emptyList() }
    }

}