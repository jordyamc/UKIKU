package knf.kuma.favorite

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import knf.kuma.commons.PrefsUtil
import knf.kuma.database.CacheDB
import knf.kuma.pojos.FavoriteObject

class FavoriteViewModel : ViewModel() {

    fun getData(): LiveData<MutableList<FavoriteObject>> {
        return if (PrefsUtil.showFavSections())
            FavSectionHelper.init()
        else
            when (PrefsUtil.favsOrder) {
                0 -> CacheDB.INSTANCE.favsDAO().all
                1 -> CacheDB.INSTANCE.favsDAO().allID
                else -> CacheDB.INSTANCE.favsDAO().all
            }
    }

}
