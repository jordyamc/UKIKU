package knf.kuma.videoservers;

import java.util.ArrayList;
import java.util.List;

public class Option {
    public String name;
    public String url;

    public Option(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public static List<String> getNames(List<Option> options) {
        List<String> names = new ArrayList<>();
        for (Option option : options)
            names.add(option.name);
        return names;
    }

    public static List<String> getLinks(List<Option> options) {
        List<String> links = new ArrayList<>();
        for (Option option : options)
            links.add(option.name);
        return links;
    }
}
