package knf.kuma.recents;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.CastUtil;
import knf.kuma.commons.Network;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.commons.SelfServer;
import knf.kuma.custom.SeenAnimeOverlay;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.ChaptersDAO;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.database.dao.FavsDAO;
import knf.kuma.database.dao.RecordsDAO;
import knf.kuma.directory.DirectoryService;
import knf.kuma.download.DownloadManager;
import knf.kuma.download.FileAccessHelper;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.RecentObject;
import knf.kuma.pojos.RecordObject;
import knf.kuma.queue.QueueManager;
import knf.kuma.videoservers.ServersFactory;
import xdroid.toaster.Toaster;

public class RecentsAdapter extends RecyclerView.Adapter<RecentsAdapter.ItemHolder> {

    private Context context;
    private Fragment fragment;
    private RecyclerView recyclerView;
    private List<RecentObject> list = new ArrayList<>();
    private FavsDAO dao = CacheDB.INSTANCE.favsDAO();
    private AnimeDAO animeDAO = CacheDB.INSTANCE.animeDAO();
    private ChaptersDAO chaptersDAO = CacheDB.INSTANCE.chaptersDAO();
    private RecordsDAO recordsDAO = CacheDB.INSTANCE.recordsDAO();
    private DownloadsDAO downloadsDAO = CacheDB.INSTANCE.downloadsDAO();
    private boolean isNetworkAvailable;

