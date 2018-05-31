package knf.kuma.recents;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.CastUtil;
import knf.kuma.commons.Network;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.commons.SelfServer;
import knf.kuma.custom.SeenAnimeOverlay;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.ChaptersDAO;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.database.dao.FavsDAO;
import knf.kuma.database.dao.RecordsDAO;
import knf.kuma.directory.DirectoryService;
import knf.kuma.downloadservice.FileAccessHelper;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.FavoriteObject;
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
        PicassoSingle.get(context).load(object.img).into(holder.imageView);
        holder.setNew(object.isNew);
        holder.setFav(dao.isFav(Integer.parseInt(object.aid)));
        holder.setSeen(chaptersDAO.chapterIsSeen(object.eid));
        dao.favObserver(Integer.parseInt(object.aid)).observe(fragment, new Observer<FavoriteObject>() {
            @Override
            public void onChanged(@Nullable FavoriteObject object) {
                holder.setFav(object != null);
            }
        });
        chaptersDAO.chapterSeen(object.eid).observe(fragment, new Observer<AnimeObject.WebInfo.AnimeChapter>() {
            @Override
            public void onChanged(@Nullable AnimeObject.WebInfo.AnimeChapter chapter) {
                holder.setSeen(chapter != null);
            }
        });
        downloadsDAO.getLiveByEid(object.eid).observe(fragment, new Observer<DownloadObject>() {
            @Override
            public void onChanged(@Nullable DownloadObject downloadObject) {
                if (downloadObject == null) {
                    object.downloadState = -8;
                    object.isDownloading = false;
                } else {
                    object.isDownloading = (downloadObject.state == DownloadObject.DOWNLOADING || downloadObject.state == DownloadObject.PENDING);
                    object.downloadState = downloadObject.state;
                    File file = FileAccessHelper.INSTANCE.getFile(object.getFileName());
                    object.isChapterDownloaded = file.exists();
                }
                holder.setState(isNetworkAvailable, object.isChapterDownloaded || object.isDownloading);
            }
        });
        CastUtil.get().getCasting().observe(fragment, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (object.eid.equals(s)) {
                    holder.setCasting(true);
                    holder.streaming.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CastUtil.get().openControls();
                        }
                    });
                } else {
                    holder.setCasting(false);
                    holder.streaming.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (object.isChapterDownloaded || object.isDownloading) {
                                new MaterialDialog.Builder(context)
                                        .content("¿Eliminar el " + object.chapter.toLowerCase() + " de " + object.name + "?")
                                        .positiveText("CONFIRMAR")
                                        .negativeText("CANCELAR")
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                FileAccessHelper.INSTANCE.delete(object.getFileName());
                                                downloadsDAO.deleteByEid(object.eid);
                                                QueueManager.remove(object.eid);
                                                object.isChapterDownloaded = false;
                                                holder.setState(isNetworkAvailable, false);
                                            }
                                        }).build().show();
                            } else {
                                ServersFactory.start(context, object.url, DownloadObject.fromRecent(object), true, new ServersFactory.ServersInterface() {
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
                        }
                    });
                }
            }
        });
        holder.title.setText(object.name);
        holder.chapter.setText(object.chapter);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!chaptersDAO.chapterIsSeen(object.eid)) {
                    chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(object));
                    holder.animeOverlay.setSeen(true, true);
                } else {
                    chaptersDAO.deleteChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(object));
                    holder.animeOverlay.setSeen(false, true);
                }
                return true;
            }
        });
        holder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadObject obj = downloadsDAO.getByEid(object.eid);
                if (FileAccessHelper.INSTANCE.canDownload(fragment) &&
                        !object.isChapterDownloaded &&
                        !object.isDownloading &&
                        object.downloadState != DownloadObject.PENDING) {
                    holder.setLocked(true);
                    ServersFactory.start(context, object.url, AnimeObject.WebInfo.AnimeChapter.fromRecent(object), false, new ServersFactory.ServersInterface() {
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
            }
        });
        holder.download.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                DownloadObject obj = downloadsDAO.getByEid(object.eid);
                if (CastUtil.get().connected() &&
                        object.isChapterDownloaded && (obj == null || obj.state == DownloadObject.COMPLETED)) {
                    chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(object));
                    CastUtil.get().play(fragment.getActivity(), object.eid, SelfServer.start(object.getFileName(), true), object.name, object.chapter, object.aid, true);
                }
                return true;
            }
        });
    }

    public void updateList(List<RecentObject> list) {
        this.isNetworkAvailable = Network.isConnected();
        final boolean wasEmpty = this.list.size() == 0;
        this.list = list;
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
                if (wasEmpty)
                    recyclerView.scheduleLayoutAnimation();
            }
        });
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

        ItemHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setNew(final boolean isNew) {
            new_icon.post(new Runnable() {
                @Override
                public void run() {
                    new_icon.setVisibility(isNew ? View.VISIBLE : View.GONE);
                }
            });
        }

        void setFav(final boolean isFav) {
            fav_icon.post(new Runnable() {
                @Override
                public void run() {
                    fav_icon.setVisibility(isFav ? View.VISIBLE : View.GONE);
                }
            });
        }

        void setDownloaded(final boolean isDownloaded) {
            down_icon.post(new Runnable() {
                @Override
                public void run() {
                    down_icon.setVisibility(isDownloaded ? View.VISIBLE : View.GONE);
                }
            });
        }

        void setSeen(boolean seen) {
            animeOverlay.setSeen(seen, false);
        }

        void setLocked(final boolean locked) {
            streaming.post(new Runnable() {
                @Override
                public void run() {
                    streaming.setEnabled(!locked);
                }
            });
            download.post(new Runnable() {
                @Override
                public void run() {
                    download.setEnabled(!locked);
                }
            });
        }

        void setCasting(final boolean casting) {
            streaming.post(new Runnable() {
                @Override
                public void run() {
                    streaming.setText(casting ? "CAST" : "STREAMING");
                }
            });
        }

        @UiThread
        void setState(final boolean isNetworkAvailable, final boolean existFile) {
            setDownloaded(existFile);
            streaming.post(new Runnable() {
                @Override
                public void run() {
                    streaming.setText(existFile ? "ELIMINAR" : "STREAMING");
                    if (!existFile) {
                        streaming.setEnabled(isNetworkAvailable);
                    } else {
                        streaming.setEnabled(true);
                    }
                }
            });
            download.post(new Runnable() {
                @Override
                public void run() {
                    download.setEnabled(isNetworkAvailable || existFile);
                    download.setText(existFile ? "REPRODUCIR" : "DESCARGA");
                }
            });
        }
    }
}
