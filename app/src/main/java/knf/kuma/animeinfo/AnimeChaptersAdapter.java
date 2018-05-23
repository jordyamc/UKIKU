package knf.kuma.animeinfo;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener;
import com.squareup.picasso.Callback;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.CastUtil;
import knf.kuma.commons.EAHelper;
import knf.kuma.commons.Network;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.commons.SelfServer;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.ChaptersDAO;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.database.dao.RecordsDAO;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.downloadservice.FileAccessHelper;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.RecordObject;
import knf.kuma.pojos.SeeingObject;
import knf.kuma.queue.QueueManager;
import knf.kuma.videoservers.ServersFactory;
import xdroid.toaster.Toaster;

public class AnimeChaptersAdapter extends RecyclerView.Adapter<AnimeChaptersAdapter.ChapterImgHolder> {

    private Context context;
    private Fragment fragment;
    private ChaptersDAO chaptersDAO = CacheDB.INSTANCE.chaptersDAO();
    private RecordsDAO recordsDAO = CacheDB.INSTANCE.recordsDAO();
    private SeeingDAO seeingDAO = CacheDB.INSTANCE.seeingDAO();
    private DownloadsDAO downloadsDAO = CacheDB.INSTANCE.downloadsDAO();
    private List<AnimeObject.WebInfo.AnimeChapter> chapters = new ArrayList<>();
    private DragSelectTouchListener touchListener;
    private boolean isNetworkAvailable = Network.isConnected();
    private HashSet<Integer> selected = new HashSet<>();
    private SeeingObject seeingObject;

    public AnimeChaptersAdapter(Fragment fragment, List<AnimeObject.WebInfo.AnimeChapter> chapters, DragSelectTouchListener touchListener) {
        this.context = fragment.getContext();
        this.fragment = fragment;
        this.chapters = chapters;
        this.touchListener = touchListener;
        chaptersDAO.init();
        if (chapters.size() > 0)
            seeingObject = seeingDAO.getByAid(chapters.get(0).aid);
    }

