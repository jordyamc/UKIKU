package knf.kuma.backup.objects;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.FavoriteObject;
import xdroid.toaster.Toaster;

public class FavList {
    @SerializedName("response")
    String response;
    @SerializedName("favs")
    List<FavSection> favs;

    public static List<FavoriteObject> decode(InputStream inputStream) {
        int totalCount = 0;
        int errorCount = 0;
        AnimeDAO dao = CacheDB.INSTANCE.animeDAO();
        FavList favList = new Gson().fromJson(new InputStreamReader(inputStream), new TypeToken<FavList>() {
        }.getType());
        List<FavoriteObject> favs = new ArrayList<>();
        for (FavSection section : favList.favs) {
            totalCount += section.list.size();
            for (FavEntry favEntry : section.list) {
                AnimeObject object = dao.getByAid(favEntry.aid);
                if (object != null)
                    favs.add(new FavoriteObject(object));
                else errorCount++;
            }
        }
        Toaster.toast("Migrados correctamente " + (totalCount - errorCount) + "/" + totalCount);
        return favs;
    }

    static class FavSection {
        @SerializedName("name")
        String name;
        @SerializedName("list")
        List<FavEntry> list;
    }

    static class FavEntry {
        @SerializedName("title")
        String title;
        @SerializedName("aid")
        String aid;
        @SerializedName("section")
        String section;
        @SerializedName("order")
        int order;
    }
}
