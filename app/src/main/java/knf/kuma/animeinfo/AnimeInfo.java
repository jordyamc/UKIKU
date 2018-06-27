package knf.kuma.animeinfo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import knf.kuma.commons.PatternUtil;
import knf.kuma.pojos.AnimeObject;

public class AnimeInfo {
    public String aid;
    public String title;
    public String sid;
    public AnimeObject.Day day;
    public HashMap<String, String> epMap;
    private String date;

    public AnimeInfo(String code) {
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
                case 3:
                    this.date = matcher.group(1);
                    break;
            }
            i++;
        }
        this.day = getDay();
        this.epMap = PatternUtil.getEpListMap(code);
    }

    private AnimeObject.Day getDay() {
        try {
            if (date == null)
                return AnimeObject.Day.NONE;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date));
            switch (calendar.get(Calendar.DAY_OF_WEEK)) {
                case 2:
                    return AnimeObject.Day.MONDAY;
                case 3:
                    return AnimeObject.Day.TUESDAY;
                case 4:
                    return AnimeObject.Day.WEDNESDAY;
                case 5:
                    return AnimeObject.Day.THURSDAY;
                case 6:
                    return AnimeObject.Day.FRIDAY;
                case 7:
                    return AnimeObject.Day.SATURDAY;
                case 1:
                    return AnimeObject.Day.SUNDAY;
                default:
                    return AnimeObject.Day.NONE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return AnimeObject.Day.NONE;
        }
    }
}
