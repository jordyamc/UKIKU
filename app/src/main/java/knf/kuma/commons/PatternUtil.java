package knf.kuma.commons;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import knf.kuma.pojos.AnimeObject;

public class PatternUtil {
    public static String getLinkNumber(String link){
        Pattern pattern=Pattern.compile("/(\\d+)[/.]");
        Matcher matcher=pattern.matcher(link);
        matcher.find();
        return matcher.group(1);
    }

    public static String getRapidLink(String link) {
        Pattern pattern = Pattern.compile("\"(.*rapidvideo.*)\"");
        Matcher matcher = pattern.matcher(link);
        matcher.find();
        return matcher.group(1);
    }

    public static String getRapidVideoLink(String link) {
        Pattern pattern = Pattern.compile("\"(http.*\\.mp4)\"");
        Matcher matcher = pattern.matcher(link);
        matcher.find();
        return matcher.group(1);
    }

    public static String getLinkId(String link){
        Matcher matcher=Pattern.compile("^.*/(.*)-\\d+$").matcher(link);
        matcher.find();
        return matcher.group(1);
    }

    public static String getLinkNum(String link){
        Matcher matcher=Pattern.compile("^.*-(\\d+)$").matcher(link);
        matcher.find();
        return matcher.group(1);
    }

    public static String getFileName(String link){
        try {
            Matcher matcher=Pattern.compile("^.*/(.*-\\d+\\.?\\d*)$").matcher(link);
            matcher.find();
            return matcher.group(1) + ".mp4";
        }catch (Exception e){
            Log.e("Pattern","No name found in: "+link,e);
            return "N-F.mp4";
        }
    }

    public static String getRootFileName(String link){
        try {
            Matcher matcher=Pattern.compile("^.*/([a-z\\-\\d]+).*$").matcher(link);
            matcher.find();
            return matcher.group(1);
        }catch (Exception e){
            Log.e("Pattern","No name found in: "+link,e);
            return "N-F";
        }
    }

    public static String getNameFromFile(String file){
        Matcher matcher=Pattern.compile("^.*\\$(.*)-\\d+\\.?\\d*\\.mp4$").matcher(file);
        matcher.find();
        return matcher.group(1)+"/";
    }

    public static String getNumFromfile(String file){
        Matcher matcher=Pattern.compile("^.*\\$[a-z-0-9]*-(\\d+)\\.mp4$").matcher(file);
        matcher.find();
        return matcher.group(1);
    }

    public static String getEidFromfile(String file){
        Matcher matcher=Pattern.compile("^(\\d+)\\$.*$").matcher(file);
        matcher.find();
        return matcher.group(1);
    }

    public static String extractLink(String html){
        Matcher matcher=Pattern.compile("https?://[a-zA-Z0-a.=?/&]+").matcher(html);
        matcher.find();
        return matcher.group(0);
    }

    public static String extractMediaLink(String html) {
        Matcher matcher = Pattern.compile("www\\.mediafire[a-zA-Z0-a.=?/&%]+").matcher(html);
        matcher.find();
        return "https://" + matcher.group().replace("%2F", "/");
    }

    public static String extractOkruLink(String html) {
        Matcher matcher = Pattern.compile("\"(https://ok\\.ru.*)\"").matcher(html);
        matcher.find();
        return matcher.group(1);
    }

    public static String getAnimeUrl(String chapter,String aid){
        return "https://animeflv.net/anime/"+aid+chapter.substring(chapter.lastIndexOf("/"),chapter.lastIndexOf("-"));
    }

    public static String getCover(String aid) {
        return "https://animeflv.net/uploads/animes/covers/" + aid + ".jpg";
    }

    public static String getBanner(String aid) {
        return "https://animeflv.net/uploads/animes/banners/" + aid + ".jpg";
    }

    public static HashMap<String, String> getEpListMap(String code) {
        HashMap<String, String> map = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("\\[(\\d+),(\\d+)\\]").matcher(code);
        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }

    public static boolean isCustomSearch(String s){
        return s.matches("^:[a-z]+:.*$");
    }

    public static String getCustomSearch(String s){
        Matcher matcher=Pattern.compile("^:[a-z]+:(.*$)").matcher(s);
        matcher.find();
        return matcher.group(1);
    }

    public static String getCustomAttr(String s){
        Matcher matcher=Pattern.compile("^:([a-z]+):.*$").matcher(s);
        matcher.find();
        return matcher.group(1);
    }

    public static List<String> getEids(List<AnimeObject.WebInfo.AnimeChapter> chapters){
        List<String> eids=new ArrayList<>();
        for (AnimeObject.WebInfo.AnimeChapter chapter:chapters){
            eids.add(chapter.eid);
        }
        return eids;
    }
}
