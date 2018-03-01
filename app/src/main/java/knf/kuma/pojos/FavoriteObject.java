package knf.kuma.pojos;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class FavoriteObject {
    @PrimaryKey
    public int key;
    public String aid;
    public String name;
    public String img;
    public String type;
    public String link;
    public String category;

    public FavoriteObject(int key, String aid, String name, String img, String type, String link, String category) {
        this.key = key;
        this.aid = aid;
        this.name = name;
        this.img = img;
        this.type = type;
        this.link = link;
        this.category = category;
    }

    @Ignore
    public FavoriteObject(AnimeObject object){
        this.key=object.key;
        this.aid=object.aid;
        this.name=object.name;
        this.img=object.img;
        this.type=object.type;
        this.link=object.link;
        this.category="_NONE_";
    }

    public void setCategory(String category){
        this.category=category;
    }
}
