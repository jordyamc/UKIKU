package knf.kuma.pojos;

import static java.lang.Math.abs;

import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.IconCompat;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import knf.kuma.animeinfo.AnimeInfo;
import knf.kuma.animeinfo.ktx.ExtensionsKt;
import knf.kuma.commons.ExtensionUtilsKt;
import knf.kuma.commons.FileWrapper;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.database.CacheDB;
import pl.droidsonroids.jspoon.ElementConverter;
import pl.droidsonroids.jspoon.annotation.Selector;

@Entity(indices = @Index(value = {"name", "link", "aid", "type", "state", "fileName"}, unique = true))
@TypeConverters(AnimeObject.Converter.class)
public class AnimeObject implements Comparable<AnimeObject>, Serializable {
    @PrimaryKey
    @SerializedName("key")
    public int key;
    @SerializedName("link")
    public String link;
    @SerializedName("sid")
    public String sid;
    @SerializedName("name")
    public String name;
    @SerializedName("fileName")
    public String fileName;
    @Embedded
    @SerializedName("webInfo")
    public WebInfo webInfo;
    @Ignore
    @NonNull
    public transient String aid = "";
    @Ignore
    public transient String img;
    @Ignore
    public transient String description;
    @SerializedName("type")
    public String type;
    @SerializedName("state")
    public String state;
    @SerializedName("day")
    public Day day;
    @Ignore
    public transient String followers;
    @Ignore
    public transient String rate_stars;
    @Ignore
    public transient String rate_count;
    @Ignore
    public transient List<String> genres;
    @Ignore
    public transient List<WebInfo.AnimeRelated> related;
    @SerializedName("chapters")
    public List<WebInfo.AnimeChapter> chapters;
    @Ignore
    public transient IconCompat icon;

    @Ignore
    public AnimeObject() {

    }

    public AnimeObject(int key, String link, String sid, String name, String fileName, WebInfo webInfo, String type, String state, Day day) {
        this.key = key;
        this.link = link;
        this.sid = sid;
        this.name = name;
        this.fileName = fileName;
        this.webInfo = webInfo;
        this.type = type;
        this.state = state;
        this.day = day;
        populate(webInfo);
    }

    @Ignore
    public AnimeObject(int key, String link, String sid, String name, String fileName, String type, String state, Day day, List<WebInfo.AnimeChapter> chapters) {
        this.key = key;
        this.link = link;
        this.sid = sid;
        this.name = name;
        this.fileName = fileName;
        this.type = type;
        this.state = state;
        this.day = day;
        this.chapters = chapters;
    }

    @Ignore
    public AnimeObject(String link, WebInfo webInfo) {
        this.link = link;
        this.fileName = PatternUtil.INSTANCE.getRootFileName(link);
        this.sid = extract(link);
        this.webInfo = webInfo;
        populate(webInfo);
    }

    private void populate(WebInfo webInfo) {
        this.key = Integer.parseInt(webInfo.aid);
        this.webInfo = webInfo;
        this.aid = webInfo.aid;
        this.name = PatternUtil.INSTANCE.fromHtml(webInfo.name);
        this.img = "https://animeflv.net" + webInfo.img;
        this.description = PatternUtil.INSTANCE.fromHtml(webInfo.description);
        this.type = getType(webInfo.type);
        this.state = getState(webInfo.state);
        this.day = webInfo.emisionDay;
        this.followers = webInfo.followers;
        this.rate_stars = webInfo.rate_stars;
        this.rate_count = webInfo.rate_count;
        this.genres = webInfo.genres;
        this.related = webInfo.related;
        completeInfo(webInfo.scripts);
    }

