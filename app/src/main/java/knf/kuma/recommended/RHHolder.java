package knf.kuma.recommended;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;

/**
 * Created by jordy on 26/03/2018.
 */

public class RHHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.title)
    public TextView title;

    public RHHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
