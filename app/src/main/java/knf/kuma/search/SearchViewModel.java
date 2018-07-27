package knf.kuma.search;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.arch.paging.PagedList;

import knf.kuma.pojos.AnimeObject;
import knf.kuma.retrofit.Repository;

public class SearchViewModel extends ViewModel {
    private Repository repository = new Repository();

    private LiveData<PagedList<AnimeObject>> liveData = new MutableLiveData<>();
    private Observer<PagedList<AnimeObject>> observer;

    public void setSearch(String query, String genres, LifecycleOwner owner, Observer<PagedList<AnimeObject>> observer) {
        if (this.observer != null)
            liveData.removeObserver(this.observer);
        this.observer = observer;
        if (query.equals("") && genres.equals(""))
            liveData = repository.getSearch();
        else if (genres.equals(""))
            liveData = repository.getSearch(query);
        else
            liveData = repository.getSearch(query, genres);
        liveData.observe(owner, this.observer);
    }
}
