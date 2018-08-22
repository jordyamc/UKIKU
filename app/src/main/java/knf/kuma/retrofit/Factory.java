package knf.kuma.retrofit;

import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.Recents;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface Factory {
    @GET(".")
    Call<Recents> getRecents(@Header("Cookie") String cookies, @Header("User-Agent") String userAgent, @Header("Referer") String referer);

    @GET("{rest}")
    Call<AnimeObject.WebInfo> getAnime(@Header("Cookie") String cookies, @Header("User-Agent") String userAgent, @Header("Referer") String referer, @Path("rest") String rest);
}
