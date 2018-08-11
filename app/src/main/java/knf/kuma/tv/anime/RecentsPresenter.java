package knf.kuma.tv.anime;

import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.RecentObject;
import knf.kuma.tv.cards.RecentsCardView;
import knf.kuma.tv.details.TVAnimesDetails;

public class RecentsPresenter extends Presenter {

    public RecentsPresenter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new RecentsCardView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((RecentsCardView) viewHolder.view).bind((RecentObject) item);
        viewHolder.view.setOnLongClickListener(v -> {
            AnimeObject object = CacheDB.INSTANCE.animeDAO().getByAid(((RecentObject) item).aid);
            if (object != null)
                TVAnimesDetails.start(v.getContext(), object.link);
            return true;
        });
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
