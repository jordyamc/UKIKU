package knf.kuma.favorite;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
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
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.FavoriteObject;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ItemHolder> {

    private Context context;
    private Fragment fragment;
    private RecyclerView recyclerView;
    private List<FavoriteObject> list = new ArrayList<>();

    public FavoriteAdapter(Fragment fragment, RecyclerView recyclerView) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.recyclerView = recyclerView;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemHolder(LayoutInflater.from(parent.getContext()).inflate(getLayout(), parent, false));
    }

    @LayoutRes
    private int getLayout(){
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("lay_type","0").equals("0")){
            return R.layout.item_fav;
        }else {
            return R.layout.item_fav_grid;
        }
    }

    @Override
    public void onBindViewHolder(final ItemHolder holder, int position) {
        final FavoriteObject object=list.get(position);
        PicassoSingle.get(context).load(object.img).into(holder.imageView);
        holder.title.setText(object.name);
        holder.type.setText(object.type);
        holder.cardView.setOnClickListener(view -> ActivityAnime.open(fragment, object, holder.imageView));
    }

    public void updateList(List<FavoriteObject> list){
        this.list=list;
        recyclerView.post(() -> notifyDataSetChanged());
    }

    @Override
    public int getItemCount() {
        return list.size();
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
}
