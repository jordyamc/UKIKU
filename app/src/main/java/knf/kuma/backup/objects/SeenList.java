package knf.kuma.backup.objects;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.pojos.AnimeObject;
import xdroid.toaster.Toaster;

/**
 * Created by jordy on 01/03/2018.
 */

public class SeenList {
    @SerializedName("response")
    String response;
    @SerializedName("vistos")
    String vistos;
    List<SeenObj> list;

    public static List<AnimeObject.WebInfo.AnimeChapter> decode(InputStream inputStream) {
        int errorCount = 0;
        AnimeDAO dao = CacheDB.INSTANCE.animeDAO();
        SeenList seenList = new Gson().fromJson(new InputStreamReader(inputStream), new TypeToken<SeenList>() {
        }.getType());
        seenList.deserialize();
        int totalCount = seenList.list.size();
        List<AnimeObject.WebInfo.AnimeChapter> chapters = new ArrayList<>();
        AnimeObject animeObject = null;
        for (SeenObj obj : seenList.list) {
            if (animeObject == null || !animeObject.aid.equals(obj.aid))
                animeObject = dao.getByAid(obj.aid);
            List<AnimeObject.WebInfo.AnimeChapter> chapterList = animeObject.chapters;
            boolean found = false;
            for (AnimeObject.WebInfo.AnimeChapter chapter : chapterList) {
                if (chapter.number.endsWith(" " + obj.num)) {
                    chapters.add(chapter);
                    found = true;
                    break;
                }
            }
            if (!found)
                errorCount++;
        }
        Toaster.toast("Migrados correctamente " + (totalCount - errorCount) + "/" + totalCount);
        return chapters;
    }

    private void deserialize() {
        list = new ArrayList<>();
        Log.e("Seen", vistos);
        String[] els = vistos.replace("E", "").split(":::");
        for (String el : els) {
            if (!el.equals("")) {
                String[] spl = el.split("_");
                list.add(new SeenObj(spl[0], spl[1]));
            }
        }
        Collections.sort(list);
    }

    class SeenObj implements Comparable<SeenObj> {
        String aid;
        String num;

        SeenObj(String aid, String num) {
            this.aid = aid;
            this.num = num;
        }

        @Override
        public int compareTo(@NonNull SeenObj o) {
            int bname = aid.compareTo(o.aid);
            if (bname != 0) {
                return bname;
            } else {
                return num.compareTo(o.num);
            }
        }
    }
}
