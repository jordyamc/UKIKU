package knf.kuma.animeinfo.viewholders;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener;
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.AnimeChaptersAdapter;
import knf.kuma.commons.PatternUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.ChaptersDAO;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.SeeingObject;

/**
 * Created by Jordy on 05/01/2018.
 */

public class AnimeChaptersHolder {
    @BindView(R.id.recycler)
    public RecyclerView recyclerView;
    private Context context;
    private LinearLayoutManager manager;
    private List<AnimeObject.WebInfo.AnimeChapter> chapters = new ArrayList<>();
    private AnimeChaptersAdapter adapter;
    private DragSelectTouchListener touchListener;

    public AnimeChaptersHolder(final Context context, View view) {
        this.context = context;
        ButterKnife.bind(this, view);
        manager = new LinearLayoutManager(view.getContext());
        manager.setSmoothScrollbarEnabled(true);
        recyclerView.setLayoutManager(manager);
        touchListener = new DragSelectTouchListener()
                .withSelectListener(new DragSelectionProcessor(new DragSelectionProcessor.ISelectionHandler() {
                    @Override
                    public Set<Integer> getSelection() {
                        return adapter.getSelection();
                    }

                    @Override
                    public boolean isSelected(int i) {
                        return adapter.getSelection().contains(i);
                    }

                    @Override
                    public void updateSelection(int i, int i1, boolean b, boolean b1) {
                        adapter.selectRange(i, i1, b);
                    }
                }).withStartFinishedListener(new DragSelectionProcessor.ISelectionStartFinishedListener() {
                    @Override
                    public void onSelectionStarted(int i, boolean b) {

                    }

                    @Override
                    public void onSelectionFinished(int i) {
                        new MaterialDialog.Builder(context)
                                .content("¿Como desea marcar " + (adapter.getCountSelected() == 1 ? "este episodio?" : ("estos " + adapter.getCountSelected() + " episodios?")))
                                .positiveText("visto")
                                .negativeText("no visto")
                                .neutralText("cancelar")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        final MaterialDialog d = new MaterialDialog.Builder(context)
                                                .content("Marcando...")
                                                .progress(true, 0)
                                                .cancelable(false)
                                                .build();
                                        d.show();
                                        AsyncTask.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                ChaptersDAO dao = CacheDB.INSTANCE.chaptersDAO();
                                                for (int i : adapter.getSelection()) {
                                                    dao.addChapter(chapters.get(i));
                                                }
                                                SeeingDAO seeingDAO = CacheDB.INSTANCE.seeingDAO();
                                                SeeingObject seeingObject = seeingDAO.getByAid(chapters.get(0).aid);
                                                if (seeingObject != null) {
                                                    seeingObject.chapter = chapters.get(0).number;
                                                    seeingDAO.update(seeingObject);
                                                }
                                                recyclerView.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        adapter.deselectAll();
                                                    }
                                                });
                                                d.dismiss();
                                            }
                                        });
                                    }
                                })
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        final MaterialDialog d = new MaterialDialog.Builder(context)
                                                .content("Marcando...")
                                                .progress(true, 0)
                                                .cancelable(false)
                                                .build();
                                        d.show();
                                        AsyncTask.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                ChaptersDAO dao = CacheDB.INSTANCE.chaptersDAO();
                                                for (int i : adapter.getSelection()) {
                                                    dao.deleteChapter(chapters.get(i));
                                                }
                                                SeeingDAO seeingDAO = CacheDB.INSTANCE.seeingDAO();
                                                SeeingObject seeingObject = seeingDAO.getByAid(chapters.get(0).aid);
                                                if (seeingObject != null) {
                                                    seeingObject.chapter = chapters.get(0).number;
                                                    seeingDAO.update(seeingObject);
                                                }
                                                recyclerView.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        adapter.deselectAll();
                                                    }
                                                });
                                                d.dismiss();
                                            }
                                        });
                                    }
                                })
                                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        adapter.deselectAll();
                                    }
                                })
                                .cancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        adapter.deselectAll();
                                    }
                                }).build().show();
                    }
                }).withMode(DragSelectionProcessor.Mode.Simple))
                .withMaxScrollDistance(32);
    }

    public void setAdapter(Fragment fragment, List<AnimeObject.WebInfo.AnimeChapter> chapters) {
        this.chapters = chapters;
        this.adapter = new AnimeChaptersAdapter(fragment, chapters, touchListener);
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(adapter);
                recyclerView.addOnItemTouchListener(touchListener);
            }
        });
    }

    public AnimeChaptersAdapter getAdapter() {
        return adapter;
    }

    public void goToChapter() {
        if (chapters.size() > 0) {
            AnimeObject.WebInfo.AnimeChapter chapter = CacheDB.INSTANCE.chaptersDAO().getLast(PatternUtil.getEids(chapters));
            if (chapter != null) {
                int position = chapters.indexOf(chapter);
                if (position >= 0)
                    manager.scrollToPositionWithOffset(position, 150);
            }
        }
    }

    public void smoothGoToChapter() {
        if (chapters.size() > 0) {
            AnimeObject.WebInfo.AnimeChapter chapter = CacheDB.INSTANCE.chaptersDAO().getLast(PatternUtil.getEids(chapters));
            if (chapter != null) {
                final int position = chapters.indexOf(chapter);
                if (position >= 0)
                    recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            manager.smoothScrollToPosition(recyclerView, null, position);
                        }
                    });
            }
        }
    }
}
