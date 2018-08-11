package knf.kuma.directory;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.AnimeObject;

public class DirectorypageAdapter extends PagedListAdapter<AnimeObject,DirectorypageAdapter.ItemHolder> {

    private static final DiffUtil.ItemCallback<AnimeObject> DIFF_CALLBACK = new DiffUtil.ItemCallback<AnimeObject>() {
        @Override
        public boolean areItemsTheSame(@NonNull AnimeObject oldItem, @NonNull AnimeObject newItem) {
            return oldItem.key == newItem.key;
        }

        @Override
        public boolean areContentsTheSame(@NonNull AnimeObject oldItem, @NonNull AnimeObject newItem) {
            return oldItem.name.equals(newItem.name);
        }
    };
    private Fragment fragment;

    DirectorypageAdapter(Fragment fragment) {
        super(DIFF_CALLBACK);
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemHolder(LayoutInflater.from(parent.getContext()).inflate(getLayType(), parent, false));
    }

    @LayoutRes
    private int getLayType(){
        if (PreferenceManager.getDefaultSharedPreferences(fragment.getContext()).getString("lay_type","0").equals("0")){
            return R.layout.item_dir;
        }else {
            return R.layout.item_dir_grid;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemHolder holder, int position) {
        final AnimeObject object=getItem(position);
        if (object!=null){
            PicassoSingle.get(fragment.getContext()).load(PatternUtil.getCover(object.aid)).into(holder.imageView);
            holder.textView.setText(object.name);
            holder.cardView.setOnClickListener(view -> ActivityAnime.open(fragment, object, holder.imageView));
        }
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.title)
        TextView textView;

        public ItemHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