    private void completeInfo(List<Element> scripts) {
        try {
            Element element = findDataScript(scripts);
            if (element != null) {
                AnimeInfo animeInfo = new AnimeInfo(element.html());
                //this.name = PatternUtil.fromHtml(animeInfo.title);
                this.day = animeInfo.getDay();
                this.chapters = WebInfo.AnimeChapter.create(animeInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Element findDataScript(List<Element> scripts) {
        try {
            for (Element element : scripts)
                if (element.html().contains("var anime_info"))
                    return element;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extract(String link) {
        return link.substring(link.lastIndexOf("/") + 1);
    }

    private String getType(String className) {
        switch (className) {
            default:
            case "Type tv":
                return "Anime";
            case "Type ova":
                return "OVA";
            case "Type special":
                return "Especial";
            case "Type movie":
                return "Película";
        }
    }

    private String getState(String className) {
        switch (className) {
            case "AnmStts":
                return "En emisión";
            case "AnmStts A":
                return "Finalizado";
            default:
                return "Próximamente";
        }
    }

    public String getFileName() {
        if (PrefsUtil.INSTANCE.getSaveWithName())
            return fileName;
        else
            return aid;
    }

    public String getGenresString() {
        if (genres.size() == 0)
            return "Sin generos";
        StringBuilder builder = new StringBuilder();
        for (String genre : genres) {
            builder.append(genre)
                    .append(", ");
        }
        String g = builder.toString();
        return g.substring(0, g.lastIndexOf(","));
    }

    public Boolean checkIntegrity() {
        try {
            return chapters != null && (chapters.size() == 0 || (chapters.get(0).aid != null && chapters.get(0).eid != null));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return aid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AnimeObject && aid.equals(((AnimeObject) obj).aid) && day.equals(((AnimeObject) obj).day) && state.equals(((AnimeObject) obj).state);
    }

    @Override
    public int compareTo(@NonNull AnimeObject o) {
        return name.compareTo(o.name);
    }

    public enum Day {
        MONDAY(2),
        TUESDAY(3),
        WEDNESDAY(4),
        THURSDAY(5),
        FRIDAY(6),
        SATURDAY(7),
        SUNDAY(1),
        NONE(0);
        public final int value;

        Day(int value) {
            this.value = value;
        }

        public static Day fromValue(int value) {
            for (Day day : values()) {
                if (day.value == value) {
                    return day;
                }
            }
            return NONE;
        }
    }

    public static class WebInfo {
        @Selector(value = "div.Image img[src]", attr = "src", format = "/(\\d+)[/.]")
        @SerializedName("aid")
        public String aid;
        @Selector(value = "meta[property='og:title']", attr = "content", format = "^ ?V?e?r? ?A?n?i?m?e? ?(.+ ?O?n?l?i?n?e?) Online", defValue = "Error")
        @ColumnInfo(name = "web_name")
        @SerializedName("web_name")
        public String name;
        @Selector(value = "div.Image img[src]", attr = "src")
        @SerializedName("img")
        public String img;
        @Selector(value = "div.Description", defValue = "Sin descripcion")
        @SerializedName("description")
        public String description;
        @Selector(value = "span[class^=Type]", attr = "class", defValue = "Desconocido")
        @ColumnInfo(name = "web_type")
        @SerializedName("web_type")
        public String type;
        @Selector(value = "aside.SidebarA.BFixed p", attr = "class", defValue = "Desconocido")
        @ColumnInfo(name = "web_state")
        @SerializedName("web_state")
        public String state;
        @Selector(value = "div.Title:contains(Seguidores) span", defValue = "0")
        @SerializedName("followers")
        public String followers = "0";
        @Selector(value = "span.vtprmd", defValue = "0.0")
        @SerializedName("rate_starts")
        public String rate_stars;
        @Selector(value = "span#votes_nmbr", defValue = "0")
        @SerializedName("rate_count")
        public String rate_count;
        @Selector(value = "span.Date.fa-calendar", converter = DayConverter.class)
        @SerializedName("emissionDay")
        public Day emisionDay;
        @Selector("nav.Nvgnrs a[href]")
        @SerializedName("genres")
        public List<String> genres = new ArrayList<>();
        @Selector("ul.ListAnmRel li:has(a[href~=^\\/[a-z]+\\/.+$])")
        @SerializedName("related")
        public List<AnimeRelated> related = new ArrayList<>();
        @Ignore
        @Selector("script:not([src])")
        transient List<Element> scripts;
        /*@Selector("ul.ListCaps li,ul.ListEpisodes li,ul#episodeList li")
        public List<Element> chapters = new ArrayList<>();*/

        @Keep
        public static class AnimeRelated {
            @Selector(value = "a", attr = "href")
            @SerializedName("link")
            public String link;
            @Selector(value = "a", converter = AidGetter.class)
            @SerializedName("aid")
            public String aid;
            @Selector("a")
            @SerializedName("name")
            public String name;
            @Selector(value = "li", format = "\\((.*)\\)")
            @SerializedName("relation")
            public String relation;

            public static class AidGetter implements ElementConverter<String> {
                @Keep
                public AidGetter() {

                }

                @Override
                public String convert(@NotNull Element node, @NotNull Selector selector) {
                    String aid = CacheDB.Companion.getINSTANCE().animeDAO().findAidByName(node.text());
                    if (aid != null)
                        return aid;
                    else
                        return "null";
                }
            }
        }

        @Entity
        @Keep
        public static class AnimeChapter implements Comparable<AnimeChapter>, Serializable {
            @SerializedName("chapter_key")
            @PrimaryKey
            public int key;
            @SerializedName("chapter_number")
            public String number;
            @SerializedName("chapter_eid")
            public String eid;
            @SerializedName("chapter_link")
            public String link;
            @SerializedName("chapter_name")
            public String name;
            @SerializedName("chapter_aid")
            public String aid;
            @SerializedName("chapter_img")
            @Ignore
            public String img;
            @SerializedName("chapter_Type")
            @Ignore
            public transient ChapterType chapterType;
            @SerializedName("chapter_ isDownloaded")
            @Ignore
            private transient FileWrapper fileWrapper;

            public AnimeChapter() {
            }

            public AnimeChapter(int key, String number, String eid, String link, String name, String aid) {
                this.key = key;
                this.number = number;
                this.eid = eid;
                this.link = link;
                this.name = name;
                this.aid = aid;
                this.fileWrapper = FileWrapper.Companion.create(ExtensionsKt.getFilePath(this));
            }

            @Ignore
            public AnimeChapter(String name, String aid, Element element) {
                this.name = name;
                if (element.select("img").first() == null) {
                    this.chapterType = ChapterType.OLD;
                    String full = element.select("a").first().text();
                    this.number = "Episodio " + extract(full, "^.* (\\d+\\.?\\d*):?.*$");
                    this.link = "https://animeflv.net" + element.select("a").first().attr("href");
                    this.eid = String.valueOf(abs((aid + number).hashCode()));
                } else {
                    this.chapterType = ChapterType.NEW;
                    this.number = element.select("p").first().ownText();
                    this.link = "https://animeflv.net" + element.select("a").first().attr("href");
                    this.eid = String.valueOf(abs((aid + number).hashCode()));
                    this.img = element.select("img.lazy").first().attr("src");
                }
                this.aid = aid;
                this.key = (aid + number).hashCode();
                this.fileWrapper = FileWrapper.Companion.create(ExtensionsKt.getFilePath(this));
            }

            @Ignore
            public AnimeChapter(AnimeInfo info, String num, String sid) {
                this.name = info.getTitle();
                this.chapterType = ChapterType.NEW;
                this.aid = info.getAid();
                this.number = "Episodio " + num;
                this.link = "https://animeflv.net/ver/" + info.getSid() + "-" + num;
                this.eid = String.valueOf(abs((aid + number).hashCode()));
                this.img = "https://cdn.animeflv.net/screenshots/" + info.getAid() + "/" + num + "/th_3.jpg";
                this.key = (aid + number).hashCode();
                this.fileWrapper = FileWrapper.Companion.create(ExtensionsKt.getFilePath(this));
            }

            @NonNull
            public FileWrapper fileWrapper() {
                if (fileWrapper == null)
                    this.fileWrapper = FileWrapper.Companion.create(ExtensionsKt.getFilePath(this));
                return fileWrapper;
            }

            @Ignore
            public static AnimeChapter fromData(String aid, String chapter, String eid, String url, String name) {
                return new AnimeChapter((aid + chapter).hashCode(), chapter, eid, url, name, aid);
            }

            @Ignore
            public static AnimeChapter fromRecent(RecentObject object) {
                return new AnimeChapter((object.aid + object.chapter).hashCode(), object.chapter, object.eid, object.url, object.name, object.aid);
            }

            @Ignore
            public static AnimeChapter fromDownloaded(ExplorerObject.FileDownObj object) {
                return new AnimeChapter((object.aid + "Episodio " + object.chapter).hashCode(), "Episodio " + object.chapter, object.eid, object.link, object.title, object.aid);
            }

            public static List<AnimeChapter> create(String name, String aid, List<Element> elements) {
                List<AnimeChapter> chapters = new ArrayList<>();
                for (Element element : elements) {
                    try {
                        chapters.add(new AnimeChapter(name, aid, element));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return chapters;
            }

            public static List<AnimeChapter> create(AnimeInfo info) {
                List<AnimeChapter> chapters = new ArrayList<>();
                try {
                    for (Map.Entry<String, String> entry : info.getEpMap().entrySet()) {
                        try {
                            chapters.add(new AnimeChapter(info, entry.getKey(), entry.getValue()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Collections.sort(chapters);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return chapters;
            }

            public String commentariesLink(String version) {
                try {
                    return "https://disqus.com/embed/comments/?base=default&f=https-animeflv-net&t_u=" + URLEncoder.encode(ExtensionUtilsKt.resolveRedirection(link, 0), "UTF-8") + "&s_o=default#version=" + version;
                } catch (Exception e) {
                    try {
                        return "https://disqus.com/embed/comments/?base=default&f=https-animeflv-net&t_u=" + URLEncoder.encode(link, "UTF-8") + "&s_o=default#version=" + version;
                    } catch (Exception ex) {
                        return link;
                    }
                }

            }

            @Override
            public int compareTo(@NonNull AnimeChapter animeChapter) {
                double num1 = Double.valueOf(number.substring(number.lastIndexOf(" ") + 1));
                double num2 = Double.valueOf(animeChapter.number.substring(animeChapter.number.lastIndexOf(" ") + 1));
                return Double.compare(num2, num1);
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof AnimeChapter && eid.equals(((AnimeChapter) obj).eid);
            }

            @Override
            public int hashCode() {
                return name.hashCode() + number.hashCode();
            }

            private String extract(String st, String regex) {
                Matcher matcher = Pattern.compile(regex).matcher(st);
                matcher.find();
                return matcher.group(1);
            }

            public enum ChapterType {
                @SerializedName("NEW")
                NEW(0),
                @SerializedName("OLD")
                OLD(1);
                public int value;

                ChapterType(int value) {
                    this.value = value;
                }
            }
        }
    }

    public static class Converter {

        @TypeConverter
        public List<String> stringToList(String json) {
            return Arrays.asList(json.split(";"));
        }

        @TypeConverter
        public String listToString(List<String> list) {
            return TextUtils.join(";", list);
        }

        @TypeConverter
        public List<WebInfo.AnimeRelated> stringToRelated(String json) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<List<WebInfo.AnimeRelated>>() {
                }.getType();
                return gson.fromJson(json, type);
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }

        @TypeConverter
        public String relatedToString(List<WebInfo.AnimeRelated> list) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<WebInfo.AnimeRelated>>() {
            }.getType();
            return gson.toJson(list, type);
        }

        @TypeConverter
        public List<WebInfo.AnimeChapter> stringToChapters(String json) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<WebInfo.AnimeChapter>>() {
            }.getType();
            return gson.fromJson(json, type);
        }

        @TypeConverter
        public String chaptersToString(List<WebInfo.AnimeChapter> list) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<List<WebInfo.AnimeChapter>>() {
                }.getType();
                return gson.toJson(list, type);
            } catch (Exception e) {
                return "{}";
            }
        }

        @TypeConverter
        public Day intToDay(int day) {
            return Day.fromValue(day);
        }

        @TypeConverter
        public int dayToInt(Day day) {
            return day.value;
        }
    }

    public static class DayConverter implements ElementConverter<Day> {
        @Keep
        public DayConverter() {
        }

        @Override
        public Day convert(@NotNull Element node, @NotNull Selector selector) {
            try {
                Element element = node.select(selector.value()).first();
                if (element == null)
                    return Day.NONE;
                String date = element.ownText().trim();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date));
                switch (calendar.get(Calendar.DAY_OF_WEEK)) {
                    case 2:
                        return Day.MONDAY;
                    case 3:
                        return Day.TUESDAY;
                    case 4:
                        return Day.WEDNESDAY;
                    case 5:
                        return Day.THURSDAY;
                    case 6:
                        return Day.FRIDAY;
                    case 7:
                        return Day.SATURDAY;
                    case 1:
                        return Day.SUNDAY;
                    default:
                        return Day.NONE;
                }
            } catch (Exception e) {
                //e.printStackTrace();
                return Day.NONE;
            }
        }
    }

}