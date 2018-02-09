package knf.kuma.changelog.objects;

import org.jsoup.nodes.Element;

/**
 * Created by Jordy on 07/02/2018.
 */
public class Change {
    public String type;
    public String text;

    public Change(Element element) {
        this.type = element.attr("type");
        this.text = element.text();
    }
}
