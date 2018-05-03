package knf.kuma.widgets.emision;

public class WEListItem {
    public int key;
    public String link;
    public String title;
    public String aid;
    public String img;

    WEListItem(int key, String link, String title, String aid, String img) {
        this.key = key;
        this.link = link;
        this.title = title;
        this.aid = aid;
        this.img = img;
    }
}
