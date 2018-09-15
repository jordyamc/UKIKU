package knf.kuma.directory

import android.content.Context

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import knf.kuma.pojos.AnimeObject
import knf.kuma.retrofit.Repository

class DirectoryViewModel : ViewModel() {
    private val repository = Repository()

    fun getAnimes(context: Context): LiveData<PagedList<AnimeObject>> {
        return repository.getAnimeDir(context)
    }

    fun getOvas(context: Context): LiveData<PagedList<AnimeObject>> {
        return repository.getOvaDir(context)
    }

    fun getMovies(context: Context): LiveData<PagedList<AnimeObject>> {
        return repository.getMovieDir(context)
    }
}
