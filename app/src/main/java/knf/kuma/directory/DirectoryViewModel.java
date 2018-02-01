package knf.kuma.directory;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.paging.PagedList;
import android.content.Context;

import knf.kuma.pojos.AnimeObject;
import knf.kuma.retrofit.Repository;

/**
 * Created by Jordy on 06/01/2018.
 */

public class DirectoryViewModel extends ViewModel {
    private Repository repository=new Repository();

    public LiveData<PagedList<AnimeObject>> getAnimes(Context context) {
        return repository.getAnimeDir(context);
    }

    public LiveData<PagedList<AnimeObject>> getOvas(Context context) {
        return repository.getOvaDir(context);
    }

    public LiveData<PagedList<AnimeObject>> getMovies(Context context) {
        return repository.getMovieDir(context);
    }
}
