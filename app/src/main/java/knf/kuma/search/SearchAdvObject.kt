package knf.kuma.search

import com.google.gson.annotations.SerializedName

class SearchAdvObject : SearchObject() {
    @SerializedName("adv_img")
    var img = ""
    @SerializedName("adv_type")
    var type = ""
}