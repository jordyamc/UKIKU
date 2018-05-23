package knf.kuma.queue;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.QueueObject;

public class QueueAnimesAdapter extends RecyclerView.Adapter<QueueAnimesAdapter.AnimeHolder> {

    private Context context;
    private OnAnimeSelectedListener listener;
    private List<QueueObject> list = new ArrayList<>();

    QueueAnimesAdapter(Context context) {
        this.context = context;
        this.listener = (OnAnimeSelectedListener) context;
    }

    @NonNull
    @Override
    public AnimeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AnimeHolder(LayoutInflater.from(parent.getContext()).inflate(getLayout(), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AnimeHolder holder, int position) {
        final QueueObject object = list.get(position);
        PicassoSingle.get(context).load(PatternUtil.getCover(object.chapter.aid)).into(holder.imageView);
        holder.title.setText(object.chapter.name);
        int count = CacheDB.INSTANCE.queueDAO().countAlone(object.chapter.aid);
        holder.type.setText(String.format(Locale.getDefault(), count == 1 ? "%d episodio" : "%d episodios", count));
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSelect(object);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void update(List<QueueObject> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("lay_type", "0").equals("0")) {
            return R.layout.item_fav;
        } else {
            return R.layout.item_fav_grid;
        }
    }

    interface OnAnimeSelectedListener {
        void onSelect(QueueObject object);
    }

    class AnimeHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.type)
        TextView type;

        AnimeHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
