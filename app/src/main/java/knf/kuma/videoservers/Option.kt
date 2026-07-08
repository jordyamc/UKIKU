package knf.kuma.videoservers

data class Option(
    val server: String,
    val name: String?,
    val url: String?,
    val headers: Headers? = null,
    val needTabs: Boolean = false
)

/*open class Option : Parcelable {
    var server: String? = null
    var name: String? = null
    var url: String? = null
    var headers: Headers? = null

    *//**
     * Crea una opcion de descarga
     *
     * @param server  Nombre del servidor de donde viene la opcion [VideoServer.Names]
     * @param name    Nombre de la opcion, null si es una opcion unica
     * @param url     Url de la opcion
     * @param headers Headers requeridos por la opcion
     *//*
    constructor(server: String, name: String?, url: String?, headers: Headers?) {
        if (url == null || url.trim { it <= ' ' }.isEmpty())
            throw IllegalStateException("Url is not valid!")
        this.server = server
        this.name = name
        this.url = url
        this.headers = headers
    }

    constructor(server: String, name: String?, url: String?) {
        if (url == null || url.trim { it <= ' ' }.isEmpty())
            throw IllegalStateException("Url is not valid!")
        this.server = server
        this.name = name
        this.url = url
    }

    protected constructor(`in`: Parcel) {
        server = `in`.readString()
        name = `in`.readString()
        url = `in`.readString()
        headers = `in`.readParcelable(Headers::class.java.classLoader)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(server)
        dest.writeString(name)
        dest.writeString(url)
        dest.writeParcelable(headers, flags)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Option> = object : Parcelable.Creator<Option> {
            override fun createFromParcel(parcel: Parcel): Option {
                return Option(parcel)
            }

            override fun newArray(size: Int): Array<Option?> {
                return arrayOfNulls(size)
            }
        }

        fun getNames(options: MutableList<Option>): MutableList<String> {
            val names = ArrayList<String>()
            for (option in options)
                names.add(option.name ?: "")
            return names
        }
    }
}*/
