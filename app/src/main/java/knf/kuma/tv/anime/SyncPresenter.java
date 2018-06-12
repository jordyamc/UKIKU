package knf.kuma.tv.anime;

import android.support.v17.leanback.widget.Presenter;
import android.view.ViewGroup;

import knf.kuma.tv.cards.SyncCardView;
import knf.kuma.tv.sync.SyncObject;

public class SyncPresenter extends Presenter {

    public SyncPresenter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new SyncCardView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((SyncCardView) viewHolder.view).bind((SyncObject) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
