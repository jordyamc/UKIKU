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

    DetailsDescriptionPresenter() {
        this.titleColor = 0;
        this.bodyColor = 0;
    }

    @Override
    protected void onBindDescription(AbstractDetailsDescriptionPresenter.ViewHolder viewHolder, Object itemData) {
        AnimeObject animeObject = (AnimeObject) itemData;
        viewHolder.getTitle().setText(animeObject.name);
        viewHolder.getSubtitle().setText(animeObject.getGenresString());
        viewHolder.getBody().setText(animeObject.description);
        if (titleColor != 0)
            viewHolder.getTitle().setTextColor(titleColor);
        if (bodyColor != 0) {
            viewHolder.getSubtitle().setTextColor(bodyColor);
            viewHolder.getBody().setTextColor(bodyColor);
        }
    }
}
