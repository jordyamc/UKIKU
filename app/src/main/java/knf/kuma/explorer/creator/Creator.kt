package knf.kuma.explorer.creator

import knf.kuma.pojos.ExplorerObject

interface Creator {
    fun exist(): Boolean
    fun createSlugList(): List<String>
    fun createDirectoryList(progressCallback: (Int, Int) -> Unit): List<ExplorerObject>
}