package knf.kuma.changelog.objects;

import org.jsoup.nodes.Element;

public class Change {
    public String type;
    public String text;

    public Change(Element element) {
        this.type = element.attr("type");
        this.text = element.text();
    }
}
