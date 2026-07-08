package knf.kuma.videoservers

import android.os.Parcel
import android.os.Parcelable

open class VideoServer {
    var name: String
    var options: MutableList<Option> = ArrayList()
    var skipVerification = false

    val option: Option
        get() = options[0]

    constructor(name: String, skipVerification: Boolean = false) {
        this.name = name
        this.skipVerification = skipVerification
    }

    constructor(name: String, option: Option, skipVerification: Boolean = false) {
        this.name = name
        addOption(option)
        this.skipVerification = skipVerification
    }

    constructor(name: String, options: MutableList<Option>, skipVerification: Boolean = false) {
        this.name = name
        this.options = options
        this.skipVerification = skipVerification
    }

    fun addOption(option: Option) {
        options.add(option)
    }

    fun haveOptions(): Boolean {
        return options.size > 1
    }

    object Names {
        const val UPNServer = "UPNShare"
        const val PDRAIN = "PDrain"
        const val ZILLA = "Zilla"
        const val IZANAGI = "Izanagi"
        const val HYPERION = "Hyperion"
        const val OKRU = "Okru"
        const val FEMBED = "Fembed"
        const val FIRE = "Fire"
        const val MANGO = "Mango"
        const val NATSUKI = "Natsuki"
        const val VERYSTREAM = "VeryStream"
        const val FENIX = "Fenix"
        const val RV = "RV"
        const val MP4UPLOAD = "Mp4Upload"
        const val YOURUPLOAD = "YourUpload"
        const val ZIPPYSHARE = "Zippyshare"
        const val GOCDN = "GoCDN"
        const val STAPE = "Stape"
        const val STREAMWISH = "Streamwish"
        const val SBVIDEO = "SBVideo"
        const val MEGA = "Mega"
    }

    companion object {

        fun filter(videoServers: MutableList<VideoServer>): MutableList<VideoServer> {
            val names = ArrayList<String>()
            val filtered = ArrayList<VideoServer>()
            for (videoServer in videoServers) {
                if (!names.contains(videoServer.name)) {
                    names.add(videoServer.name)
                    filtered.add(videoServer)
                }
            }
            return filtered
        }
    }
}
