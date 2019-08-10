package knf.kuma.search

import com.google.gson.annotations.SerializedName

class SearchAdvObject : SearchObject() {
    @SerializedName("adv_img")
    var img = ""
    @SerializedName("adv_type")
    var type = ""

    override fun equals(other: Any?): Boolean {
        return other is SearchAdvObject &&
                key == other.key &&
                aid == other.aid &&
                name == other.name &&
                link == other.link &&
                img == other.img &&
                type == other.type
    }

    override fun hashCode(): Int {
        return "$key$aid$name$link$img$type".hashCode()
    }
}