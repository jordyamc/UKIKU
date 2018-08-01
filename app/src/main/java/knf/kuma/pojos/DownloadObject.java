package knf.kuma.pojos;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import java.util.Locale;

import knf.kuma.database.BaseConverter;
import knf.kuma.videoservers.Headers;

@Entity
@TypeConverters({BaseConverter.class})
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
    public Headers headers;
    @Ignore
    public String title;
    @Ignore
    public boolean addQueue = false;
    public int progress;
    public long d_bytes;
    public long t_bytes;
    public boolean canResume;
    public int state;

    public DownloadObject(int key, String eid, String file, String link, String name, String chapter, Headers headers, int progress, long d_bytes, long t_bytes, boolean canResume, int state) {
        this.key = key;
        this.eid = eid;
        this.file = file;
        this.link = link;
        this.name = name;
        this.chapter = chapter;
        this.headers = headers;
        this.title = name + chapter.substring(chapter.lastIndexOf(" "));
        this.progress = progress;
        this.d_bytes = d_bytes;
        this.t_bytes = t_bytes;
        this.canResume = canResume;
        this.state = state;
    }

    @Ignore
    public DownloadObject(String eid, String file, String name, String chapter, boolean addQueue) {
        this.eid = eid;
        this.file = file;
        this.name = name;
        this.addQueue = addQueue;
        this.chapter = chapter;
        this.title = name + chapter.substring(chapter.lastIndexOf(" "));
        this.progress = 0;
        this.d_bytes = 0;
        this.t_bytes = -1;
        this.canResume = false;
        this.state = PENDING;
    }

    @NonNull
    public static DownloadObject fromRecent(RecentObject object) {
        return new DownloadObject(object.eid, object.getFileName(), object.name, object.chapter, false);
    }

    @NonNull
    public static DownloadObject fromChapter(AnimeObject.WebInfo.AnimeChapter chapter, boolean addQueue) {
        return new DownloadObject(chapter.eid, chapter.getFileName(), chapter.name, chapter.number, addQueue);
    }

    public boolean isDownloading() {
        return state == DOWNLOADING || state == PENDING;
    }

    public String getSize() {
        /*if (t_bytes == -1)
            return "";
        String size = formatSize(t_bytes);
        if (size.endsWith(".0"))
            size = size.substring(0, size.lastIndexOf("."));
        return size;*/
        return progress + "%";
    }

    private String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format(Locale.US, "%.1f%sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    public void reset() {
        progress = 0;
        d_bytes = 0;
        t_bytes = -1;
        canResume = false;
    }

}
