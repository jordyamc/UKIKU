package knf.kuma.retrofit

import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.Recents
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface Factory {
    @GET(".")
    fun getRecents(@Header("Cookie") cookies: String, @Header("User-Agent") userAgent: String, @Header("Referer") referer: String): Call<Recents>

    @GET("{rest}")
    fun getAnime(@Header("Cookie") cookies: String, @Header("User-Agent") userAgent: String, @Header("Referer") referer: String, @Path("rest") rest: String): Call<AnimeObject.WebInfo>
}
