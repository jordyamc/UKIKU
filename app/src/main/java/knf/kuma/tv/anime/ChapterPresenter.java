package knf.kuma.tv.anime;

import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.tv.cards.ChapterCardView;

public class ChapterPresenter extends Presenter {

    public ChapterPresenter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(new ChapterCardView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((ChapterCardView) viewHolder.view).bind((AnimeObject.WebInfo.AnimeChapter) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
