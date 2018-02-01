package knf.kuma.pojos;

import pl.droidsonroids.jspoon.annotation.Selector;

/**
 * Created by Jordy on 04/01/2018.
 */

public class Anime {
    @Selector("html")
    public AnimeObject.WebInfo object;
}
