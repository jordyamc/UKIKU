package knf.kuma.videoservers

import android.os.Parcel
import android.os.Parcelable
import android.util.Pair

class Headers : Parcelable {
    var headers: MutableList<Pair<String, String>> = mutableListOf()
        get() {
            if (cookies.size > 0)
                field.add(Pair("Cookie", getCookies()))
            return field
        }
    private var cookies: MutableList<Pair<String, String>> = mutableListOf()

    constructor()

    constructor(parcel: Parcel) {
        headers = parcel.readArrayList(null)?.filterIsInstance<Pair<String, String>>() as MutableList<Pair<String, String>>
        cookies = parcel.readArrayList(null)?.filterIsInstance<Pair<String, String>>() as MutableList<Pair<String, String>>
    }

    fun addHeader(key: String, value: String) {
        headers.add(Pair(key, value))
    }

    fun addCookie(key: String, value: String) {
        cookies.add(Pair(key, value))
    }

    private fun getCookies(): String {
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
        parcel.writeList(headers)
        parcel.writeList(cookies)
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
