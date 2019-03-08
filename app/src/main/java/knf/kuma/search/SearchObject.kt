package knf.kuma.search

import com.google.gson.annotations.SerializedName

open class SearchObject {
    @SerializedName("adv_key")
    var key = 0
    @SerializedName("adv_aid")
    var aid = ""
    @SerializedName("adv_name")
    var name = ""
    @SerializedName("adv_link")
    var link = ""
}