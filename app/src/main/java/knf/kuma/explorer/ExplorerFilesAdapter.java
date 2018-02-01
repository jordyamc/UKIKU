package knf.kuma.explorer;

import android.app.Activity;
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
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.ExplorerObject;

/**
 * Created by Jordy on 30/01/2018.
 */

public class ExplorerFilesAdapter extends RecyclerView.Adapter<ExplorerFilesAdapter.FileItem> {

    private List<ExplorerObject> list = new ArrayList<>();
    private Fragment fragment;
    private FragmentFiles.SelectedListener listener;

    public ExplorerFilesAdapter(Fragment fragment, FragmentFiles.SelectedListener listener) {
        this.fragment=fragment;
        this.listener = listener;
    }

    @Override
    public FileItem onCreateViewHolder(ViewGroup parent, int viewType) {
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

    @Override
    public void onBindViewHolder(final FileItem holder, int position) {
        final ExplorerObject object=list.get(position);
        PicassoSingle.get(fragment.getContext()).load(object.img).into(holder.imageView);
        holder.title.setText(object.name);
        holder.chapter.setText(String.format(Locale.getDefault(),object.count==1?"%d archivo":"%d archivos",object.count));
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSelected(object.fileName);
            }
        });
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ActivityAnime.open(fragment,object,holder.imageView);
                return true;
            }
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