    RecentsAdapter(Fragment fragment, RecyclerView recyclerView) {
        this.context = fragment.getContext();
        this.fragment = fragment;
        this.recyclerView = recyclerView;
        this.isNetworkAvailable = Network.isConnected();
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recents, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemHolder holder, int position) {
        final RecentObject object = list.get(position);
        holder.setState(isNetworkAvailable, object.isChapterDownloaded || object.isDownloading);
        PicassoSingle.get(context).load(PatternUtil.getCover(object.aid)).into(holder.imageView);
        holder.setNew(object.isNew);
        holder.setFav(dao.isFav(Integer.parseInt(object.aid)));
        holder.setSeen(chaptersDAO.chapterIsSeen(object.eid));
        dao.favObserver(Integer.parseInt(object.aid)).observe(fragment, object1 -> holder.setFav(object1 != null));
        holder.setChapterObserver(chaptersDAO.chapterSeen(object.eid), fragment, chapter -> holder.setSeen(chapter != null));
        holder.setDownloadObserver(downloadsDAO.getLiveByEid(object.eid), fragment, downloadObject -> {
            holder.setDownloadState(downloadObject);
            if (downloadObject == null) {
                object.downloadState = -8;
                object.isDownloading = false;
            } else {
                object.isDownloading = downloadObject.state == DownloadObject.DOWNLOADING || downloadObject.state == DownloadObject.PENDING || downloadObject.state == DownloadObject.PAUSED;
                object.downloadState = downloadObject.state;
                File file = FileAccessHelper.INSTANCE.getFile(object.getFileName());
                object.isChapterDownloaded = file.exists();
                if (downloadObject.state == DownloadObject.DOWNLOADING || downloadObject.state == DownloadObject.PENDING)
                    holder.down_icon.setImageResource(R.drawable.ic_download);
                else if (downloadObject.state == DownloadObject.PAUSED)
                    holder.down_icon.setImageResource(R.drawable.ic_pause_normal);
            }
            holder.setState(isNetworkAvailable, object.isChapterDownloaded || object.isDownloading);
        });
        holder.setCastingObserver(fragment, s -> {
            if (object.eid.equals(s)) {
                holder.setCasting(true, object.getFileName());
                holder.streaming.setOnClickListener(v -> CastUtil.get().openControls());
            } else {
                holder.setCasting(false, object.getFileName());
                holder.streaming.setOnClickListener(view -> {
                    if (object.isChapterDownloaded || object.isDownloading) {
                        new MaterialDialog.Builder(context)
                                .content("¿Eliminar el " + object.chapter.toLowerCase() + " de " + object.name + "?")
                                .positiveText("CONFIRMAR")
                                .negativeText("CANCELAR")
                                .onPositive((dialog, which) -> {
                                    FileAccessHelper.INSTANCE.delete(object.getFileName());
                                    //downloadsDAO.deleteByEid(object.eid);
                                    DownloadManager.cancel(object.eid);
                                    QueueManager.remove(object.eid);
                                    object.isChapterDownloaded = false;
                                    holder.setState(isNetworkAvailable, false);
                                }).build().show();
                    } else {
                        ServersFactory.start(context, fragment.getChildFragmentManager(), object.url, DownloadObject.fromRecent(object), true, new ServersFactory.ServersInterface() {
                            @Override
                            public void onFinish(boolean started, boolean success) {
                                if (!started && success) {
                                    chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(object));
                                    recordsDAO.add(RecordObject.fromRecent(object));
                                }
                            }

                            @Override
                            public void onCast(String url) {
                                CastUtil.get().play(fragment.getActivity(), object.eid, url, object.name, object.chapter, object.aid, true);
                                chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(object));
                                recordsDAO.add(RecordObject.fromRecent(object));
                                holder.setSeen(true);
                            }
                        });
                    }
                });
            }
        });
        holder.title.setText(object.name);
        holder.chapter.setText(object.chapter);
        holder.cardView.setOnClickListener(view -> {
            if (object.animeObject != null) {
                ActivityAnime.open(fragment, object.animeObject, holder.imageView);
            } else {
                AnimeObject animeObject = animeDAO.getByAid(object.aid);
                if (animeObject != null) {
                    ActivityAnime.open(fragment, animeObject, holder.imageView);
                } else {
                    Toaster.toast("Aún no esta en directorio!");
                    DirectoryService.run(context);
                }
            }
        });
        holder.cardView.setOnLongClickListener(v -> {
            if (!chaptersDAO.chapterIsSeen(object.eid)) {
                chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(object));
                holder.animeOverlay.setSeen(true, true);
            } else {
                chaptersDAO.deleteChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(object));
                holder.animeOverlay.setSeen(false, true);
            }
            return true;
        });
        holder.download.setOnClickListener(view -> {
            DownloadObject obj = downloadsDAO.getByEid(object.eid);
            if (FileAccessHelper.INSTANCE.canDownload(fragment) &&
                    !object.isChapterDownloaded &&
                    !object.isDownloading &&
                    object.downloadState != DownloadObject.PENDING) {
                holder.setLocked(true);
                ServersFactory.start(context, fragment.getChildFragmentManager(), object.url, AnimeObject.WebInfo.AnimeChapter.fromRecent(object), false, new ServersFactory.ServersInterface() {
                    @Override
                    public void onFinish(boolean started, boolean success) {
                        if (started) {
                            object.isChapterDownloaded = true;
                            holder.setState(isNetworkAvailable, true);
                        } else {
                            holder.setLocked(false);
                        }
                    }

                    @Override
                    public void onCast(String url) {

                    }
                });
            } else if (object.isChapterDownloaded && (obj == null || obj.state == DownloadObject.DOWNLOADING || obj.state == DownloadObject.COMPLETED)) {
                chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(object));
                recordsDAO.add(RecordObject.fromRecent(object));
                holder.setSeen(true);
                ServersFactory.startPlay(context, object.getEpTitle(), object.getFileName());
            } else {
                Toaster.toast("Aun no se está descargando");
            }
        });
        holder.download.setOnLongClickListener(v -> {
            DownloadObject obj = downloadsDAO.getByEid(object.eid);
            if (CastUtil.get().connected() &&
                    object.isChapterDownloaded && (obj == null || obj.state == DownloadObject.COMPLETED)) {
                chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(object));
                CastUtil.get().play(fragment.getActivity(), object.eid, SelfServer.start(object.getFileName(), true), object.name, object.chapter, object.aid, true);
            }
            return true;
        });
    }

    @Override
    public void onViewRecycled(@NonNull ItemHolder holder) {
        holder.unsetChapterObserver();
        holder.unsetDownloadObserver();
        holder.unsetCastingObserver();
        super.onViewRecycled(holder);
    }

    void updateList(List<RecentObject> list) {
        this.isNetworkAvailable = Network.isConnected();
        final boolean wasEmpty = this.list.size() == 0;
        this.list = list;
        recyclerView.post(() -> {
            notifyDataSetChanged();
            if (wasEmpty)
                recyclerView.scheduleLayoutAnimation();
        });
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).key;
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
        @BindView(R.id.chapter)
        TextView chapter;
        @BindView(R.id.streaming)
        Button streaming;
        @BindView(R.id.download)
        Button download;
        @BindView(R.id.seenOverlay)
        SeenAnimeOverlay animeOverlay;
        @BindView(R.id.down_icon)
        ImageView down_icon;
        @BindView(R.id.new_icon)
        ImageView new_icon;
        @BindView(R.id.fav_icon)
        ImageView fav_icon;
        @BindView(R.id.progress)
        ProgressBar progressBar;

        private LiveData<AnimeObject.WebInfo.AnimeChapter> chapterLiveData = new MutableLiveData<>();
        private LiveData<DownloadObject> downloadLiveData = new MutableLiveData<>();

        private Observer<AnimeObject.WebInfo.AnimeChapter> chapterObserver;
        private Observer<DownloadObject> downloadObserver;
        private Observer<String> castingObserver;

        ItemHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setChapterObserver(LiveData<AnimeObject.WebInfo.AnimeChapter> chapterLiveData, LifecycleOwner owner, Observer<AnimeObject.WebInfo.AnimeChapter> observer) {
            this.chapterLiveData = chapterLiveData;
            this.chapterObserver = observer;
            this.chapterLiveData.observe(owner, chapterObserver);
        }

        void unsetChapterObserver() {
            if (chapterObserver != null) {
                chapterLiveData.removeObserver(chapterObserver);
                chapterObserver = null;
            }
        }

        void setDownloadObserver(LiveData<DownloadObject> downloadLiveData, LifecycleOwner owner, Observer<DownloadObject> observer) {
            this.downloadLiveData = downloadLiveData;
            this.downloadObserver = observer;
            this.downloadLiveData.observe(owner, downloadObserver);
        }

        void unsetDownloadObserver() {
            if (downloadObserver != null) {
                downloadLiveData.removeObserver(downloadObserver);
                downloadObserver = null;
            }
        }

        void setCastingObserver(LifecycleOwner owner, Observer<String> observer) {
            this.castingObserver = observer;
            CastUtil.get().getCasting().observe(owner, castingObserver);
        }

        void unsetCastingObserver() {
            if (castingObserver != null) {
                CastUtil.get().getCasting().removeObserver(castingObserver);
                castingObserver = null;
            }
        }

        void setNew(final boolean isNew) {
            new_icon.post(() -> new_icon.setVisibility(isNew ? View.VISIBLE : View.GONE));
        }

        void setFav(final boolean isFav) {
            fav_icon.post(() -> fav_icon.setVisibility(isFav ? View.VISIBLE : View.GONE));
        }

        void setDownloaded(final boolean isDownloaded) {
            down_icon.post(() -> down_icon.setVisibility(isDownloaded ? View.VISIBLE : View.GONE));
        }

        void setSeen(boolean seen) {
            animeOverlay.setSeen(seen, false);
        }

        void setLocked(final boolean locked) {
            streaming.post(() -> streaming.setEnabled(!locked));
            download.post(() -> download.setEnabled(!locked));
        }

        void setCasting(final boolean casting, String file_name) {
            streaming.post(() -> streaming.setText(casting ? "CAST" : FileAccessHelper.INSTANCE.getFile(file_name).exists() ? "ELIMINAR" : "STREAMING"));
        }

        @UiThread
        void setState(final boolean isNetworkAvailable, final boolean existFile) {
            setDownloaded(existFile);
            streaming.post(() -> {
                streaming.setText(existFile ? "ELIMINAR" : "STREAMING");
                if (!existFile) {
                    streaming.setEnabled(isNetworkAvailable);
                } else {
                    streaming.setEnabled(true);
                }
            });
            download.post(() -> {
                download.setEnabled(isNetworkAvailable || existFile);
                download.setText(existFile ? "REPRODUCIR" : "DESCARGA");
            });
        }

        void setDownloadState(DownloadObject object) {
            progressBar.post(() -> {
                if (object != null && PrefsUtil.INSTANCE.showProgress())
                    switch (object.state) {
                        case DownloadObject.PENDING:
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setIndeterminate(true);
                            break;
                        case DownloadObject.DOWNLOADING:
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setIndeterminate(false);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                progressBar.setProgress(object.progress, true);
                            else
                                progressBar.setProgress(object.progress);
                            break;
                        case DownloadObject.PAUSED:
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setIndeterminate(false);
                            break;
                        default:
                            progressBar.setVisibility(View.GONE);
                            break;
                    }
                else progressBar.setVisibility(View.GONE);
            });
        }
    }
}
