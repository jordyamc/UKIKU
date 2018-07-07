package knf.kuma.random;

import android.app.Activity;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.AnimeObject;

public class RandomAdapter extends RecyclerView.Adapter<RandomAdapter.RandomItem> {

    private Activity activity;
    private List<AnimeObject> list = new ArrayList<>();

    public RandomAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public RandomItem onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RandomItem(LayoutInflater.from(activity).inflate(getLayout(), parent, false));
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(activity).getString("lay_type", "0").equals("0")) {
            return R.layout.item_fav;
        } else {
            return R.layout.item_fav_grid;
        }
    }

    @Override
    public void onBindViewHolder(final RandomItem holder, int position) {
        final AnimeObject animeObject = list.get(position);
        PicassoSingle.get(activity).load(PatternUtil.getCover(animeObject.aid)).into(holder.imageView);
        holder.title.setText(animeObject.name);
        holder.type.setText(animeObject.type);
        holder.cardView.setOnClickListener(v -> ActivityAnime.open(activity, animeObject, holder.imageView, false, true));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void update(List<AnimeObject> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    class RandomItem extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.type)
        TextView type;

        public RandomItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
