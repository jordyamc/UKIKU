package knf.kuma.favorite.objects;

import java.util.ArrayList;
import java.util.List;

import knf.kuma.pojos.FavSection;
import knf.kuma.pojos.FavoriteObject;

public class InfoContainer {
    public List<FavoriteObject> updated;
    public boolean needReload = false;
    public int from;
    public int to;
    private List<FavoriteObject> current;

    public InfoContainer() {

    }

    public void setLists(List<FavoriteObject> current, List<FavoriteObject> updated) {
        this.current = new ArrayList<>(current);
        this.updated = new ArrayList<>(updated);
    }

    public void reload(FavoriteObject object) {
        if (object instanceof FavSection || object == null) {
            needReload = true;
        } else if (!updated.contains(object)) {
            needReload = true;
        } else if (current.size() != updated.size()) {
            needReload = true;
        } else {
            needReload = false;
            from = current.indexOf(object);
            to = updated.indexOf(object);
        }
    }
}
