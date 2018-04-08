package knf.kuma.explorer;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.CastUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.commons.SelfServer;
import knf.kuma.custom.SeenAnimeOverlay;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.ChaptersDAO;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.database.dao.ExplorerDAO;
import knf.kuma.database.dao.RecordsDAO;
import knf.kuma.downloadservice.FileAccessHelper;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.ExplorerObject;
import knf.kuma.pojos.RecordObject;
import knf.kuma.videoservers.ServersFactory;

import static android.provider.MediaStore.Video.Thumbnails.MINI_KIND;

public class ExplorerChapsAdapter extends RecyclerView.Adapter<ExplorerChapsAdapter.ChapItem> {

    private Fragment fragment;
    private Context context;
    private ExplorerObject explorerObject;
    private FragmentChapters.ClearInterface clearInterface;

    private DownloadsDAO downloadsDAO = CacheDB.INSTANCE.downloadsDAO();
    private ChaptersDAO chaptersDAO = CacheDB.INSTANCE.chaptersDAO();
    private RecordsDAO recordsDAO = CacheDB.INSTANCE.recordsDAO();
    private ExplorerDAO explorerDAO=CacheDB.INSTANCE.explorerDAO();

    ExplorerChapsAdapter(Fragment fragment, ExplorerObject explorerObject, FragmentChapters.ClearInterface clearInterface) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.explorerObject = explorerObject;
        this.clearInterface = clearInterface;
    }

    @NonNull
    @Override
    public ChapItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChapItem(LayoutInflater.from(parent.getContext()).inflate(getLayout(), parent, false));
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("lay_type", "0").equals("0")) {
            return R.layout.item_chap;
        } else {
            return R.layout.item_chap_grid;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ChapItem holder, int position) {
        final ExplorerObject.FileDownObj chapObject = explorerObject.chapters.get(position);
        loadThumb(chapObject, holder.imageView);
        chaptersDAO.chapterSeen(chapObject.eid).observe(fragment, new Observer<AnimeObject.WebInfo.AnimeChapter>() {
            @Override
            public void onChanged(@Nullable AnimeObject.WebInfo.AnimeChapter chapter) {
                holder.seenOverlay.setSeen(chapter != null,false);
            }
        });
        holder.chapter.setText(String.format(Locale.getDefault(), "Episodio %s", chapObject.chapter));
        holder.time.setText(chapObject.time);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromDownloaded(chapObject));
                recordsDAO.add(RecordObject.fromDownloaded(chapObject));
                holder.seenOverlay.setSeen(true,true);
                if (CastUtil.get().connected()) {
                    CastUtil.get().play(fragment.getActivity(), chapObject.eid, SelfServer.start(chapObject.fileName), chapObject.title, "Episodio " + chapObject.chapter, chapObject.aid, true);
                } else {
                    ServersFactory.startPlay(context, chapObject.getChapTitle(), chapObject.fileName);
                }
            }
        });
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!chaptersDAO.chapterIsSeen(chapObject.eid)){
                    chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromDownloaded(chapObject));
                    holder.seenOverlay.setSeen(true,true);
                }else {
                    chaptersDAO.deleteChapter(AnimeObject.WebInfo.AnimeChapter.fromDownloaded(chapObject));
                    holder.seenOverlay.setSeen(false,true);
                }
                return true;
            }
        });
        holder.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(context)
                        .content("¿Eliminar el episodio " + chapObject.chapter + " de " + chapObject.title + "?")
                        .positiveText("CONFIRMAR")
                        .negativeText("CANCELAR")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                delete(chapObject, holder.getAdapterPosition());
                            }
                        }).build().show();
            }
        });
    }

    public void setInterface(FragmentChapters.ClearInterface clearInterface) {
        this.clearInterface = clearInterface;
    }

    private void delete(ExplorerObject.FileDownObj obj, final int position) {
        FileAccessHelper.INSTANCE.delete(obj.fileName);
        downloadsDAO.deleteByEid(obj.eid);
        explorerObject.chapters.remove(position);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notifyItemRemoved(position);
            }
        });
        if (explorerObject.chapters.size() == 0) {
            explorerDAO.delete(explorerObject);
            clearInterface.onClear();
        }else {
            explorerObject.count=explorerObject.chapters.size();
            explorerDAO.update(explorerObject);
        }
    }

    private void loadThumb(final ExplorerObject.FileDownObj object, final ImageView imageView) {
        final File file = new File(context.getCacheDir(), explorerObject.fileName + "_" + object.chapter.toLowerCase() + ".png");
        if (file.exists()) {
            PicassoSingle.get(context).load(file).into(imageView);
        } else {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(object.path, MINI_KIND);
                        if (bitmap == null) {
                            throw new IllegalStateException("Null bitmap");
                        } else {
                            file.createNewFile();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    PicassoSingle.get(context).load(file).into(imageView);
                                }
                            });
                        }
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                PicassoSingle.get(context).load(R.drawable.ic_no_thumb).fit().into(imageView);
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        try {
            return explorerObject.chapters.size();
        } catch (Exception e) {
            return 0;
        }
    }

    class ChapItem extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.seen)
        SeenAnimeOverlay seenOverlay;
        @BindView(R.id.chapter)
        TextView chapter;
        @BindView(R.id.time)
        TextView time;
        @BindView(R.id.action)
        ImageButton action;

        public ChapItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
