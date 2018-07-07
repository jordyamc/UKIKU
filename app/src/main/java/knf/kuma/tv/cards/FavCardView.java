package knf.kuma.tv.cards;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.FavoriteObject;
import knf.kuma.tv.BindableCardView;

public class FavCardView extends BindableCardView<FavoriteObject> {

    @BindView(R.id.img)
    ImageView imageView;
    @BindView(R.id.title)
    TextView title;

    public FavCardView(Context context) {
        super(context);
        ButterKnife.bind(this);
    }

    @Override
    public void bind(FavoriteObject data) {
        PicassoSingle.get(context).load(PatternUtil.getCover(data.aid)).into(imageView);
        title.setText(data.name);
    }

    @Override
    public ImageView getImageView() {
        return imageView;
    }

    @Override
    public int getLayoutResource() {
        return R.layout.item_tv_card;
    }
}