    @NonNull
    @Override
    public ChapterImgHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChapterImgHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter_preview, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ChapterImgHolder holder, int position) {
        final AnimeObject.WebInfo.AnimeChapter chapter = chapters.get(position);
        final DownloadObject downloadObject = downloadsDAO.getByEid(chapter.eid);
        final File d_file = FileAccessHelper.INSTANCE.getFile(chapter.getFileName());
        if (selected.contains(position)) {
            holder.cardView.setCardBackgroundColor(context.getResources().getColor(EAHelper.getThemeColorLight(context)));
        } else {
            holder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.cardview_background));
        }
        chapter.isDownloaded = canPlay(d_file);
        downloadsDAO.getLiveByEid(chapter.eid).observe(fragment, new Observer<DownloadObject>() {
            @Override
            public void onChanged(@Nullable DownloadObject object) {
                if (object != null && downloadObject != null)
                    downloadObject.state = object.state;
            }
        });
        if (!Network.isConnected() || chapter.img == null)
            holder.imageView.setVisibility(View.GONE);
        if (chapter.img != null)
            PicassoSingle.get(context).load(chapter.img).into(holder.imageView, new Callback() {
                @Override
                public void onSuccess() {
                    holder.imageView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError() {

                }
            });
        CastUtil.get().getCasting().observe(fragment, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                holder.setDownloaded(isPlayAvailable(chapter.eid, d_file), chapter.eid.equals(s));
                if (!chapter.eid.equals(s))
                    holder.setQueue(QueueManager.isInQueue(chapter.eid), isPlayAvailable(chapter.eid, d_file));
            }
        });
        holder.chapter.setTextColor(context.getResources().getColor(chaptersDAO.chapterIsSeen(chapter.eid) ? EAHelper.getThemeColor(context) : R.color.textPrimary));
        holder.separator.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
        holder.chapter.setText(chapter.number);
        holder.actions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(context, view);
                if (CastUtil.get().getCasting().getValue().equals(chapter.eid)) {
                    menu.inflate(R.menu.chapter_casting_menu);
                    if (canPlay(d_file))
                        menu.getMenu().findItem(R.id.download).setVisible(false);
                } else if (isPlayAvailable(chapter.eid, d_file)) {
                    menu.inflate(R.menu.chapter_downloaded_menu);
                    if (!CastUtil.get().connected())
                        menu.getMenu().findItem(R.id.cast).setVisible(false);
                } else {
                    menu.inflate(R.menu.chapter_menu);
                }
                if (QueueManager.isInQueue(chapter.eid))
                    menu.getMenu().findItem(R.id.queue).setVisible(false);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.play:
                                if (canPlay(d_file)) {
                                    chaptersDAO.addChapter(chapter);
                                    recordsDAO.add(RecordObject.fromChapter(chapter));
                                    updateSeeing(chapter.number);
                                    holder.setSeen(context, true);
                                    ServersFactory.startPlay(context, chapter.getEpTitle(), chapter.getFileName());
                                } else {
                                    Toaster.toast("Aun no se está descargando");
                                }
                                break;
                            case R.id.cast:
                                if (canPlay(d_file)) {
                                    CastUtil.get().play(fragment.getActivity(), chapter.eid, SelfServer.start(chapter.getFileName()), chapter.name, chapter.number, chapter.img == null ? chapter.aid : chapter.img, chapter.img == null);
                                    chaptersDAO.addChapter(chapter);
                                    recordsDAO.add(RecordObject.fromChapter(chapter));
                                    updateSeeing(chapter.number);
                                    holder.setSeen(context, true);
                                }
                                break;
                            case R.id.casting:
                                CastUtil.get().openControls();
                                break;
                            case R.id.delete:
                                new MaterialDialog.Builder(context)
                                        .content("¿Eliminar el " + chapter.number.toLowerCase() + "?")
                                        .positiveText("CONFIRMAR")
                                        .negativeText("CANCELAR")
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                if (downloadObject != null)
                                                    downloadObject.state = -8;
                                                chapter.isDownloaded = false;
                                                holder.setDownloaded(false, false);
                                                FileAccessHelper.INSTANCE.delete(chapter.getFileName());
                                                CacheDB.INSTANCE.downloadsDAO().deleteByEid(chapter.eid);
                                                QueueManager.remove(chapter.eid);
                                            }
                                        }).build().show();
                                break;
                            case R.id.download:
                                ServersFactory.start(context, chapter.link, chapter, false, new ServersFactory.ServersInterface() {
                                    @Override
                                    public void onFinish(boolean started, boolean success) {
                                        if (started) {
                                            holder.setQueue(CacheDB.INSTANCE.queueDAO().isInQueue(chapter.eid), true);
                                            chapter.isDownloaded = true;
                                        }
                                    }

                                    @Override
                                    public void onCast(String url) {

                                    }
                                });
                                break;
                            case R.id.streaming:
                                ServersFactory.start(context, chapter.link, chapter, true, new ServersFactory.ServersInterface() {
                                    @Override
                                    public void onFinish(boolean started, boolean success) {
                                        if (!started && success) {
                                            chaptersDAO.addChapter(chapter);
                                            recordsDAO.add(RecordObject.fromChapter(chapter));
                                            updateSeeing(chapter.number);
                                            holder.setSeen(context, true);
                                        }
                                    }

                                    @Override
                                    public void onCast(String url) {
                                        CastUtil.get().play(fragment.getActivity(), chapter.eid, url, chapter.name, chapter.number, chapter.img == null ? chapter.aid : chapter.img, chapter.img == null);
                                        chaptersDAO.addChapter(chapter);
                                        recordsDAO.add(RecordObject.fromChapter(chapter));
                                        updateSeeing(chapter.number);
                                        holder.setSeen(context, true);
                                    }
                                });
                                break;
                            case R.id.queue:
                                if (isPlayAvailable(chapter.eid, d_file)) {
                                    QueueManager.add(Uri.fromFile(d_file), true, chapter);
                                    holder.setQueue(true, true);
                                } else
                                    ServersFactory.start(context, chapter.link, chapter, true, true, new ServersFactory.ServersInterface() {
                                        @Override
                                        public void onFinish(boolean started, boolean success) {
                                            if (success) {
                                                holder.setQueue(true, false);
                                            }
                                        }

                                        @Override
                                        public void onCast(String url) {
                                        }
                                    });
                        }
                        return true;
                    }
                });
                menu.show();
            }
        });
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chaptersDAO.chapterIsSeen(chapter.eid)) {
                    chaptersDAO.deleteChapter(chapter);
                    holder.chapter.setTextColor(context.getResources().getColor(R.color.textPrimary));
                } else {
                    chaptersDAO.addChapter(chapter);
                    holder.chapter.setTextColor(context.getResources().getColor(R.color.colorAccent));
                }
                updateSeeing(chapter.number);
            }
        });
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                touchListener.startDragSelection(holder.getAdapterPosition());
                return true;
            }
        });
        if (!isNetworkAvailable && !isPlayAvailable(chapter.eid, d_file)) {
            holder.actions.setVisibility(View.GONE);
        } else {
            holder.actions.setVisibility(View.VISIBLE);
        }
    }

    private void updateSeeing(String chapter) {
        if (seeingObject != null) {
            seeingObject.chapter = chapter;
            seeingDAO.update(seeingObject);
        }
    }

    private boolean isPlayAvailable(String eid, File file) {
        DownloadObject downloadObject = downloadsDAO.getByEid(eid);
        return (file != null && file.exists()) || (downloadObject != null && downloadObject.isDownloading());
    }

    private boolean canPlay(File file) {
        return file != null && file.exists();
    }

    @Override
    public int getItemViewType(int position) {
        return chapters.get(position).chapterType.value;
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    public void select(int pos, boolean sel) {
        if (sel) {
            selected.add(pos);
        } else {
            selected.remove(pos);
        }
        notifyItemChanged(pos);
    }

    public void selectRange(int start, int end, boolean sel) {
        for (int i = start; i <= end; i++) {
            if (sel)
                selected.add(i);
            else
                selected.remove(i);
        }
        notifyItemRangeChanged(start, end - start + 1);
    }

    public void deselectAll() {
        selected.clear();
        notifyDataSetChanged();
    }

    public int getCountSelected() {
        return selected.size();
    }

    public HashSet<Integer> getSelection() {
        return selected;
    }

    class ChapterImgHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.separator)
        View separator;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.chapter)
        TextView chapter;
        @BindView(R.id.in_down)
        ImageView in_down;
        @BindView(R.id.actions)
        ImageButton actions;

        ChapterImgHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setDownloaded(final boolean downloaded, final boolean isCasting) {
            in_down.post(new Runnable() {
                @Override
                public void run() {
                    if (downloaded)
                        in_down.setImageResource(R.drawable.ic_chap_down);
                    if (isCasting)
                        in_down.setImageResource(R.drawable.ic_casting);
                    in_down.setVisibility(downloaded || isCasting ? View.VISIBLE : View.GONE);
                }
            });
        }

        void setQueue(final boolean isInQueue, final boolean isDownloaded) {
            in_down.post(new Runnable() {
                @Override
                public void run() {
                    if (!isInQueue)
                        setDownloaded(isDownloaded, false);
                    else {
                        in_down.setImageResource(isDownloaded ? R.drawable.ic_queue_file : R.drawable.ic_queue_normal);
                        in_down.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        void setSeen(final Context context, final boolean seen) {
            chapter.post(new Runnable() {
                @Override
                public void run() {
                    chapter.setTextColor(context.getResources().getColor(seen ? EAHelper.getThemeColor(context) : R.color.textPrimary));
                }
            });
        }
    }

}
