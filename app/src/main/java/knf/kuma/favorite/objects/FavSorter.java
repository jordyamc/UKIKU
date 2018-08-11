package knf.kuma.favorite.objects;

import java.util.Comparator;

import knf.kuma.commons.PrefsUtil;
import knf.kuma.pojos.FavoriteObject;

public class FavSorter implements Comparator<FavoriteObject> {

    @Override
    public int compare(FavoriteObject o1, FavoriteObject o2) {
        switch (PrefsUtil.INSTANCE.getFavsOrder()) {
            default:
            case 0:
                return o1.name.compareTo(o2.name);
            case 1:
                return o1.aid.compareTo(o2.aid);
        }
    }
}
