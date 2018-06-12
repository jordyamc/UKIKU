package knf.kuma.tv.anime;

import android.support.v17.leanback.widget.Presenter;
import android.view.ViewGroup;

import knf.kuma.pojos.RecordObject;
import knf.kuma.tv.cards.RecordCardView;

public class RecordPresenter extends Presenter {

    public RecordPresenter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new RecordCardView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((RecordCardView) viewHolder.view).bind((RecordObject) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
