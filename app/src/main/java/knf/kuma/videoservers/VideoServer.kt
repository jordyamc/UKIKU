package knf.kuma.videoservers

import android.os.Parcel
import android.os.Parcelable
import java.util.*

open class VideoServer : Parcelable {
    var name: String
    var options: MutableList<Option> = ArrayList()

    val option: Option
        get() = options[0]

    constructor(name: String) {
        this.name = name
    }

    constructor(name: String, option: Option) {
        this.name = name
        addOption(option)
    }

    constructor(name: String, options: MutableList<Option>) {
        this.name = name
        this.options = options
    }

    fun addOption(option: Option) {
        options.add(option)
    }

    fun haveOptions(): Boolean {
        return options.size > 1
    }

    class Sorter : Comparator<VideoServer> {
        override fun compare(videoServer: VideoServer, t1: VideoServer): Int {
            return videoServer.name.compareTo(t1.name, ignoreCase = true)
        }
    }

    protected constructor(parcel: Parcel) {
        name = parcel.readString() ?: ""
        options = parcel.createTypedArrayList(Option.CREATOR) ?: arrayListOf()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeTypedList(options)
    }

    object Names {
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
        const val MEGA = "Mega"

        internal val downloadServers: Array<String>
            get() = arrayOf(IZANAGI, HYPERION, OKRU, FEMBED, FIRE, NATSUKI, VERYSTREAM, FENIX, RV, YOURUPLOAD, ZIPPYSHARE, MEGA, MP4UPLOAD)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<VideoServer> = object : Parcelable.Creator<VideoServer> {
            override fun createFromParcel(parcel: Parcel): VideoServer {
                return VideoServer(parcel)
            }

            override fun newArray(size: Int): Array<VideoServer?> {
                return arrayOfNulls(size)
            }
        }

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

        fun getNames(videoServers: MutableList<VideoServer>): MutableList<String> {
            val names = ArrayList<String>()
            for (videoServer in videoServers) {
                names.add(videoServer.name)
            }
            return names
        }

        private fun findPosition(videoServers: MutableList<VideoServer>, name: String): Int {
            for ((i, videoServer) in videoServers.withIndex()) {
                if (videoServer.name == name)
                    return i
            }
            return 0
        }

        fun existServer(videoServers: MutableList<VideoServer>, position: Int): Boolean {
            val name = Names.downloadServers[position - 1]
            for (videoServer in videoServers) {
                if (videoServer.name == name)
                    return true
            }
            return false
        }

        fun findServer(videoServers: MutableList<VideoServer>, position: Int): VideoServer {
            val name = Names.downloadServers[position - 1]
            return videoServers[findPosition(videoServers, name)]
        }
    }
}
