package knf.kuma.emision;

import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.custom.HiddenOverlay;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.widgets.emision.WEmisionProvider;

public class EmisionAdapter extends RecyclerView.Adapter<EmisionAdapter.EmisionItem> {

    public List<AnimeObject> list = new ArrayList<>();
    private Fragment fragment;

    private Set<String> blacklist;
    private boolean showHidden;

    EmisionAdapter(Fragment fragment) {
        this.fragment = fragment;
        this.blacklist = PreferenceManager.getDefaultSharedPreferences(fragment.getContext()).getStringSet("emision_blacklist", new LinkedHashSet<String>());
        this.showHidden = PreferenceManager.getDefaultSharedPreferences(fragment.getContext()).getBoolean("show_hidden", false);
    }

    @NonNull
    @Override
    public EmisionItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EmisionItem(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emision, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final EmisionItem holder, int position) {
        final AnimeObject animeObject = list.get(position);
        PicassoSingle.get(fragment.getContext()).load(PatternUtil.getCover(animeObject.aid)).into(holder.imageView);
        holder.title.setText(animeObject.name);
        holder.hiddenOverlay.setHidden(blacklist.contains(animeObject.aid), false);
        holder.cardView.setOnClickListener(v -> ActivityAnime.open(fragment, animeObject, holder.imageView, true, true));
        holder.cardView.setOnLongClickListener(v -> {
            boolean removed;
            if (blacklist.contains(animeObject.aid)) {
                updateList(true, animeObject.aid);
                removed = true;
            } else {
                updateList(false, animeObject.aid);
                removed = false;
            }
            if (showHidden) {
                holder.hiddenOverlay.setHidden(!removed, true);
            } else if (!removed) {
                remove(holder.getAdapterPosition());
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void update(List<AnimeObject> list) {
        this.blacklist = PreferenceManager.getDefaultSharedPreferences(fragment.getContext()).getStringSet("emision_blacklist", new LinkedHashSet<String>());
        this.showHidden = PreferenceManager.getDefaultSharedPreferences(fragment.getContext()).getBoolean("show_hidden", false);
        this.list = list;
        notifyDataSetChanged();
    }

    private void updateList(boolean remove, String aid) {
        this.blacklist = new LinkedHashSet<>(PreferenceManager.getDefaultSharedPreferences(fragment.getContext()).getStringSet("emision_blacklist", new LinkedHashSet<String>()));
        if (remove) blacklist.remove(aid);
        else blacklist.add(aid);
        PreferenceManager.getDefaultSharedPreferences(fragment.getContext()).edit().putStringSet("emision_blacklist", blacklist).apply();
        WEmisionProvider.update(fragment.getContext());
    }

    public void remove(int position) {
        if (position >= 0 && position <= list.size() - 1) {
            list.remove(position);
            notifyItemRemoved(position);
        }
    }

    class EmisionItem extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.hidden)
        HiddenOverlay hiddenOverlay;
        @BindView(R.id.title)
        TextView title;

        EmisionItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
