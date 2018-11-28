package knf.kuma.directory

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import knf.kuma.pojos.AnimeObject
import knf.kuma.retrofit.Repository

class DirectoryViewModel : ViewModel() {
    private val repository = Repository()

    fun getAnimes(): LiveData<PagedList<AnimeObject>> {
        return repository.getAnimeDir()
    }

    fun getOvas(): LiveData<PagedList<AnimeObject>> {
        return repository.getOvaDir()
    }

    fun getMovies(): LiveData<PagedList<AnimeObject>> {
        return repository.getMovieDir()
    }
}
