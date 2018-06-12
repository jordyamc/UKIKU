package knf.kuma.tv.cards;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.RecentObject;
import knf.kuma.tv.BindableCardView;

public class RecentsCardView extends BindableCardView<RecentObject> {

    @BindView(R.id.img)
    ImageView imageView;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.chapter)
    TextView chapter;

    public RecentsCardView(Context context) {
        super(context);
        ButterKnife.bind(this);
    }

    @Override
    public void bind(RecentObject data) {
        PicassoSingle.get(context).load(data.img).into(imageView);
        title.setText(data.name);
        chapter.setText(data.chapter);
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
