package knf.kuma.tv.cards;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.RecordObject;
import knf.kuma.tv.BindableCardView;

public class RecordCardView extends BindableCardView<RecordObject> {

    @BindView(R.id.img)
    ImageView imageView;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.chapter)
    TextView chapter;

    public RecordCardView(Context context) {
        super(context);
        ButterKnife.bind(this);
    }

    @Override
    public void bind(RecordObject data) {
        PicassoSingle.get(context).load(PatternUtil.getCover(data.aid)).into(imageView);
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
