package knf.kuma.pojos;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import knf.kuma.database.BaseConverter;

/**
 * Created by Jordy on 10/01/2018.
 */

@Entity
@TypeConverters({DownloadObject.Converter.class, BaseConverter.class})
public class DownloadObject {
    @Ignore
    public static final int PENDING = -1;
    @Ignore
    public static final int DOWNLOADING = 0;
    @Ignore
    public static final int COMPLETED = 4;
    @PrimaryKey(autoGenerate = true)
    public int key;
    public String eid;
    public String file;
    public String link;
    public String name;
    public String chapter;
    public int progress;
    public long d_bytes;
    public long t_bytes;
    public boolean canResume;
    public int state;

    public DownloadObject(int key, String eid, String file, String link, String name, String chapter, int progress, long d_bytes, long t_bytes, boolean canResume, int state) {
        this.key = key;
        this.eid = eid;
        this.file = file;
        this.link = link;
        this.name = name;
        this.chapter = chapter;
        this.progress = progress;
        this.d_bytes = d_bytes;
        this.t_bytes = t_bytes;
        this.canResume = canResume;
        this.state = state;
    }

    @Ignore
    public DownloadObject(String eid, String file, String name, String chapter) {
        this.eid = eid;
        this.file = file;
        this.name = name;
        this.chapter = chapter;
        this.progress = 0;
        this.d_bytes = 0;
        this.t_bytes = -1;
        this.canResume = false;
        this.state = PENDING;
    }

    @NonNull
    public static DownloadObject fromRecent(RecentObject object) {
        return new DownloadObject(object.eid, object.getFileName(), object.name, object.chapter);
    }

    @NonNull
    public static DownloadObject fromChapter(AnimeObject.WebInfo.AnimeChapter chapter) {
        return new DownloadObject(chapter.eid, chapter.getFileName(), chapter.name, chapter.number);
    }

    public boolean isDownloading() {
        return state == DOWNLOADING || state == PENDING;
    }

    public void reset() {
        progress = 0;
        d_bytes = 0;
        t_bytes = -1;
        canResume = false;
    }

    public static class Converter {
        @TypeConverter
        public String uriToString(Uri uri) {
            return new Gson().toJson(uri);
        }

        @TypeConverter
        public Uri stringToUri(String s) {
            return Uri.parse(s);
        }
    }

}
