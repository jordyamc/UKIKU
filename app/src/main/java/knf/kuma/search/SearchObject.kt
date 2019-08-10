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

    override fun equals(other: Any?): Boolean {
        return other is SearchObject &&
                key == other.key &&
                aid == other.aid &&
                name == other.name &&
                link == other.link
    }

    override fun hashCode(): Int {
        return "$key$aid$name$link".hashCode()
    }
}