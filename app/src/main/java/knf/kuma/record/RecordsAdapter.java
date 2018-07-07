package knf.kuma.record;

import android.app.Activity;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.RecordsDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.RecordObject;
import xdroid.toaster.Toaster;

public class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.RecordItem> {
    private Activity activity;
    private List<RecordObject> items = new ArrayList<>();

    private RecordsDAO dao = CacheDB.INSTANCE.recordsDAO();

    public RecordsAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public RecordItem onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecordItem(LayoutInflater.from(parent.getContext()).inflate(getLayout(), parent, false));
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(activity).getString("lay_type", "0").equals("0")) {
            return R.layout.item_record;
        } else {
            return R.layout.item_record_grid;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecordItem holder, int position) {
        final RecordObject item = items.get(position);
        AnimeObject animeObject = item.animeObject;
        if (animeObject != null)
            PicassoSingle.get(activity).load(PatternUtil.getCover(animeObject.aid)).into(holder.imageView);
        holder.title.setText(item.name);
        holder.chapter.setText(item.chapter);
        holder.cardView.setOnClickListener(v -> {
            if (item.animeObject != null)
                ActivityAnime.open(activity, item, holder.imageView);
            else Toaster.toast("Error al abrir");
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void remove(int position) {
        dao.delete(items.get(position));
        items.remove(position);
        notifyItemRemoved(position);
    }

    public void update(List<RecordObject> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    class RecordItem extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.chapter)
        TextView chapter;

        RecordItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
