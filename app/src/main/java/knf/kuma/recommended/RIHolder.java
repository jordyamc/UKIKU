package knf.kuma.recommended;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;

/**
 * Created by jordy on 26/03/2018.
 */

public class RIHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.card)
    public CardView cardView;
    @BindView(R.id.img)
    public ImageView img;
    @BindView(R.id.title)
    public TextView title;
    @BindView(R.id.type)
    public TextView type;

    public RIHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
