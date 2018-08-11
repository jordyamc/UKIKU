package knf.kuma.favorite;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import knf.kuma.database.CacheDB;
import knf.kuma.favorite.objects.FavSorter;
import knf.kuma.favorite.objects.InfoContainer;
import knf.kuma.pojos.FavSection;
import knf.kuma.pojos.FavoriteObject;

public class FavSectionHelper {
    private static InfoContainer infoContainer = new InfoContainer();
    private static List<FavoriteObject> current_list = new ArrayList<>();
    private static MutableLiveData<List<FavoriteObject>> liveData = new MutableLiveData<>();

    public static LiveData<List<FavoriteObject>> init() {
        reload();
        return getLiveData();
    }

    public static InfoContainer getInfoContainer(FavoriteObject object) {
        infoContainer.reload(object);
        return infoContainer;
    }

    public static List<FavoriteObject> getCurrentList() {
        return current_list;
    }

    private static LiveData<List<FavoriteObject>> getLiveData() {
        return liveData;
    }

    private static void setLiveData(List<FavoriteObject> list) {
        new Handler(Looper.getMainLooper()).post(() -> liveData.setValue(list));
    }

    private static List<FavoriteObject> getList() {
        List<FavoriteObject> list = new ArrayList<>();
        String current_section = null;
        List<FavoriteObject> section = new ArrayList<>();
        List<FavoriteObject> no_section = new ArrayList<>();
        for (FavoriteObject object : CacheDB.INSTANCE.favsDAO().getByCategory()) {
            if (current_section == null || !current_section.equals(object.category)) {
                if (current_section != null && !current_section.equals(object.category)) {
                    if (!current_section.equals(FavoriteObject.CATEGORY_NONE)) {
                        list.add(new FavSection(current_section));
                        Collections.sort(section, new FavSorter());
                        list.addAll(section);
                    } else no_section = new ArrayList<>(section);
                    section = new ArrayList<>();
                }
                current_section = object.category;
                section.add(object);
            } else if (current_section.equals(object.category))
                section.add(object);
        }
        if (current_section != null)
            if (!current_section.equals(FavoriteObject.CATEGORY_NONE)) {
                list.add(new FavSection(current_section));
                Collections.sort(section, new FavSorter());
                list.addAll(section);
            } else
                no_section = new ArrayList<>(section);
        if (no_section.size() > 0) {
            list.add(new FavSection(FavoriteObject.CATEGORY_NONE));
            Collections.sort(no_section, new FavSorter());
            list.addAll(no_section);
        }
        infoContainer.setLists(current_list, list);
        current_list = list;
        return list;
    }

    public static void reload() {
        AsyncTask.execute(() -> setLiveData(getList()));
    }
}
