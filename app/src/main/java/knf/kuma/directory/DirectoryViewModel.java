package knf.kuma.directory;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.PagedList;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.retrofit.Repository;

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
