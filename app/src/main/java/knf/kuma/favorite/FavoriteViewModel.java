package knf.kuma.favorite;

import android.content.Context;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.FavoriteObject;

public class FavoriteViewModel extends ViewModel {

    public LiveData<List<FavoriteObject>> getData(Context context) {
        if (PrefsUtil.INSTANCE.showFavSections())
            return FavSectionHelper.init();
        else
            switch (PrefsUtil.INSTANCE.getFavsOrder()) {
                default:
                case 0:
                    return CacheDB.INSTANCE.favsDAO().getAll();
                case 1:
                    return CacheDB.INSTANCE.favsDAO().getAllID();
            }
    }

}
