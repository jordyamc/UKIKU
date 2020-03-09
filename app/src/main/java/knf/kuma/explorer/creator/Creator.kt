package knf.kuma.explorer.creator

import knf.kuma.pojos.ExplorerObject

interface Creator {
    fun exist(): Boolean
    fun createDirectoryList(progressCallback: (Int, Int) -> Unit): List<ExplorerObject>
    fun createSubFilesList(fileName: String): List<SubFile>
}