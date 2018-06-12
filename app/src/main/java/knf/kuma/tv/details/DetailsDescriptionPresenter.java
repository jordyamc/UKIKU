package knf.kuma.tv.details;

import android.support.annotation.ColorInt;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import knf.kuma.pojos.AnimeObject;

public class DetailsDescriptionPresenter
        extends AbstractDetailsDescriptionPresenter {
    @ColorInt
    private int titleColor;
    @ColorInt
    private int bodyColor;

    DetailsDescriptionPresenter(int titleColor, int bodyColor) {
        this.titleColor = titleColor;
        this.bodyColor = bodyColor;
    }

    @Override
    protected void onBindDescription(AbstractDetailsDescriptionPresenter.ViewHolder viewHolder, Object itemData) {
        AnimeObject animeObject = (AnimeObject) itemData;
        viewHolder.getTitle().setText(animeObject.name);
        viewHolder.getSubtitle().setText(animeObject.getGenresString());
        viewHolder.getBody().setText(animeObject.description);
        viewHolder.getTitle().setTextColor(titleColor);
        viewHolder.getSubtitle().setTextColor(bodyColor);
        viewHolder.getBody().setTextColor(bodyColor);
    }
}
