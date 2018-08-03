package knf.kuma.pojos;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import knf.kuma.commons.PatternUtil;
import knf.kuma.database.BaseConverter;
import knf.kuma.videoservers.Headers;

@Entity
@TypeConverters({BaseConverter.class})
public class DownloadObject {
    @Ignore
    public static final int PAUSED = -2;
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
    public String did;
    public String eta;
    public String speed;
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

    public DownloadObject(int key, String eid, String file, String link, String name, String chapter, String did, String eta, String speed, Headers headers, int progress, long d_bytes, long t_bytes, boolean canResume, int state) {
        this.key = key;
        this.eid = eid;
        this.file = file;
        this.link = link;
        this.name = PatternUtil.fromHtml(name);
        this.chapter = chapter;
        this.did = did;
        this.eta = eta;
        this.speed = speed;
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
        this.name = PatternUtil.fromHtml(name);
        this.addQueue = addQueue;
        this.chapter = chapter;
        this.did = "0";
        this.eta = "-1";
        this.speed = "0";
        this.title = name + chapter.substring(chapter.lastIndexOf(" "));
        this.progress = 0;
        this.d_bytes = 0;
        this.t_bytes = -1;
        this.canResume = false;
        this.state = PENDING;
    }

    @NonNull
    public static DownloadObject fromRecent(RecentObject object) {
        return new DownloadObject(object.eid, object.getFileName(), PatternUtil.fromHtml(object.name), object.chapter, false);
    }

    @NonNull
    public static DownloadObject fromChapter(AnimeObject.WebInfo.AnimeChapter chapter, boolean addQueue) {
        return new DownloadObject(chapter.eid, chapter.getFileName(), PatternUtil.fromHtml(chapter.name), chapter.number, addQueue);
    }

    public boolean isDownloading() {
        return state == DOWNLOADING || state == PENDING;
    }

    public int getDid() {
        return Integer.parseInt(did);
    }

    public void setDid(int did) {
        this.did = String.valueOf(did);
    }

    public long getEta() {
        if (eta == null) return -1;
        return Long.parseLong(eta);
    }

    public void setEta(long eta) {
        this.eta = String.valueOf(eta);
    }

    public long getSpeed() {
        if (speed == null) return 0;
        return Long.parseLong(speed);
    }

    public void setSpeed(long speed) {
        this.speed = String.valueOf(speed);
    }

    public String getTime() {
        try {
            long duration = getEta();
            if (duration == -1)
                return "Desconocido";
            else if (duration == -2)
                return "Moviendo...";
            long hours = TimeUnit.MILLISECONDS.toHours(duration);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
            long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
            StringBuilder builder = new StringBuilder();
            if (hours > 0) {
                builder.append(hours);
                builder.append("h");
            }
            if (minutes > 0) {
                if (minutes <= 9)
                    builder.append("0");
                builder.append(minutes);
                builder.append("m");
            }
            if (seconds <= 9) {
                builder.append("0");
            }
            builder.append(seconds);
            builder.append("s");
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
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
