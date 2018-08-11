package knf.kuma.recommended;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.GenreStatusObject;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankHolder> {

    private List<GenreStatusObject> list = CacheDB.INSTANCE.genresDAO().getRanking();
    private int total = 0;

    public RankingAdapter() {
        for (GenreStatusObject object : list)
            total += object.count;
    }

    @NonNull
    @Override
    public RankHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RankHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ranking, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RankHolder holder, int position) {
        GenreStatusObject object = list.get(position);
        holder.title.setText(object.name);
        holder.count.setText(String.valueOf(object.count));
        holder.ranking.setMax(total);
        holder.ranking.setProgress(object.count);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class RankHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.count)
        TextView count;
        @BindView(R.id.ranking)
        ProgressBar ranking;

        RankHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
