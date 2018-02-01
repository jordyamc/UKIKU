package knf.kuma.recents;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import java.util.List;

import knf.kuma.database.CacheDB;
import knf.kuma.pojos.RecentObject;
import knf.kuma.retrofit.Repository;

/**
 * Created by Jordy on 03/01/2018.
 */

public class RecentsViewModel extends ViewModel {
    private Repository repository=new Repository();

    public RecentsViewModel() {
    }

    public LiveData<List<RecentObject>> getLiveData(){
        return repository.getRecents();
    }

    public void reload(){repository.reloadRecents();}

    public LiveData<List<RecentObject>> getDBLiveData(){return CacheDB.INSTANCE.recentsDAO().getObjects();}
}
