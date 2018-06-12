package knf.kuma.tv.anime;

import android.support.v17.leanback.widget.Presenter;
import android.view.ViewGroup;

import knf.kuma.pojos.FavoriteObject;
import knf.kuma.tv.cards.FavCardView;

public class FavPresenter extends Presenter {

    public FavPresenter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new FavCardView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((FavCardView) viewHolder.view).bind((FavoriteObject) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
