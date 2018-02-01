package knf.kuma.pojos;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import knf.kuma.database.CacheDB;

/**
 * Created by Jordy on 23/01/2018.
 */
@Entity
public class SeeingObject {
    @PrimaryKey
    public int key;
    public String img;
    public String link;
    public String aid;
    public String title;
    public String chapter;
    @Ignore
    public AnimeObject.WebInfo.AnimeChapter lastChapter;

    public SeeingObject(int key, String img, String link, String aid, String title, String chapter) {
        this.key = key;
        this.img = img;
        this.link = link;
        this.aid = aid;
        this.title = title;
        this.chapter = chapter;
        this.lastChapter= CacheDB.INSTANCE.chaptersDAO().getLastByAid(aid);
    }

    @Ignore
    private SeeingObject() {
    }

    @Ignore
    public static SeeingObject fromAnime(FavoriteObject favoriteObject){
        SeeingObject item=new SeeingObject();
        item.key=Integer.parseInt(favoriteObject.aid);
        item.img=favoriteObject.img;
        item.link=favoriteObject.link;
        item.aid=favoriteObject.aid;
        item.title=favoriteObject.name;
        item.chapter="No empezado";
        return item;
    }
}
