package knf.kuma.search;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.paging.PagedList;
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
