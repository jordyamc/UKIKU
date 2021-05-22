package knf.kuma.directory

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import knf.kuma.retrofit.Repository
import kotlinx.coroutines.flow.Flow

class DirectoryViewModel : ViewModel() {
    private val repository = Repository()

    fun getAnimes(): Flow<PagingData<DirObject>> {
        return repository.getAnimeDir()
    }

    fun getOvas(): Flow<PagingData<DirObject>> {
        return repository.getOvaDir()
    }

    fun getMovies(): Flow<PagingData<DirObject>> {
        return repository.getMovieDir()
    }
}
