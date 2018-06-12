package knf.kuma.tv.cards;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.tv.BindableCardView;

public class ChapterCardView extends BindableCardView<AnimeObject.WebInfo.AnimeChapter> {

    @BindView(R.id.img)
    ImageView imageView;
    @BindView(R.id.indicator)
    ImageView indicator;
    @BindView(R.id.chapter)
    TextView chapter;

    public ChapterCardView(Context context) {
        super(context);
        ButterKnife.bind(this);
    }

    @Override
    public void bind(AnimeObject.WebInfo.AnimeChapter data) {
        PicassoSingle.get(context).load(data.img).into(imageView);
        indicator.setVisibility(CacheDB.INSTANCE.chaptersDAO().chapterIsSeen(data.eid) ? VISIBLE : GONE);
        chapter.setText(data.number);
    }

    @Override
    public ImageView getImageView() {
        return imageView;
    }

    @Override
    public int getLayoutResource() {
        return R.layout.item_tv_card_chapter_preview;
    }
}
