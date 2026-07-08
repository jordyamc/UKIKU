package knf.kuma.pojos;

import androidx.annotation.Keep;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import knf.kuma.database.CacheDBWrap;
import knf.kuma.search.SearchObject;

@Keep
@Entity
public class RecordObject {
    @SerializedName("key")
    @PrimaryKey
    public int key;
    @SerializedName("name")
    public String name;
    @SerializedName("chapter")
    public String chapter;
    @SerializedName("aid")
    public String aid;
    @SerializedName("eid")
    public String eid;
    @SerializedName("date")
    public long date;
    @SerializedName("animeObject")
    @Ignore
    public transient SearchObject animeObject;

    public RecordObject(int key, String name, String chapter, String aid, String eid, long date) {
        this.key = key;
        this.name = name;
        this.chapter = chapter;
        this.aid = aid;
        this.eid = eid;
        this.date = date;
        this.animeObject = CacheDBWrap.INSTANCE.animeDAO().getByAidSimple(aid);
    }

}
