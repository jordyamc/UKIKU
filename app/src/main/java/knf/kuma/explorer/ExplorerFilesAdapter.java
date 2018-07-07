package knf.kuma.explorer;

import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
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
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.ExplorerObject;

public class ExplorerFilesAdapter extends RecyclerView.Adapter<ExplorerFilesAdapter.FileItem> {

    private List<ExplorerObject> list = new ArrayList<>();
    private Fragment fragment;
    private FragmentFiles.SelectedListener listener;

    ExplorerFilesAdapter(Fragment fragment, FragmentFiles.SelectedListener listener) {
        this.fragment=fragment;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileItem(LayoutInflater.from(parent.getContext()).inflate(getLayout(), parent, false));
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(fragment.getContext()).getString("lay_type", "0").equals("0")) {
            return R.layout.item_explorer;
        } else {
            return R.layout.item_explorer_grid;
        }
    }

    public void setListener(FragmentFiles.SelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull final FileItem holder, int position) {
        final ExplorerObject object=list.get(position);
        PicassoSingle.get(fragment.getContext()).load(object.img).into(holder.imageView);
        holder.title.setText(object.name);
        holder.chapter.setText(String.format(Locale.getDefault(),object.count==1?"%d archivo":"%d archivos",object.count));
        holder.cardView.setOnClickListener(v -> listener.onSelected(object));
        holder.cardView.setOnLongClickListener(v -> {
            ActivityAnime.open(fragment, object, holder.imageView);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void update(List<ExplorerObject> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    class FileItem extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.chapter)
        TextView chapter;

        FileItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
