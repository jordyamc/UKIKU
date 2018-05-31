package knf.kuma.animeinfo;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.retrofit.Repository;

public class AnimeViewModel extends ViewModel {
    private Repository repository = new Repository();
    private LiveData<AnimeObject> liveData;

    public void init(Context context, String link, boolean persist) {
        if (liveData != null)
            return;
        liveData = repository.getAnime(context, link, persist);
    }

    public void init(String aid) {
        if (liveData != null)
            return;
        liveData = CacheDB.INSTANCE.animeDAO().getAnimeByAid(aid);
    }

    public LiveData<AnimeObject> getLiveData() {
        return liveData;
    }

    public enum ModeState{
        NORMAL()
    }
}
