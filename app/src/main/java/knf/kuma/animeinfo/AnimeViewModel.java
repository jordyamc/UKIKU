package knf.kuma.animeinfo;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import knf.kuma.pojos.AnimeObject;
import knf.kuma.retrofit.Repository;

public class AnimeViewModel extends ViewModel {
    private Repository repository = new Repository();
    private LiveData<AnimeObject> liveData;
    private MutableLiveData<ModeState> chaptersCallback=new MutableLiveData<>();

    public void init(Context context, String link, boolean persist) {
        if (liveData != null)
            return;
        liveData = repository.getAnime(context, link, persist);
    }

    public LiveData<AnimeObject> getLiveData() {
        return liveData;
    }

    public void setChapterState(ModeState state){
        chaptersCallback.setValue(state);
    }

    public MutableLiveData<ModeState> getChaptersCallback(){
        return chaptersCallback;
    }

    public enum ModeState{
        NORMAL()
    }
}
