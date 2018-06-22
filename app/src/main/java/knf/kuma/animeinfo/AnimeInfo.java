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
        //Matcher matcher = Pattern.compile("\"(.*)\",\"(.*)\",\"(.*)\",\"(.*)\"").matcher(code);
        Matcher matcher = Pattern.compile("\"([^\",/<>]*)\"").matcher(code);
        int i = 0;
        while (matcher.find()) {
            switch (i) {
                case 0:
                    this.aid = matcher.group(1);
                    break;
                case 1:
                    this.title = matcher.group(1);
                    break;
                case 2:
                    this.sid = matcher.group(1);
                    break;
            }
            i++;
        }
        this.epMap = PatternUtil.getEpListMap(code);
    }
}
