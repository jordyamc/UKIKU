package knf.kuma.favorite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import knf.kuma.database.CacheDB
import knf.kuma.favorite.objects.FavSorter
import knf.kuma.favorite.objects.InfoContainer
import knf.kuma.pojos.FavSection
import knf.kuma.pojos.FavoriteObject
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.doAsync
import java.util.*

object FavSectionHelper {
    private val infoContainer = InfoContainer()
    var currentList: MutableList<FavoriteObject> = ArrayList()
        private set
    private val liveData = MutableLiveData<MutableList<FavoriteObject>>()

    private val list: MutableList<FavoriteObject>
        get() {
            val list = ArrayList<FavoriteObject>()
            var currentSection: String? = null
            var section: MutableList<FavoriteObject> = ArrayList()
            var noSection: MutableList<FavoriteObject> = ArrayList()
            for (favoriteObject in CacheDB.INSTANCE.favsDAO().byCategory) {
                if (currentSection == null || currentSection != favoriteObject.category) {
                    if (currentSection != null && currentSection != favoriteObject.category) {
                        if (currentSection != FavoriteObject.CATEGORY_NONE) {
                            list.add(FavSection(currentSection))
                            Collections.sort(section, FavSorter())
                            list.addAll(section)
                        } else
                            noSection = ArrayList(section)
                        section = ArrayList()
                    }
                    currentSection = favoriteObject.category
                    section.add(favoriteObject)
                } else if (currentSection == favoriteObject.category)
                    section.add(favoriteObject)
            }
            if (currentSection != null)
                if (currentSection != FavoriteObject.CATEGORY_NONE) {
                    list.add(FavSection(currentSection))
                    Collections.sort(section, FavSorter())
                    list.addAll(section)
                } else
                    noSection = ArrayList(section)
            if (noSection.isNotEmpty()) {
                list.add(FavSection(FavoriteObject.CATEGORY_NONE))
                Collections.sort(noSection, FavSorter())
                list.addAll(noSection)
            }
            infoContainer.setLists(currentList, list)
            currentList = list
            return list
        }

    fun init(): LiveData<MutableList<FavoriteObject>> {
        reload()
        return getLiveData()
    }

    fun getInfoContainer(favoriteObject: FavoriteObject?): InfoContainer {
        infoContainer.reload(favoriteObject)
        return infoContainer
    }

    private fun getLiveData(): LiveData<MutableList<FavoriteObject>> {
        return liveData
    }

    private fun setLiveData(list: MutableList<FavoriteObject>) {
        launch(UI) { liveData.setValue(list) }
    }

    fun reload() {
        doAsync { setLiveData(list) }
    }
}
