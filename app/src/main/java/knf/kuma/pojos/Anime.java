package knf.kuma.pojos;

import pl.droidsonroids.jspoon.annotation.Selector;

public class Anime {
    @Selector("html")
    public AnimeObject.WebInfo object;
}
