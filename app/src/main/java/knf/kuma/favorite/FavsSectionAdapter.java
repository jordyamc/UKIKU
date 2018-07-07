package knf.kuma.favorite;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import knf.kuma.commons.PrefsUtil;
import knf.kuma.favorite.objects.InfoContainer;
import knf.kuma.pojos.FavoriteObject;

public class FavsSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static final int TYPE_HEADER = 0;
    static final int TYPE_ITEM = 1;

    private Context context;
    private Fragment fragment;
    private RecyclerView recyclerView;
    private OnMoveListener listener;
    private List<FavoriteObject> list = new ArrayList<>();

    public FavsSectionAdapter(Fragment fragment, RecyclerView recyclerView) {
        this.fragment = fragment;
        this.listener = (OnMoveListener) fragment;
        this.context = fragment.getContext();
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            default:
            case TYPE_HEADER:
                return new HeaderHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fav_header, parent, false));
            case TYPE_ITEM:
                return new ItemHolder(LayoutInflater.from(parent.getContext()).inflate(getLayout(), parent, false));
        }
    }

    @LayoutRes
    private int getLayout() {
        if (PrefsUtil.getLayType().equals("0")) {
            return R.layout.item_fav;
        } else {
            return R.layout.item_fav_grid;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        FavoriteObject object = list.get(position);
        if (h instanceof HeaderHolder) {
            HeaderHolder holder = (HeaderHolder) h;
            holder.header.setText(object.name);
            holder.action.setOnClickListener(v -> listener.onEdit(object.name));
        } else if (h instanceof ItemHolder) {
            ItemHolder holder = (ItemHolder) h;
            PicassoSingle.get(context).load(PatternUtil.getCover(object.aid)).into(holder.imageView);
            holder.title.setText(object.name);
            holder.type.setText(object.type);
            holder.cardView.setOnClickListener(view -> ActivityAnime.open(fragment, object, holder.imageView));
            holder.cardView.setOnLongClickListener(v -> {
                listener.onSelect(object);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).isSection ? TYPE_HEADER : TYPE_ITEM;
    }

    public void updatePosition(InfoContainer container) {
        list = container.updated;
        recyclerView.post(() -> notifyItemMoved(container.from, container.to));
    }

    public void updateList(List<FavoriteObject> list) {
        this.list = list;
        recyclerView.post(this::notifyDataSetChanged);
    }

    interface OnMoveListener {
        void onSelect(FavoriteObject object);

        void onEdit(String category);
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.type)
        TextView type;

        public ItemHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class HeaderHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.header)
        TextView header;
        @BindView(R.id.action)
        ImageButton action;

        public HeaderHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
