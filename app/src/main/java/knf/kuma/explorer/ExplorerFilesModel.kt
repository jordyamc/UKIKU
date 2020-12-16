package knf.kuma.explorer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import knf.kuma.pojos.ExplorerObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExplorerFilesModel : ViewModel() {
    val localFilesData = MutableLiveData<List<ExplorerObject>>()
    private var localList = mutableListOf<ExplorerObject>()

    fun setData(list: List<ExplorerObject>) {
        viewModelScope.launch {
            localFilesData.value = list
            localList = list.toMutableList()
        }
    }

    fun remove(item: ExplorerObject) {
        viewModelScope.launch(Dispatchers.IO) {
            localList.filter { it.key != item.key }.let {
                withContext(Dispatchers.Main) {
                    localFilesData.value = it
                }
            }
        }
    }

    fun removeOne(item: ExplorerObject) {
        viewModelScope.launch(Dispatchers.IO) {
            val found = localList.find { it.key == item.key }
            val index = localList.indexOf(found)
            if (index >= 0 && found != null) {
                val new = ExplorerObject(found).apply {
                    count -= 1
                }
                localList.removeAt(index)
                localList.add(index, new)
                withContext(Dispatchers.Main) {
                    localFilesData.value = localList
                }
            }
        }
    }

}