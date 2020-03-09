package knf.kuma.explorer.creator

import android.net.Uri

data class SubFile(val name: String, private val uri: String) {

    fun getFileUri(): Uri = Uri.parse(uri)

}