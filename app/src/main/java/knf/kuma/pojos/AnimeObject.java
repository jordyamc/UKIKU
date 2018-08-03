package knf.kuma.pojos;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.nodes.Element;

import java.lang.reflect.Type;
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
import knf.kuma.commons.PatternUtil;
import pl.droidsonroids.jspoon.ElementConverter;
import pl.droidsonroids.jspoon.annotation.Selector;

@Entity(indices = @Index(value = {"name", "link", "aid", "type", "state", "fileName"}, unique = true))
@TypeConverters(AnimeObject.Converter.class)
public class AnimeObject implements Comparable<AnimeObject> {
    @PrimaryKey
    public int key;
    public String link;
    public String sid;
    public String name;
    public String fileName;
    @Embedded
    public WebInfo webInfo;

    @Ignore
    public String aid;
    @Ignore
    public String img;
    @Ignore
    public String description;
    public String type;
    public String state;
    public Day day;
    @Ignore
    public String rate_stars;
    @Ignore
    public String rate_count;
    @Ignore
    public List<String> genres;
    @Ignore
    public List<WebInfo.AnimeRelated> related;
    public List<WebInfo.AnimeChapter> chapters;

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
    public AnimeObject(String link, WebInfo webInfo) {
        this.link = link;
        this.fileName = PatternUtil.getRootFileName(link);
        this.sid = extract(link);
        this.webInfo = webInfo;
        populate(webInfo);
    }

    private void populate(WebInfo webInfo) {
        this.key = Integer.parseInt(webInfo.aid);
        this.webInfo = webInfo;
        this.aid = webInfo.aid;
        this.name = PatternUtil.fromHtml(webInfo.name);
        this.img = "https://animeflv.net" + webInfo.img;
        this.description = PatternUtil.fromHtml(webInfo.description);
        this.type = getType(webInfo.type);
        this.state = getState(webInfo.state);
        this.day = webInfo.emisionDay;
        this.rate_stars = webInfo.rate_stars;
        this.rate_count = webInfo.rate_count;
        this.genres = webInfo.genres;
        this.related = webInfo.related;
        completeInfo(webInfo.scripts);
    }

