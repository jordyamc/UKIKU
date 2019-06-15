package knf.kuma.pojos;

import java.util.List;

import pl.droidsonroids.jspoon.annotation.Selector;

public class Recents {
    @Selector("ul.ListEpisodios li:not(article), ul.List-Episodes li:not(article)")
    public List<RecentObject.WebInfo> list;

}
