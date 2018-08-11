package knf.kuma.queue;

import android.app.Activity;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.QueueObject;

public class QueueAnimesAdapter extends RecyclerView.Adapter<QueueAnimesAdapter.AnimeHolder> {

    private Activity activity;
    private OnAnimeSelectedListener listener;
    private List<QueueObject> list = new ArrayList<>();

    QueueAnimesAdapter(Activity activity) {
        this.activity = activity;
        this.listener = (OnAnimeSelectedListener) activity;
    }

    @NonNull
    @Override
    public AnimeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AnimeHolder(LayoutInflater.from(parent.getContext()).inflate(getLayout(), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final AnimeHolder holder, int position) {
        final QueueObject object = list.get(position);
        final String img = PatternUtil.getCover(object.chapter.aid);
        PicassoSingle.get(activity).load(img).into(holder.imageView);
        holder.title.setText(object.chapter.name);
        int count = CacheDB.INSTANCE.queueDAO().countAlone(object.chapter.aid);
        holder.type.setText(String.format(Locale.getDefault(), count == 1 ? "%d episodio" : "%d episodios", count));
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onSelect(object);
            }
        });
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ActivityAnime.open(activity, object, img, holder.imageView);
                return true;
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

    public void clear() {
        listener = null;
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(activity).getString("lay_type", "0").equals("0")) {
            return R.layout.item_anim_queue;
        } else {
            return R.layout.item_anim_queue_grid;
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
