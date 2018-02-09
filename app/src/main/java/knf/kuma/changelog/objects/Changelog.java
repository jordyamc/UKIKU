package knf.kuma.changelog.objects;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jordy on 07/02/2018.
 */
public class Changelog {
    public List<Release> releases;

    public Changelog(Document document) {
        List<Release> list = new ArrayList<>();
        for (Element element : document.select("release")) {
            list.add(new Release(element));
        }
        Collections.reverse(list);
        this.releases = list;
    }
}
