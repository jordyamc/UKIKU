package knf.kuma.pojos;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.text.Html;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import knf.kuma.commons.PatternUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.RecentsDAO;
import knf.kuma.downloadservice.FileAccessHelper;
import pl.droidsonroids.jspoon.annotation.Selector;

/**
 * Created by Jordy on 03/01/2018.
 */

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

    private RecentObject(AnimeDAO dao,WebInfo webInfo) {
        this.webInfo = webInfo;
        populate(dao,webInfo);
    }

    public static List<RecentObject> create(List<WebInfo> infos) {
        AnimeDAO dao=CacheDB.INSTANCE.animeDAO();
        List<RecentObject> objects = new ArrayList<>();
        for (WebInfo info : infos) {
            objects.add(new RecentObject(dao,info));
        }
        return objects;
    }

    public String getFileName() {
        return eid+"$"+PatternUtil.getFileName(url);
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
        this.name = Html.fromHtml(webInfo.name).toString();
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
        this.animeObject = CacheDB.INSTANCE.animeDAO().getByAid(aid);
    }

    private void populate(AnimeDAO dao,WebInfo webInfo) {
        this.aid = webInfo.aid;
        this.eid = webInfo.eid;
        this.name = Html.fromHtml(webInfo.name).toString();
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