    private void completeInfo(List<Element> scripts) {
        try {
            AnimeInfo animeInfo = new AnimeInfo(findDataScript(scripts).html());
            this.day = animeInfo.day;
            this.chapters = WebInfo.AnimeChapter.create(animeInfo);
        } catch (Exception e) {
            //
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
        return PatternUtil.getLinkNumber(link);
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

    @Override
    public int hashCode() {
        return aid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AnimeObject && aid.equals(((AnimeObject) obj).aid);
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
        public int value;

        Day(int value) {
            this.value = value;
        }

        public static Day fromValue(int value) {
            for (Day color : values()) {
                if (color.value == value) {
                    return color;
                }
            }
            return NONE;
        }
    }

    public static class WebInfo {
        @Selector(value = "div.Image img[src]", attr = "src", format = "/(\\d+)[/.]")
        public String aid;
        @Selector(value = "meta[property='og:title']", attr = "content", format = "^ ?(.+ ?O?n?l?i?n?e?) Online", defValue = "Error")
        @ColumnInfo(name = "web_name")
        public String name;
        @Selector(value = "div.Image img[src]", attr = "src")
        public String img;
        @Selector(value = "div.Description p", defValue = "Sin descripcion")
        public String description;
        @Selector(value = "div.Ficha div.Container span", attr = "class")
        @ColumnInfo(name = "web_type")
        public String type;
        @Selector(value = "aside.SidebarA.BFixed p", attr = "class")
        @ColumnInfo(name = "web_state")
        public String state;
        @Selector(value = "meta[itemprop='ratingValue']", attr = "content")
        public String rate_stars;
        @Selector(value = "meta[itemprop='ratingCount']", attr = "content")
        public String rate_count;
        @Selector(value = "span.Date.fa-calendar", converter = DayConverter.class)
        public Day emisionDay;
        @Selector("nav.Nvgnrs a")
        public List<String> genres = new ArrayList<>();
        @Selector("ul.ListAnmRel li:has(a[href~=^\\/[a-z]+\\/\\d+\\/.+$])")
        public List<AnimeRelated> related = new ArrayList<>();
        @Ignore
        @Selector("script[type=text/javascript]:not([src])")
        public List<Element> scripts;
        /*@Selector("ul.ListCaps li,ul.ListEpisodes li,ul#episodeList li")
        public List<Element> chapters = new ArrayList<>();*/

        public static class AnimeRelated {
            @Selector(value = "a", attr = "href")
            public String link;
            @Selector(value = "a", attr = "href", format = "/(\\d+)/")
            public String aid;
            @Selector("a")
            public String name;
            @Selector(value = "li", format = "\\((.*)\\)")
            public String relation;
        }

        @Entity
        public static class AnimeChapter implements Comparable<AnimeChapter> {
            @PrimaryKey
            public int key;
            public String number;
            public String eid;
            public String link;
            public String name;
            public String aid;
            @Ignore
            public String img;
            @Ignore
            public ChapterType chapterType;
            @Ignore
            public boolean isDownloaded = false;

            public AnimeChapter(int key, String number, String eid, String link, String name, String aid) {
                this.key = key;
                this.number = number;
                this.eid = eid;
                this.link = link;
                this.name = name;
                this.aid = aid;
            }

            @Ignore
            public AnimeChapter(String name, String aid, Element element) {
                this.name = name;
                if (element.select("img").first() == null) {
                    this.chapterType = ChapterType.OLD;
                    String full = element.select("a").first().text();
                    this.number = "Episodio " + extract(full, "^.* (\\d+\\.?\\d*):?.*$");
                    this.link = "https://animeflv.net" + element.select("a").first().attr("href");
                    this.eid = extract(link, "^.*/(\\d+)/.*$");
                } else {
                    this.chapterType = ChapterType.NEW;
                    this.number = element.select("p").first().ownText();
                    this.link = "https://animeflv.net" + element.select("a").first().attr("href");
                    this.eid = extract(link, "^.*/(\\d+)/.*$");
                    this.img = element.select("img.lazy").first().attr("src");
                }
                this.key = Integer.parseInt(eid);
                this.aid = aid;
            }

            @Ignore
            public AnimeChapter(AnimeInfo info, String num, String sid) {
                this.name = info.title;
                this.chapterType = ChapterType.NEW;
                this.number = "Episodio " + num;
                this.link = "https://animeflv.net/ver/" + sid + "/" + info.sid + "-" + num;
                this.eid = sid;
                this.img = "https://animeflv.net/uploads/animes/screenshots/" + info.aid + "/" + num + "/3.jpg";
                this.key = Integer.parseInt(eid);
                this.aid = info.aid;
            }

            @Ignore
            public static AnimeChapter fromRecent(RecentObject object) {
                return new AnimeChapter(Integer.parseInt(object.eid), object.chapter, object.eid, object.url, object.name, object.aid);
            }

            @Ignore
            public static AnimeChapter fromDownloaded(ExplorerObject.FileDownObj object) {
                return new AnimeChapter(Integer.parseInt(object.eid), object.chapter, object.eid, object.link, object.time, object.aid);
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
                    for (Map.Entry<String, String> entry : info.epMap.entrySet()) {
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

            public String getFileName() {
                return eid + "$" + PatternUtil.getFileName(link);
            }

            public String getEpTitle() {
                return name + number.substring(number.lastIndexOf(" "));
            }

            @Override
            public int compareTo(@NonNull AnimeChapter animeChapter) {
                int num1 = Integer.parseInt(number.substring(number.lastIndexOf(" ") + 1));
                int num2 = Integer.parseInt(animeChapter.number.substring(animeChapter.number.lastIndexOf(" ") + 1));
                return num2 - num1;
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
                NEW(0),
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
            Gson gson = new Gson();
            Type type = new TypeToken<List<WebInfo.AnimeRelated>>() {
            }.getType();
            return gson.fromJson(json, type);
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
            Gson gson = new Gson();
            Type type = new TypeToken<List<WebInfo.AnimeChapter>>() {
            }.getType();
            return gson.toJson(list, type);
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
        public Day convert(Element node, Selector selector) {
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
                e.printStackTrace();
                return Day.NONE;
            }
        }
    }

}