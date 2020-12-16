package knf.kuma.pojos;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import knf.kuma.commons.PatternUtil;
import knf.kuma.database.CacheDBWrap;
import knf.kuma.download.FileAccessHelper;
import knf.kuma.emision.AnimeSubObject;
import knf.kuma.explorer.ThumbServer;
import knf.kuma.explorer.creator.SubFile;
import xdroid.toaster.Toaster;

@Entity
@TypeConverters(ExplorerObject.Converter.class)
public class ExplorerObject {
    @PrimaryKey
    public int key;
    public String img;
    public String link;
    public String fileName;
    public String name;
    public String aid;
    public int count;
    public String path;
    public List<FileDownObj> chapters = new ArrayList<>();
    @Ignore
    private final MutableLiveData<List<FileDownObj>> liveData = new MutableLiveData<>();
    @Ignore
    private boolean isProcessed = false;
    @Ignore
    private boolean isProcessing = false;
    @Ignore
    private List<SubFile> file_list;

    public ExplorerObject(int key, String img, String link, String fileName, String name, String aid, int count, String path, List<FileDownObj> chapters) {
        this.key = key;
        this.img = img;
        this.link = link;
        this.fileName = fileName;
        this.name = name;
        this.aid = aid;
        this.count = count;
        this.path = path;
        this.file_list = FileAccessHelper.INSTANCE.getDownloadsDirectoryFiles(fileName);
        this.chapters = chapters;
    }

    @Ignore
    public ExplorerObject(ExplorerObject item) {
        this.key = item.key;
        this.img = item.img;
        this.link = item.link;
        this.fileName = item.fileName;
        this.name = item.name;
        this.aid = item.aid;
        this.count = item.count;
        this.path = item.path;
        this.file_list = FileAccessHelper.INSTANCE.getDownloadsDirectoryFiles(fileName);
        this.chapters = item.chapters;
    }

    @Ignore
    public ExplorerObject(@Nullable AnimeSubObject object) throws IllegalStateException {
        if (object == null)
            throw new IllegalStateException("Anime not found!!!");
        this.key = object.getKey();
        this.img = PatternUtil.INSTANCE.getCover(object.getAid());
        this.link = object.getLink();
        this.fileName = object.getFinalName();
        this.name = object.getName();
        this.aid = object.getAid();
        file_list = FileAccessHelper.INSTANCE.getDownloadsDirectoryFiles(object.getFileName());
        if (file_list.size() == 0) {
            throw new IllegalStateException("Directory empty: " + object.getFinalName());
        }
        this.count = file_list.size();
        this.path = "";
    }

    private void process(Context context) {
        isProcessing = true;
        AsyncTask.execute(() -> {
            try {
                chapters = new ArrayList<>();
                this.file_list = FileAccessHelper.INSTANCE.getDownloadsDirectoryFiles(fileName);
                for (SubFile chap : file_list)
                    try {
                        String file_name = chap.getName();
                        chapters.add(new FileDownObj(context, name, aid, PatternUtil.INSTANCE.getNumFromFile(file_name), file_name, chap));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                this.count = chapters.size();
                if (count == 0)
                    Log.e("Directory empty", fileName);
                Collections.sort(chapters);
                isProcessed = true;
                isProcessing = false;
                new Handler(Looper.getMainLooper()).post(() -> liveData.setValue(chapters));
                CacheDBWrap.INSTANCE.explorerDAO().update(this);
            } catch (Exception e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
                Toaster.toast("Error al obtener lista de episodios");
                isProcessed = true;
                isProcessing = false;
            }
        });
    }

    @NonNull
    public LiveData<List<FileDownObj>> getLiveData(Context context) {
        if (!isProcessed && !isProcessing) process(context);
        else if (isProcessed || chapters.size() > 0)
            new Handler(Looper.getMainLooper()).post(() -> liveData.setValue(chapters));
        return liveData;
    }

    public void clearLiveData(LifecycleOwner owner) {
        liveData.removeObservers(owner);
    }

    public static class FileDownObj implements Comparable<FileDownObj> {
        public String title;
        public String chapter;
        public String aid;
        public String eid;
        public SubFile file;
        public String time;
        public String fileName;
        public String link;
        public File thumb;

        FileDownObj(Context context, String title, String aid, String chapter, String name, SubFile file) {
            this.title = title;
            this.chapter = chapter;
            this.aid = aid;
            this.eid = PatternUtil.INSTANCE.getEidFromFile(name);
            this.fileName = name;
            this.file = file;
            this.time = getTime(context, file);
            if (time.equals(""))
                throw new IllegalStateException("No duration");
            this.link = "https://animeflv.net/ver/" + fileName.substring(fileName.indexOf("$") + 1).replace(".mp4", "");
        }

        public static String[] getTitles(List<FileDownObj> list) {
            List<String> names = new ArrayList<>();
            for (FileDownObj file : list) {
                names.add(file.getChapTitle());
            }
            return names.toArray(new String[]{});
        }

        public static Uri[] getUris(List<FileDownObj> list) {
            List<Uri> uris = new ArrayList<>();
            for (FileDownObj file : list) {
                uris.add(Uri.fromFile(FileAccessHelper.INSTANCE.getFile(file.fileName)));
            }
            return uris.toArray(new Uri[]{});
        }

        public String getChapTitle() {
            return title + " " + chapter;
        }

        public String getChapPreviewLink() {
            if (thumb == null)
                return "https://animeflv.net/uploads/animes/screenshots/" + aid + "/" + chapter + "/th_2.jpg";
            else
                return ThumbServer.INSTANCE.loadFile(thumb);
        }

        private String getTime(Context context, SubFile file) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(context, file.getFileUri());
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
            double num1 = Double.valueOf(chapter.substring(chapter.lastIndexOf(" ") + 1));
            double num2 = Double.valueOf(o.chapter.substring(o.chapter.lastIndexOf(" ") + 1));
            return Double.compare(num1, num2);
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

        @TypeConverter
        public String UriToString(Uri uri) {
            return uri.toString();
        }

        @TypeConverter
        public Uri StringToUri(String text) {
            return Uri.parse(text);
        }
    }
}
