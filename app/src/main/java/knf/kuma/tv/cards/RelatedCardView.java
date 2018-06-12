package knf.kuma.tv.cards;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.tv.BindableCardView;

public class RelatedCardView extends BindableCardView<AnimeObject.WebInfo.AnimeRelated> {

    @BindView(R.id.img)
    ImageView imageView;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.chapter)
    TextView type;

    public RelatedCardView(Context context) {
        super(context);
        ButterKnife.bind(this);
    }

    @Override
    public void bind(AnimeObject.WebInfo.AnimeRelated data) {
        AnimeObject object = CacheDB.INSTANCE.animeDAO().getByLink("%" + data.link);
        if (object != null)
            PicassoSingle.get(context).load(object.img).into(imageView);
        else
            PicassoSingle.get(context).load((Uri) null).into(imageView);
        title.setText(data.name);
        type.setText(data.relation);
    }

    @Override
    public ImageView getImageView() {
        return imageView;
    }

    @Override
    public int getLayoutResource() {
        return R.layout.item_tv_card_chapter;
    }
}
