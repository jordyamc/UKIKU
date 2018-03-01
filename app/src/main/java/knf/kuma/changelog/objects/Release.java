package knf.kuma.changelog.objects;

import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class Release {
    public String version;
    public String code;
    public List<Change> changes;

    public Release(Element element) {
        this.version = element.attr("version");
        this.code = element.attr("code");
        List<Change> list = new ArrayList<>();
        for (Element e : element.select("change")) {
            list.add(new Change(e));
        }
        this.changes = list;
    }
}
