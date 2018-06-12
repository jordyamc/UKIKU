package knf.kuma.tv.anime;

import android.support.v17.leanback.widget.Presenter;
import android.view.ViewGroup;

import knf.kuma.pojos.AnimeObject;
import knf.kuma.tv.cards.RelatedCardView;

public class RelatedPresenter extends Presenter {

    public RelatedPresenter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new RelatedCardView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((RelatedCardView) viewHolder.view).bind((AnimeObject.WebInfo.AnimeRelated) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
