package knf.kuma.pojos;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import knf.kuma.commons.PatternUtil;
import knf.kuma.downloadservice.FileAccessHelper;

/**
 * Created by Jordy on 29/01/2018.
 */

@Entity
@TypeConverters(ExplorerObject.Converter.class)
public class ExplorerObject {
    @PrimaryKey
    public int key;
    public String img;
    public String link;
    public String fileName;
    public String name;
    public int count;
    public String path;
    public List<FileDownObj> chapters;

    public ExplorerObject(int key, String img, String link, String fileName, String name, int count, String path, List<FileDownObj> chapters) {
        this.key = key;
        this.img = img;
        this.link = link;
        this.fileName = fileName;
        this.name = name;
        this.count = count;
        this.path = path;
        this.chapters = chapters;
    }

    @Ignore
    public ExplorerObject(Context context, @Nullable AnimeObject object) throws IllegalStateException {
        if (object == null)
            throw new IllegalStateException("Anime not found!!!");
        this.key = object.key;
        this.img = object.img;
        this.link = object.link;
        this.fileName = object.fileName;
        this.name = object.name;
        File file = FileAccessHelper.INSTANCE.getDownloadsDirectory(object.fileName);
        this.path = file.getAbsolutePath();
        chapters = new ArrayList<>();
        for (File chap : file.listFiles()) {
            try {
                chapters.add(new FileDownObj(context, object.name, object.aid, PatternUtil.getNumFromfile(chap.getName()), chap));
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        this.count = chapters.size();
        if (count == 0)
            throw new IllegalStateException("Directory empty: " + object.fileName);
        Collections.sort(chapters);
    }

    public static class FileDownObj implements Comparable<FileDownObj> {
        public String title;
        public String chapter;
        public String aid;
        public String eid;
        public String path;
        public String time;
        public String fileName;
        public String link;

        public FileDownObj(Context context, String title, String aid, String chapter, File file) {
            this.title = title;
            this.chapter = chapter;
            this.aid = aid;
            this.eid = PatternUtil.getEidFromfile(file.getName());
            this.fileName = file.getName();
            this.path = file.getAbsolutePath();
            this.time = getTime(context, file);
            if (time.equals(""))
                throw new IllegalStateException("No duration");
            this.link = "https://animeflv.net/ver/" + fileName.replace("$", "/").replace(".mp4", "");
        }

        public String getChapTitle() {
            return title + " " + chapter;
        }

        private String getTime(Context context, File file) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(context, Uri.fromFile(file));
                long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                long hours = TimeUnit.MILLISECONDS.toHours(duration);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
                long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
                StringBuilder builder = new StringBuilder();
                if (hours > 0) {
                    builder.append(hours);
                    builder.append(":");
                }
                if (minutes <= 9) {
                    builder.append("0");
                }
                builder.append(minutes);
                builder.append(":");
                if (seconds <= 9) {
                    builder.append("0");
                }
                builder.append(seconds);
                return builder.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "??:??";
            }
        }

        @Override
        public int compareTo(@NonNull FileDownObj o) {
            return eid.compareTo(o.eid);
        }
    }

    public static class Converter {
        @TypeConverter
        public List<FileDownObj> StringToList(String s) {
            return new Gson().fromJson(s, new TypeToken<List<FileDownObj>>() {
            }.getType());
        }

        @TypeConverter
        public String ListToString(List<FileDownObj> list) {
            return new Gson().toJson(list, new TypeToken<List<FileDownObj>>() {
            }.getType());
        }
    }
}
