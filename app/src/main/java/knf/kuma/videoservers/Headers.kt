package knf.kuma.videoservers

import android.os.Parcel
import android.os.Parcelable
import knf.kuma.commons.noCrash

class Headers : Parcelable {
    var headers: MutableList<Pair<String, String>> = mutableListOf()
    private var cookies: MutableList<Pair<String, String>> = mutableListOf()

    constructor()

    constructor(block: MutableList<Pair<String, String>>.() -> Unit) {
        block(headers)
    }

    constructor(parcel: Parcel) {
        headers = parcel.readArrayList(null)?.filterIsInstance<Pair<String, String>>() as MutableList<Pair<String, String>>
        cookies = parcel.readArrayList(null)?.filterIsInstance<Pair<String, String>>() as MutableList<Pair<String, String>>
    }

    fun createHeadersList(): List<String> = mutableListOf<String>().apply {
        headers.forEach {
            add(it.first)
            add(it.second)
        }
        if (cookies.size > 0) {
            add("Cookie")
            add(getCookies())
        }
    }

    fun createHeaders(): List<Pair<String, String>> =
            mutableListOf<Pair<String, String>>().apply {
                addAll(headers)
                if (cookies.size > 0)
                    add("Cookie" to getCookies())
            }

    fun createHeadersMap(): HashMap<String, String> {
        val map = HashMap<String, String>()
        createHeaders().forEach {
            map[it.first] = it.second
        }
        return map
    }

    fun addHeader(key: String, value: String) {
        headers.add(Pair(key, value))
    }

    fun addCookie(key: String, value: String) {
        cookies.add(Pair(key, value))
    }

    fun getCookies(): String {
        val builder = StringBuilder()
        for (pair in cookies)
            builder.append(pair.first)
                    .append('=')
                    .append(pair.second)
                    .append("; ")
        return builder.toString().trim { it <= ' ' }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        noCrash {
            parcel.writeList(headers as List<*>?)
            parcel.writeList(cookies as List<*>?)
        }
    }

    override fun toString(): String {
        return createHeadersList().toString()
    }

    companion object CREATOR : Parcelable.Creator<Headers> {
        override fun createFromParcel(parcel: Parcel): Headers {
            return Headers(parcel)
        }

        override fun newArray(size: Int): Array<Headers?> {
            return arrayOfNulls(size)
        }
    }
}
