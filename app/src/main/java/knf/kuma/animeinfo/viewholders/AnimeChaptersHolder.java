package knf.kuma.animeinfo.viewholders;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener;
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.AnimeChaptersAdapter;
import knf.kuma.animeinfo.BottomActionsDialog;
import knf.kuma.commons.PatternUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.ChaptersDAO;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.SeeingObject;

public class AnimeChaptersHolder {
    @BindView(R.id.recycler)
    public RecyclerView recyclerView;
    private Context context;
    private FragmentManager fragmentManager;
    private LinearLayoutManager manager;
    private List<AnimeObject.WebInfo.AnimeChapter> chapters = new ArrayList<>();
    private AnimeChaptersAdapter adapter;
    private DragSelectTouchListener touchListener;

    public AnimeChaptersHolder(final Context context, View view, FragmentManager fragmentManager) {
        this.context = context;
        this.fragmentManager = fragmentManager;
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
                        BottomActionsDialog.newInstance(new BottomActionsDialog.ActionsCallback() {
                            @Override
                            public void onSelect(int state) {
                                final MaterialDialog d = new MaterialDialog.Builder(context)
                                        .content("Marcando...")
                                        .progress(true, 0)
                                        .cancelable(false)
                                        .build();
                                d.show();
                                switch (state) {
                                    case BottomActionsDialog.STATE_SEEN:
                                        AsyncTask.execute(() -> {
                                            ChaptersDAO dao = CacheDB.INSTANCE.chaptersDAO();
                                            for (int i13 : new ArrayList<>(adapter.getSelection())) {
                                                dao.addChapter(chapters.get(i13));
                                            }
                                            SeeingDAO seeingDAO = CacheDB.INSTANCE.seeingDAO();
                                            SeeingObject seeingObject = seeingDAO.getByAid(chapters.get(0).aid);
                                            if (seeingObject != null) {
                                                seeingObject.chapter = chapters.get(0).number;
                                                seeingDAO.update(seeingObject);
                                            }
                                            recyclerView.post(() -> adapter.deselectAll());
                                            safeDismiss(d);
                                        });
                                        break;
                                    case BottomActionsDialog.STATE_UNSEEN:
                                        AsyncTask.execute(() -> {
                                            try {
                                                ChaptersDAO dao = CacheDB.INSTANCE.chaptersDAO();
                                                for (int i12 : new ArrayList<>(adapter.getSelection())) {
                                                    dao.deleteChapter(chapters.get(i12));
                                                }
                                                SeeingDAO seeingDAO = CacheDB.INSTANCE.seeingDAO();
                                                SeeingObject seeingObject = seeingDAO.getByAid(chapters.get(0).aid);
                                                if (seeingObject != null) {
                                                    seeingObject.chapter = chapters.get(0).number;
                                                    seeingDAO.update(seeingObject);
                                                }
                                                recyclerView.post(() -> adapter.deselectAll());
                                                safeDismiss(d);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                if (d.isShowing())
                                                    safeDismiss(d);
                                            }
                                        });
                                        break;
                                }
                            }

                            @Override
                            public void onDismiss() {
                                adapter.deselectAll();
                            }
                        }).show(fragmentManager, "actions_dialog");
                    }
                }).withMode(DragSelectionProcessor.Mode.Simple))
                .withMaxScrollDistance(32);
    }

    public void setAdapter(Fragment fragment, List<AnimeObject.WebInfo.AnimeChapter> chapters) {
        this.chapters = chapters;
        this.adapter = new AnimeChaptersAdapter(fragment, chapters, touchListener);
        recyclerView.post(() -> {
            recyclerView.setAdapter(adapter);
            recyclerView.addOnItemTouchListener(touchListener);
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
                    recyclerView.post(() -> manager.smoothScrollToPosition(recyclerView, null, position));
            }
        }
    }

    private void safeDismiss(MaterialDialog dialog) {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            //
        }
    }
}
