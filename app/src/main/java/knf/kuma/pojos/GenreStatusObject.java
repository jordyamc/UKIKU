package knf.kuma.pojos;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jordy on 26/03/2018.
 */

@Entity
public class GenreStatusObject implements Comparable<GenreStatusObject> {
    public String name;
    public int count;
    @PrimaryKey
    public int key;

    public GenreStatusObject(int key, String name, int count) {
        this.key = key;
        this.name = name;
        this.count = count;
    }

    @Ignore
    public GenreStatusObject(String name) {
        this.key = Math.abs(name.hashCode());
        this.name = name;
        this.count = 0;
    }

    public static List<String> getNames(List<GenreStatusObject> list) {
        List<String> names = new ArrayList<>();
        for (GenreStatusObject object : list)
            names.add(object.getName());
        return names;
    }

    public String getName() {
        return name;
    }

    public boolean isBlocked() {
        return count < 0;
    }

    public void add(int number) {
        count += number;
    }

    public void sub(int number) {
        count -= number;
        if (count < 0) count = 0;
    }

    public void block() {
        count = -1;
    }

    public void reset() {
        count = 0;
    }

    @Override
    public int compareTo(@NonNull GenreStatusObject o) {
        return name.compareTo(o.getName());
    }
}
