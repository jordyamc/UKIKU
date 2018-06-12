package knf.kuma.tv.cards;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.tv.BindableCardView;

public class AnimeCardView extends BindableCardView<AnimeObject> {

    @BindView(R.id.img)
    ImageView imageView;
    @BindView(R.id.title)
    TextView title;

    public AnimeCardView(Context context) {
        super(context);
        ButterKnife.bind(this);
    }

    @Override
    public void bind(AnimeObject data) {
        PicassoSingle.get(context).load(data.img).into(imageView);
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
