package knf.kuma.favorite;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.preference.PreferenceManager;

import java.util.List;

import knf.kuma.database.CacheDB;
import knf.kuma.pojos.FavoriteObject;
import knf.kuma.retrofit.Factory;
import knf.kuma.retrofit.Repository;

/**
 * Created by Jordy on 08/01/2018.
 */

public class FavoriteViewModel extends ViewModel {

    public LiveData<List<FavoriteObject>> getData(Context context){
        switch (PreferenceManager.getDefaultSharedPreferences(context).getInt("favs_order",0)){
            default:
            case 0:
                return CacheDB.INSTANCE.favsDAO().getAll();
            case 1:
                return CacheDB.INSTANCE.favsDAO().getAllID();
        }
    }

}
