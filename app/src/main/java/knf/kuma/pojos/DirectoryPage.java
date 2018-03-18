package knf.kuma.pojos;

import android.content.Context;
import android.util.Log;

import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.Network;
import knf.kuma.database.dao.AnimeDAO;
import pl.droidsonroids.jspoon.Jspoon;
import pl.droidsonroids.jspoon.annotation.Selector;

public class DirectoryPage {
    @Selector(value = "article.Anime.alt.B > a", attr = "href")
    public List<String> links = new ArrayList<>();

    public List<AnimeObject> getAnimes(Context context, AnimeDAO animeDAO, Jspoon jspoon, UpdateInterface updateInterface) {
        List<AnimeObject> animeObjects = new ArrayList<>();
        for (String link : links) {
            if (Network.isConnected()) {
                if (!animeDAO.existLink("https://animeflv.net" + link))
                    try {
                        AnimeObject.WebInfo webInfo = jspoon.adapter(AnimeObject.WebInfo.class).fromHtml(Jsoup.connect("https://animeflv.net" + link).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).timeout(3000).get().outerHtml());
                        animeObjects.add(new AnimeObject("https://animeflv.net" + link, webInfo));
                        Log.e("Directory Getter", "Added: https://animeflv.net" + link);
                        updateInterface.onAdd();
                    } catch (Exception e) {
                        Log.e("Directory Getter", "Error adding: https://animeflv.net" + link +"\nCause: "+e.getMessage());
                        updateInterface.onError();
                    }
            } else {
                Log.e("Directory Getter", "Abort: No internet");
                break;
            }
        }
        return animeObjects;
    }

    public List<AnimeObject> getAnimesRecreate(Context context, Jspoon jspoon, UpdateInterface updateInterface) {
        List<AnimeObject> animeObjects = new ArrayList<>();
        for (String link : links) {
            if (Network.isConnected()) {
                try {
                    AnimeObject.WebInfo webInfo = jspoon.adapter(AnimeObject.WebInfo.class).fromHtml(Jsoup.connect("https://animeflv.net" + link).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).timeout(3000).get().outerHtml());
                    animeObjects.add(new AnimeObject("https://animeflv.net" + link, webInfo));
                    Log.e("Directory Getter", "Replaced: https://animeflv.net" + link);
                    updateInterface.onAdd();
                } catch (Exception e) {
                    Log.e("Directory Getter", "Error replacing: https://animeflv.net" + link +"\nCause: "+e.getMessage());
                    updateInterface.onError();
                }
            } else {
                Log.e("Directory Getter", "Abort: No internet");
                break;
            }
        }
        return animeObjects;
    }

    public interface UpdateInterface {
        void onAdd();

        void onError();
    }
}
