package knf.kuma.recents;

import android.content.Context;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.RecentObject;
import knf.kuma.retrofit.Repository;

public class RecentsViewModel extends ViewModel {
    private Repository repository=new Repository();

    public RecentsViewModel() {
    }

    public void reload(Context context) {
        repository.reloadRecents(context);
    }

    public LiveData<List<RecentObject>> getDBLiveData(){return CacheDB.INSTANCE.recentsDAO().getObjects();}
}
