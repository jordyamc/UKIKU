package knf.kuma.pojos;

import android.net.Uri;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.RoomWarnings;
import androidx.room.TypeConverters;
import knf.kuma.database.BaseConverter;
import knf.kuma.database.CacheDB;

@Entity
@TypeConverters({BaseConverter.class})
@SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
public class QueueObject implements Serializable {
    @PrimaryKey
    public int id;
    public boolean isFile;
    public String uri;
    public long time;
    @Embedded
    public AnimeObject.WebInfo.AnimeChapter chapter;
    @Ignore
    public int count = -1;

    public QueueObject(int id, boolean isFile, String uri, long time, AnimeObject.WebInfo.AnimeChapter chapter) {
        this.id = id;
        this.isFile = isFile;
        this.uri = uri;
        this.time = time;
        this.chapter = chapter;
    }

    @Ignore
    public QueueObject(Uri uri, boolean isFile, AnimeObject.WebInfo.AnimeChapter chapter) {
        this.id = chapter.key;
        this.uri = uri.toString();
        this.isFile = isFile;
        this.time = System.currentTimeMillis();
        this.chapter = chapter;
    }

    public static Uri[] getUris(List<QueueObject> list) {
        List<Uri> uris = new ArrayList<>();
        for (QueueObject object : list)
            uris.add(object.getUri());
        return uris.toArray(new Uri[]{});
    }

    public static List<QueueObject> getOne(List<QueueObject> list) {
        List<String> aids = new ArrayList<>();
        List<QueueObject> n_list = new ArrayList<>();
        for (QueueObject object : list) {
            if (!aids.contains(object.chapter.aid)) {
                aids.add(object.chapter.aid);
                object.count = CacheDB.INSTANCE.queueDAO().countAlone(object.chapter.aid);
                n_list.add(object);
            }
        }
        return n_list;
    }

    public static String[] getTitles(List<QueueObject> list) {
        List<String> titles = new ArrayList<>();
        for (QueueObject object : list)
            titles.add(object.getTitle());
        return titles.toArray(new String[]{});
    }

    public Uri getUri() {
        return Uri.parse(uri);
    }

    public String getTitle() {
        try {
            return chapter.name + chapter.number.substring(chapter.number.lastIndexOf(" "));
        } catch (Exception e) {
            return chapter.name;
        }
    }

    @Override
    public int hashCode() {
        return chapter.eid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof QueueObject && chapter.eid.equals(((QueueObject) obj).chapter.eid);
    }

    public boolean equalsAnime(Object obj) {
        return obj instanceof QueueObject && chapter.aid.equals(((QueueObject) obj).chapter.aid);
    }
}


