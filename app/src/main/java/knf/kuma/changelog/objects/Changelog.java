package knf.kuma.changelog.objects;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class Changelog {
    public List<Release> releases;

    public Changelog(Document document) {
        List<Release> list = new ArrayList<>();
        for (Element element : document.select("release")) {
            list.add(new Release(element));
        }
        this.releases = list;
    }
}
