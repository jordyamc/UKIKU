package knf.kuma.tv.anime;

import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.tv.cards.AnimeCardView;

public class AnimePresenter extends Presenter {

    public AnimePresenter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new AnimeCardView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((AnimeCardView) viewHolder.view).bind((AnimeObject) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
