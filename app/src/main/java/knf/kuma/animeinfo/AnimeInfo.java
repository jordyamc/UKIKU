package knf.kuma.animeinfo;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import knf.kuma.commons.PatternUtil;

public class AnimeInfo {
    public String aid;
    public String title;
    public String sid;
    public HashMap<String, String> epMap;

    public AnimeInfo(String code) {
        Matcher matcher = Pattern.compile("\"(.*)\",\"(.*),\"(.*)\"").matcher(code);
        matcher.find();
        this.aid = matcher.group(1);
        this.title = matcher.group(2);
        this.sid = matcher.group(3);
        this.epMap = PatternUtil.getEpListMap(code);
    }
}
