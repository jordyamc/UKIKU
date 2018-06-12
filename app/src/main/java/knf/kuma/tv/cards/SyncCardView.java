package knf.kuma.tv.cards;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.tv.BindableCardView;
import knf.kuma.tv.sync.SyncObject;

public class SyncCardView extends BindableCardView<SyncObject> {

    @BindView(R.id.img)
    ImageView imageView;
    @BindView(R.id.title)
    TextView title;

    public SyncCardView(Context context) {
        super(context);
        ButterKnife.bind(this);
    }

    @Override
    public void bind(SyncObject data) {
        PicassoSingle.get(context).load(data.getImage()).into(imageView);
        title.setText(data.getTitle());
    }

    @Override
    public ImageView getImageView() {
        return imageView;
    }

    @Override
    public int getLayoutResource() {
        return R.layout.item_tv_card_sync;
    }
}
