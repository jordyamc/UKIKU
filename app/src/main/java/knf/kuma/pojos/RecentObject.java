package knf.kuma.pojos;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import knf.kuma.commons.PatternUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.download.FileAccessHelper;
import pl.droidsonroids.jspoon.annotation.Selector;

@Entity
public class RecentObject {
    @PrimaryKey(autoGenerate = true)
    public int key;
    @Ignore
    public String aid;
    @Ignore
    public String eid;
    @Ignore
    public String name;
    @Ignore
    public String chapter;
    @Ignore
    public String url;
    @Ignore
    public String anime;
    @Ignore
    public String img;
    @Ignore
    public boolean isNew;
    @Ignore
    public boolean isDownloading;
    @Ignore
    public boolean isChapterDownloaded;
    @Ignore
    public int downloadState;
    @Ignore
    public AnimeObject animeObject;
    @Embedded
    public WebInfo webInfo;

    public RecentObject(int key, WebInfo webInfo) {
        this.key = key;
        populate(webInfo);
    }

    private RecentObject(AnimeDAO dao, WebInfo webInfo) {
        this.webInfo = webInfo;
        populate(dao, webInfo);
    }

    public static List<RecentObject> create(List<WebInfo> infos) {
        AnimeDAO dao = CacheDB.INSTANCE.animeDAO();
        List<RecentObject> objects = new ArrayList<>();
        for (WebInfo info : infos) {
            try {
                objects.add(new RecentObject(dao, info));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return objects;
    }

    public String getFileName() {
        return eid + "$" + PatternUtil.getFileName(url);
    }

    public String getEpTitle() {
        return name + chapter.substring(chapter.lastIndexOf(" "));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RecentObject && (
                eid.equals(((RecentObject) obj).eid) ||
                        (name.equals(((RecentObject) obj).name) && chapter.equals(((RecentObject) obj).chapter)));
    }

    @Override
    public int hashCode() {
        return name.hashCode() + chapter.hashCode();
    }

    private void populate(WebInfo webInfo) {
        this.aid = webInfo.aid;
        this.eid = webInfo.eid;
        this.name = PatternUtil.fromHtml(webInfo.name);
        this.chapter = webInfo.chapter.trim();
        this.url = "https://animeflv.net" + webInfo.url;
        this.img = "https://animeflv.net" + webInfo.img.replace("thumbs", "covers");
        this.isNew = chapter.matches("^.* [10]$");
        this.anime = PatternUtil.getAnimeUrl(this.url, this.aid);
        File file = FileAccessHelper.INSTANCE.getFile(getFileName());
        DownloadObject downloadObject = CacheDB.INSTANCE.downloadsDAO().getByEid(eid);
        this.isChapterDownloaded = (file != null) && file.exists();
        this.isDownloading = downloadObject != null && downloadObject.state == DownloadObject.DOWNLOADING;
        if (downloadObject != null) {
            this.downloadState = downloadObject.state;
        } else {
            this.downloadState = -8;
        }
        this.animeObject = CacheDB.INSTANCE.animeDAO().getByAid(aid);
    }

    private void populate(AnimeDAO dao, WebInfo webInfo) {
        if (!isNumeric(webInfo.aid) || !isNumeric(webInfo.eid))
            throw new IllegalStateException("Aid and Eid must be numbers");
        this.aid = webInfo.aid;
        this.eid = webInfo.eid;
        this.name = PatternUtil.fromHtml(webInfo.name);
        this.chapter = webInfo.chapter.trim();
        this.url = "https://animeflv.net" + webInfo.url;
        this.img = "https://animeflv.net" + webInfo.img.replace("thumbs", "covers");
        this.isNew = chapter.matches("^.* 1$| 0$| Preestreno$| [10] ?:.*$");
        this.anime = PatternUtil.getAnimeUrl(this.url, this.aid);
        File file = FileAccessHelper.INSTANCE.getFile(getFileName());
        DownloadObject downloadObject = CacheDB.INSTANCE.downloadsDAO().getByEid(eid);
        this.isChapterDownloaded = (file != null) && file.exists();
        this.isDownloading = downloadObject != null && downloadObject.state == DownloadObject.DOWNLOADING;
        if (downloadObject != null) {
            this.downloadState = downloadObject.state;
        } else {
            this.downloadState = -8;
        }
        this.animeObject = dao.getByAid(aid);
    }

    private boolean isNumeric(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class WebInfo {
        @Selector(value = "img", attr = "src", format = "/(\\d+)[/.]")
        public String aid;
        @Selector(value = "a", attr = "href", format = "/(\\d+)[/.]")
        public String eid;
        @Selector(value = "img", attr = "alt")
        public String name;
        @Selector(".Capi")
        public String chapter;
        @Selector(value = "a", attr = "href")
        public String url;
        @Selector(value = "img", attr = "src")
        public String img;
    }
}
