package knf.kuma.directory

data class DirObject(
        var key: Int = 0,
        var aid: String = "",
        var name: String = "",
        var link: String? = null,
        var type: String = "",
    var rate_stars: String? = ""
)