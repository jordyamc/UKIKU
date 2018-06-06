package knf.kuma.pojos;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import knf.kuma.database.BaseConverter;

@Entity
@TypeConverters({BaseConverter.class})
public class QueueObject {
    @PrimaryKey
    public int id;
    public boolean isFile;
    public Uri uri;
    public long time;
    @Embedded
    public AnimeObject.WebInfo.AnimeChapter chapter;

    public QueueObject(int id, boolean isFile, Uri uri, long time, AnimeObject.WebInfo.AnimeChapter chapter) {
        this.id = id;
        this.isFile = isFile;
        this.uri = uri;
        this.time = time;
        this.chapter = chapter;
    }

    @Ignore
    public QueueObject(Uri uri, boolean isFile, AnimeObject.WebInfo.AnimeChapter chapter) {
        this.id = chapter.key;
        this.uri = uri;
        this.isFile = isFile;
        this.time = System.currentTimeMillis();
        this.chapter = chapter;
    }

    public static List<QueueObject> getOne(List<QueueObject> list) {
        List<String> aids = new ArrayList<>();
        List<QueueObject> n_list = new ArrayList<>();
        for (QueueObject object : list) {
            if (!aids.contains(object.chapter.aid)) {
                aids.add(object.chapter.aid);
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

    public static Uri[] getUris(List<QueueObject> list) {
        List<Uri> uris = new ArrayList<>();
        for (QueueObject object : list)
            uris.add(object.uri);
        return uris.toArray(new Uri[]{});
    }

    public String getTitle() {
        return chapter.name + chapter.number.substring(chapter.number.lastIndexOf(" "));
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


