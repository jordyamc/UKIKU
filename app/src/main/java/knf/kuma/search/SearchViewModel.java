package knf.kuma.search;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.paging.PagedList;

import knf.kuma.pojos.AnimeObject;
import knf.kuma.retrofit.Repository;

public class SearchViewModel extends ViewModel {
    private Repository repository=new Repository();

    public LiveData<PagedList<AnimeObject>> getSearch(String query,String genres){
        if (query.equals("")&&genres.equals("")){
            return repository.getSearch();
        }else if (genres.equals("")){
            return repository.getSeacrh(query);
        }else {
            return repository.getSeacrh(query, genres);
        }
    }
}
