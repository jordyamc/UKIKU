package knf.kuma.favorite.objects

import knf.kuma.commons.PrefsUtil
import knf.kuma.pojos.FavoriteObject
import java.util.*

class FavSorter : Comparator<FavoriteObject> {

    override fun compare(o1: FavoriteObject, o2: FavoriteObject): Int {
        return when (PrefsUtil.favsOrder) {
            0 -> o1.name!!.compareTo(o2.name!!)
            1 -> o1.aid!!.compareTo(o2.aid!!)
            else -> o1.name!!.compareTo(o2.name!!)
        }
    }
}
