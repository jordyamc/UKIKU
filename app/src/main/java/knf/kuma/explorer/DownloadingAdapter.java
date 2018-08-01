package knf.kuma.explorer;

import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.pojos.DownloadObject;

public class DownloadingAdapter extends RecyclerView.Adapter<DownloadingAdapter.DownloadingItem> {

    private Fragment fragment;
    private DownloadsDAO downloadsDAO = CacheDB.INSTANCE.downloadsDAO();
    private List<DownloadObject> downloadObjects = new ArrayList<>();

    public DownloadingAdapter(Fragment fragment, List<DownloadObject> downloadObjects) {
        this.fragment = fragment;
        this.downloadObjects = downloadObjects;
    }

    @NonNull
    @Override
    public DownloadingItem onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new DownloadingItem(LayoutInflater.from(viewGroup.getContext()).inflate(getLayout(), viewGroup, false));
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(fragment.getContext()).getString("lay_type", "0").equals("0")) {
            return R.layout.item_downloading;
        } else {
            return R.layout.item_downloading_grid;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final DownloadingItem holder, int position) {
        final DownloadObject downloadObject = downloadObjects.get(position);
        holder.title.setText(downloadObject.name);
        holder.chapter.setText(downloadObject.chapter);
        holder.size.setText(downloadObject.getSize());
        holder.progress.setMax(100);
        if (downloadObject.state == DownloadObject.PENDING) {
            holder.progress.setIndeterminate(true);
            holder.progress.setProgress(0);
        } else {
            holder.progress.setIndeterminate(false);
            holder.progress.setProgress(downloadObject.progress);
        }
        holder.action.setOnClickListener(v -> {
            try {
                new MaterialDialog.Builder(fragment.getContext())
                        .content("¿Cancelar descarga del " + downloadObject.chapter.toLowerCase() + " de " + downloadObject.name + "?")
                        .positiveText("CONFIRMAR")
                        .negativeText("CANCELAR")
                        .onPositive((dialog, which) -> {
                            downloadObjects.remove(holder.getAdapterPosition());
                            notifyItemRemoved(holder.getAdapterPosition());
                            downloadsDAO.deleteByEid(downloadObject.eid);
                        }).build().show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        downloadsDAO.getLiveByKey(downloadObject.key).observe(fragment, object -> {
            try {
                Log.e("Download Updated", object == null ? "Not exist" : "Progress: " + object.progress);
                if (object == null || object.state == DownloadObject.COMPLETED) {
                    downloadObjects.remove(holder.getAdapterPosition());
                    notifyItemRemoved(holder.getAdapterPosition());
                } else {
                    if (object.state == DownloadObject.PENDING) {
                        holder.progress.setIndeterminate(true);
                        holder.progress.setProgress(0);
                    } else {
                        holder.progress.setIndeterminate(false);
                        holder.progress.setProgress(object.progress);
                        holder.size.setText(object.getSize());
                    }
                }
            } catch (Exception e) {
                //
            }
        });
    }

    @Override
    public int getItemCount() {
        return downloadObjects.size();
    }

    class DownloadingItem extends RecyclerView.ViewHolder {
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.chapter)
        TextView chapter;
        @BindView(R.id.size)
        TextView size;
        @BindView(R.id.action)
        ImageButton action;
        @BindView(R.id.progress)
        ProgressBar progress;

        DownloadingItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
