package knf.kuma.explorer;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.download.DownloadManager;
import knf.kuma.pojos.DownloadObject;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class DownloadingAdapter extends RecyclerView.Adapter<DownloadingAdapter.DownloadingItem> {

    private Fragment fragment;
    private DownloadsDAO downloadsDAO = CacheDB.INSTANCE.downloadsDAO();
    private List<DownloadObject> downloadObjects;

    DownloadingAdapter(Fragment fragment, List<DownloadObject> downloadObjects) {
        this.fragment = fragment;
        this.downloadObjects = downloadObjects;
    }

    @NonNull
    @Override
    public DownloadingItem onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new DownloadingItem(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_downloading_extra, viewGroup, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final DownloadingItem holder, int position) {
        final DownloadObject downloadObject = downloadObjects.get(position);
        holder.title.setText(downloadObject.name);
        holder.chapter.setText(downloadObject.chapter);
        holder.eta.setText(downloadObject.getSubtext());
        holder.progress.setMax(100);
        if (downloadObject.state == DownloadObject.PENDING) {
            holder.eta.setVisibility(View.GONE);
            holder.progress.setIndeterminate(true);
            holder.progress.setProgress(0);
        } else {
            if (downloadObject.state == DownloadObject.PAUSED)
                holder.eta.setVisibility(View.GONE);
            else
                holder.eta.setVisibility(View.VISIBLE);
            holder.progress.setIndeterminate(false);
            holder.progress.setProgress(downloadObject.progress);
        }
        holder.action.setOnClickListener(view -> {
            if (downloadObject.state == DownloadObject.DOWNLOADING) {
                downloadObject.state = DownloadObject.PAUSED;
                holder.action.setText("REANUDAR");
                DownloadManager.pause(downloadObject);
            } else if (downloadObject.state == DownloadObject.PAUSED) {
                downloadObject.state = DownloadObject.PENDING;
                holder.action.setText("PAUSAR");
                DownloadManager.resume(downloadObject);
            }
        });
        holder.cancel.setOnClickListener(v -> {
            try {
                new MaterialDialog.Builder(Objects.requireNonNull(fragment.getContext()))
                        .content("¿Cancelar descarga del " + downloadObject.chapter.toLowerCase() + " de " + downloadObject.name + "?")
                        .positiveText("CONFIRMAR")
                        .negativeText("CANCELAR")
                        .onPositive((dialog, which) -> {
                            downloadObjects.remove(holder.getAdapterPosition());
                            notifyItemRemoved(holder.getAdapterPosition());
                            //downloadsDAO.deleteByEid(downloadObject.eid);
                            DownloadManager.cancel(downloadObject.eid);
                        }).build().show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        downloadsDAO.getLiveByKey(downloadObject.key).observe(fragment, object -> {
            try {
                if (object == null || object.state == DownloadObject.COMPLETED) {
                    downloadObjects.remove(holder.getAdapterPosition());
                    notifyItemRemoved(holder.getAdapterPosition());
                } else {
                    downloadObject.state = object.state;
                    if (object.state == DownloadObject.PENDING) {
                        holder.eta.setVisibility(View.GONE);
                        holder.progress.setIndeterminate(true);
                        holder.progress.setProgress(0);
                    } else {
                        switch (downloadObject.state) {
                            case DownloadObject.DOWNLOADING:
                                holder.action.setText("PAUSAR");
                                holder.eta.setVisibility(View.VISIBLE);
                                break;
                            case DownloadObject.PAUSED:
                                holder.action.setText("REANUDAR");
                                holder.eta.setVisibility(View.GONE);
                                break;
                        }
                        holder.progress.setIndeterminate(false);
                        holder.progress.setProgress(object.progress);
                        holder.eta.setText(object.getSubtext());
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
        @BindView(R.id.eta)
        TextView eta;
        @BindView(R.id.action)
        Button action;
        @BindView(R.id.cancel)
        Button cancel;
        @BindView(R.id.progress)
        MaterialProgressBar progress;

        DownloadingItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
