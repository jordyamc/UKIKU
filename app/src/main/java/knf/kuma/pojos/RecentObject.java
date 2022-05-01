package knf.kuma.pojos;

import static java.lang.Math.abs;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

import knf.kuma.commons.FileWrapper;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.database.CacheDBWrap;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.search.SearchObject;
import pl.droidsonroids.jspoon.annotation.Selector;

@Entity
public class RecentObject {
    @PrimaryKey
    public int key;
    @Ignore
    @NonNull
    public String aid = "";
    @Ignore
    @NonNull
    public String eid = "";
    @Ignore
    @NonNull
    public String name = "";
    @Ignore
    @NonNull
    public String chapter = "";
    @Ignore
    @NonNull
    public String url = "";
    @Ignore
    @NonNull
    public String anime = "";
    @Ignore
    @NonNull
    public String img = "";
    @Ignore
    public boolean isNew;
    @Ignore
    public boolean isDownloading;
    @Ignore
    public boolean isFav;
    @Ignore
    public boolean isSeen;
    @Ignore
    private FileWrapper fileWrapper;
    @Ignore
    public int downloadState;
    @Ignore
    public SearchObject animeObject;
    @Embedded
    public WebInfo webInfo;

    @Ignore
    public RecentObject() {

    }

    public RecentObject(int key, WebInfo webInfo) {
        this.key = key;
        populate(webInfo);
    }

    private RecentObject(AnimeDAO dao, WebInfo webInfo) {
        this.webInfo = webInfo;
        populate(dao, webInfo);
    }

    public static List<RecentObject> create(List<WebInfo> infos) {
        AnimeDAO dao = CacheDBWrap.INSTANCE.animeDAO();
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
        if (PrefsUtil.INSTANCE.getSaveWithName())
            return eid + "$" + PatternUtil.INSTANCE.getFileName(url);
        else
            return eid + "$" + aid + "-" + chapter.substring(chapter.lastIndexOf(" ") + 1) + ".mp4";
    }

    public String getFilePath() {
        if (PrefsUtil.INSTANCE.getSaveWithName())
            return "$" + PatternUtil.INSTANCE.getFileName(url);
        else
            return "$" + aid + "-" + chapter.substring(chapter.lastIndexOf(" ") + 1) + ".mp4";
    }

    public String getEpTitle() {
        return name + chapter.substring(chapter.lastIndexOf(" "));
    }

    public FileWrapper fileWrapper() {
        if (fileWrapper == null)
            fileWrapper = FileWrapper.Companion.create(getFilePath());
        return fileWrapper;
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
        this.key = (webInfo.aid + webInfo.chapter).hashCode();
        this.aid = webInfo.aid;
        this.chapter = webInfo.chapter.trim();
        this.eid = String.valueOf(abs((aid + chapter).hashCode()));
        this.name = PatternUtil.INSTANCE.fromHtml(webInfo.name);
        this.url = "https://animeflv.net" + webInfo.url;
        this.img = "https://animeflv.net" + webInfo.img.replace("thumbs", "covers");
        this.isNew = chapter.matches("^.* [10]$");
        this.anime = PatternUtil.INSTANCE.getAnimeUrl(this.url, this.aid);
        //File file = FileAccessHelper.INSTANCE.findFile(getFilePath());
        DownloadObject downloadObject = CacheDBWrap.INSTANCE.downloadsDAO().getByEid(eid);
        this.isDownloading = downloadObject != null && downloadObject.state == DownloadObject.DOWNLOADING;
        if (downloadObject != null) {
            this.downloadState = downloadObject.state;
        } else {
            this.downloadState = -8;
        }
        this.animeObject = CacheDBWrap.INSTANCE.animeDAO().getSOByAid(aid);
        this.isFav = CacheDBWrap.INSTANCE.favsDAO().isFav(Integer.parseInt(aid));
        this.isSeen = CacheDBWrap.INSTANCE.seenDAO().chapterIsSeen(aid, chapter);
    }

    private void populate(AnimeDAO dao, WebInfo webInfo) {
        if (isNotNumeric(webInfo.aid))
            throw new IllegalStateException("Aid must be number");
        populate(webInfo);
        this.animeObject = dao.getSOByAid(aid);
        this.isFav = CacheDBWrap.INSTANCE.favsDAO().isFav(Integer.parseInt(aid));
        this.isSeen = CacheDBWrap.INSTANCE.seenDAO().chapterIsSeen(aid, chapter);
    }

    private boolean isNotNumeric(String number) {
        try {
            Integer.parseInt(number);
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public static class WebInfo {
        @Selector(value = "img[src]", attr = "src", format = "/(\\d+)\\.\\w+")
        public String aid;
        @Selector(value = "a", attr = "href", format = "/(.*)$")
        public String eid;
        @Selector(value = "img", attr = "alt")
        public String name;
        @Selector(".Capi")
        public String chapter;
        @Selector(value = "a", attr = "href")
        public String url;
        @Selector(value = "img[src]", attr = "src")
        public String img;
    }
}
