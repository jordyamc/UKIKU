package knf.kuma.pojos;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import knf.kuma.animeinfo.ktx.ExtensionsKt;
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
    public String server;
    public Headers headers;
    @Ignore
    public String title;
    @Ignore
    public boolean addQueue = false;
    public int progress;
    public long d_bytes;
    public long t_bytes;
    public long time;
    public boolean canResume;
    public int state;

    public DownloadObject(int key, String eid, String file, String link, String name, String chapter, String did, String eta, String speed, String server, Headers headers, int progress, long d_bytes, long t_bytes, boolean canResume, int state) {
        this.key = key;
        this.eid = eid;
        this.file = file;
        this.link = link;
        this.name = PatternUtil.INSTANCE.fromHtml(name);
        this.chapter = chapter;
        this.did = did;
        this.eta = eta;
        this.speed = speed;
        this.server = server;
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
        this.name = PatternUtil.INSTANCE.fromHtml(name);
        this.addQueue = addQueue;
        this.chapter = chapter;
        this.did = "0";
        this.eta = "-1";
        this.speed = "0";
        this.title = name + chapter.substring(chapter.lastIndexOf(" "));
        this.progress = 0;
        this.d_bytes = 0;
        this.t_bytes = -1;
        this.time = System.currentTimeMillis();
        this.canResume = false;
        this.state = PENDING;
    }

    @NonNull
    public static DownloadObject fromRecent(RecentObject object) {
        return new DownloadObject(object.eid, object.getFileName(), PatternUtil.INSTANCE.fromHtml(object.name), object.chapter, false);
    }

    @NonNull
    public static DownloadObject fromChapter(AnimeObject.WebInfo.AnimeChapter chapter, boolean addQueue) {
        return new DownloadObject(chapter.eid, ExtensionsKt.getFileName(chapter), PatternUtil.INSTANCE.fromHtml(chapter.name), chapter.number, addQueue);
    }

    public boolean isDownloading() {
        return state == DOWNLOADING || state == PENDING;
    }

    public int getDid() {
        if (did == null) return 0;
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

    private String getSpeed() {
        if (speed == null) return "0kB/s";
        long s = Long.parseLong(speed);
        if (s < 1024)
            return s + "B/s";
        else if (s < 1024000)
            return String.format(Locale.getDefault(), "%.0fkB/s", s / 1024f);
        else
            return String.format(Locale.getDefault(), "%.1fMB/s", s / 1024000f);
    }

    public void setSpeed(long speed) {
        this.speed = String.valueOf(speed);
    }

    public String getTime() {
        try {
            long duration = getEta();
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

    public String getSubtext() {
        if (!canResume)
            return getSize();
        long duration = getEta();
        if (duration == -1)
            return "Desconocido";
        else if (duration == -2)
            return "Moviendo...";
        else {
            return getTime() + ", " + getSpeed();
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

    public String getDownloadServer() {
        if ("".equals(server.trim()))
            return "Desconocido";
        else
            return server;
    }

    private String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format(Locale.US, "%.1f%sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    @Override
    public int hashCode() {
        return (file + link + key + state + eta + speed + progress + d_bytes + t_bytes).hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof DownloadObject
                && key == ((DownloadObject) obj).key
                && state == ((DownloadObject) obj).state
                && file.equals(((DownloadObject) obj).file)
                && link.equals(((DownloadObject) obj).link)
                && eta.equals(((DownloadObject) obj).eta)
                && speed.equals(((DownloadObject) obj).speed)
                && progress == ((DownloadObject) obj).progress
                && d_bytes == ((DownloadObject) obj).d_bytes
                && t_bytes == ((DownloadObject) obj).t_bytes;
    }
}
