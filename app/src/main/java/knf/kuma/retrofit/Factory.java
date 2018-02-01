package knf.kuma.retrofit;

import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.Recents;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

/**
 * Created by Jordy on 03/01/2018.
 */

public interface Factory {
    @GET(".")
    Call<Recents> getRecents(@Header("Cookie") String cookies);

    @GET("{rest}")
    Call<AnimeObject.WebInfo> getAnime(@Header("Cookie") String cookies, @Path("rest") String rest);
}
