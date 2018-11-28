package knf.kuma.pojos;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import knf.kuma.database.CacheDB;

@Entity
public class RecordObject {
    @PrimaryKey
    public int key;
    public String name;
    public String chapter;
    public String aid;
    public String eid;
    public long date;
    @Ignore
    public transient AnimeObject animeObject;

    public RecordObject(int key, String name, String chapter, String aid, String eid, long date) {
        this.key = key;
        this.name = name;
        this.chapter = chapter;
        this.aid = aid;
        this.eid = eid;
        this.date = date;
        this.animeObject = CacheDB.INSTANCE.animeDAO().getByAid(aid);
    }

    @Ignore
    private RecordObject() {
    }

    @Ignore
    public static RecordObject fromRecent(RecentObject recentObject) {
        RecordObject object = new RecordObject();
        object.key = Integer.parseInt(recentObject.aid);
        object.name = recentObject.name;
        object.chapter = recentObject.chapter;
        object.aid = recentObject.aid;
        object.eid = recentObject.eid;
        object.date = System.currentTimeMillis();
        return object;
    }

    @Ignore
    public static RecordObject fromDownloaded(ExplorerObject.FileDownObj downloadedObject) {
        RecordObject object = new RecordObject();
        object.key = Integer.parseInt(downloadedObject.aid);
        object.name = downloadedObject.title;
        object.chapter = "Episodio " + downloadedObject.chapter;
        object.aid = downloadedObject.aid;
        object.eid = downloadedObject.eid;
        object.date = System.currentTimeMillis();
        return object;
    }

    @Ignore
    public static RecordObject fromChapter(AnimeObject.WebInfo.AnimeChapter chapter) {
        RecordObject object = new RecordObject();
        object.key = Integer.parseInt(chapter.aid);
        object.name = chapter.name;
        object.chapter = chapter.number;
        object.aid = chapter.aid;
        object.eid = chapter.eid;
        object.date = System.currentTimeMillis();
        return object;
    }
}
