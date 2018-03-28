package knf.kuma.recommended;

import java.util.List;

import knf.kuma.database.CacheDB;
import knf.kuma.pojos.GenreStatusObject;

/**
 * Created by jordy on 26/03/2018.
 */

public class RecommendHelper {
    public static void registerAll(List<String> list, RankType type) {
        for (String genre : list)
            register(genre, type);
    }

    public static void register(String name, RankType type) {
        GenreStatusObject status = getStatus(name);
        if (status.isBlocked()) return;
        switch (type) {
            case FAV:
                status.add(3);
                break;
            case UNFAV:
                status.sub(3);
                break;
            case FOLLOW:
                status.add(2);
                break;
            case UNFOLLOW:
                status.sub(2);
                break;
            case CHECK:
                status.add(1);
                break;
            case SEARCH:
                status.add(1);
                break;
        }
        CacheDB.INSTANCE.genresDAO().insertStatus(status);
    }

    public static void block(String name) {
        GenreStatusObject status = getStatus(name);
        status.block();
        CacheDB.INSTANCE.genresDAO().insertStatus(status);
    }

    public static void reset(String name) {
        GenreStatusObject status = getStatus(name);
        status.reset();
        CacheDB.INSTANCE.genresDAO().insertStatus(status);
    }

    private static GenreStatusObject getStatus(String name) {
        GenreStatusObject status = CacheDB.INSTANCE.genresDAO().getStatus(name);
        if (status == null) status = new GenreStatusObject(name);
        return status;
    }
}
