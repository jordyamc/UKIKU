package knf.kuma.tv.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v17.leanback.app.BrowseSupportFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;
import android.view.View;

import com.dropbox.core.android.Auth;

import knf.kuma.R;
import knf.kuma.backup.BUUtils;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.FavoriteObject;
import knf.kuma.pojos.RecentObject;
import knf.kuma.pojos.RecordObject;
import knf.kuma.retrofit.Repository;
import knf.kuma.tv.AnimeRow;
import knf.kuma.tv.GlideBackgroundManager;
import knf.kuma.tv.TVServersFactory;
import knf.kuma.tv.anime.FavPresenter;
import knf.kuma.tv.anime.RecentsPresenter;
import knf.kuma.tv.anime.RecordPresenter;
import knf.kuma.tv.anime.SyncPresenter;
import knf.kuma.tv.details.TVAnimesDetails;
import knf.kuma.tv.search.TVSearch;
import knf.kuma.tv.sync.LogOutObject;
import knf.kuma.tv.sync.SyncObject;
import xdroid.toaster.Toaster;

public class TVMainFragment extends BrowseSupportFragment implements OnItemViewSelectedListener, OnItemViewClickedListener, View.OnClickListener, BUUtils.LoginInterface {

    private static final int RECENTS = 0;
    private static final int LAST_SEEN = 1;
    private static final int FAVORITES = 2;
    private static final int SYNC = 3;
    SparseArray<AnimeRow> mRows;

    GlideBackgroundManager backgroundManager;

    private boolean waitingLogin = false;

    public static TVMainFragment get() {
        return new TVMainFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        backgroundManager = new GlideBackgroundManager(getActivity());
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setTitle("UKIKU");
        setBrandColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        setSearchAffordanceColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        setOnSearchClickedListener(this);
        createDataRows();
        createRows();
        prepareEntranceTransition();
        fetchData();
        BUUtils.init(getActivity(), this, savedInstanceState == null);
        if (!BUUtils.isLogedIn())
            onLogin();
    }

    private void createDataRows() {
        mRows = new SparseArray<>();
        mRows.put(RECENTS, new AnimeRow()
                .setId(RECENTS)
                .setAdapter(new ArrayObjectAdapter(new RecentsPresenter()))
                .setTitle("Recientes")
                .setPage(1));
        mRows.put(LAST_SEEN, new AnimeRow()
                .setId(LAST_SEEN)
                .setAdapter(new ArrayObjectAdapter(new RecordPresenter()))
                .setTitle("Ultimos vistos")
                .setPage(1));
        mRows.put(FAVORITES, new AnimeRow()
                .setId(FAVORITES)
                .setAdapter(new ArrayObjectAdapter(new FavPresenter()))
                .setTitle("Favoritos")
                .setPage(1));
    }

    private void createRows() {
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        for (int i = 0; i < mRows.size(); i++) {
            AnimeRow row = mRows.get(i);
            HeaderItem headerItem = new HeaderItem(row.getId(), row.getTitle());
            ListRow listRow = new ListRow(headerItem, row.getAdapter());
            rowsAdapter.add(listRow);
        }
        setAdapter(rowsAdapter);
        setOnItemViewSelectedListener(this);
        setOnItemViewClickedListener(this);
    }

    private void fetchData() {
        new Repository().reloadRecents(getContext());
        CacheDB.INSTANCE.recentsDAO().getObjects().observe(getActivity(), recentObjects -> {
            AnimeRow row = mRows.get(RECENTS);
            row.setPage(row.getPage() + 1);
            row.getAdapter().clear();
            for (RecentObject recentObject : recentObjects) {
                row.getAdapter().add(recentObject);
            }
            startEntranceTransition();
        });
        CacheDB.INSTANCE.recordsDAO().getAll().observe(getActivity(), recordObjects -> {
            AnimeRow row = mRows.get(LAST_SEEN);
            row.setPage(row.getPage() + 1);
            row.getAdapter().clear();
            for (RecordObject recordObject : recordObjects) {
                row.getAdapter().add(recordObject);
            }
            startEntranceTransition();
        });
        CacheDB.INSTANCE.favsDAO().getAll().observe(getActivity(), favoriteObjects -> {
            AnimeRow row = mRows.get(FAVORITES);
            row.setPage(row.getPage() + 1);
            row.getAdapter().clear();
            for (FavoriteObject favoriteObject : favoriteObjects) {
                row.getAdapter().add(favoriteObject);
            }
            startEntranceTransition();
        });
    }

    @Override
    public void onClick(View v) {
        TVSearch.start(getContext());
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (item instanceof RecentObject) {
            RecentObject object = (RecentObject) item;
            TVServersFactory.start(getActivity(), object.url, AnimeObject.WebInfo.AnimeChapter.fromRecent(object), ((TVMain) getActivity()));
        } else if (item instanceof RecordObject) {
            RecordObject object = (RecordObject) item;
            if (object.animeObject != null)
                TVAnimesDetails.start(getContext(), object.animeObject.link);
            else Toaster.toast("Anime no encontrado");
        } else if (item instanceof FavoriteObject) {
            FavoriteObject object = (FavoriteObject) item;
            TVAnimesDetails.start(getContext(), object.link);
        } else if (item instanceof SyncObject) {
            if (item instanceof LogOutObject) {
                BUUtils.logOut();
                onLogin();
            } else {
                SyncObject object = (SyncObject) item;
                waitingLogin = true;
                if (object.isDropbox)
                    BUUtils.startClient(BUUtils.BUType.DROPBOX, false);
                else
                    BUUtils.startClient(BUUtils.BUType.DRIVE, false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (waitingLogin) {
            String token = Auth.getOAuth2Token();
            if (token != null)
                BUUtils.setType(BUUtils.BUType.DROPBOX);
            BUUtils.setDropBoxClient(token);
        }
    }

    @Override
    public void onLogin() {
        if (!BUUtils.isLogedIn() && waitingLogin) {
            Toaster.toast("Error al iniciar sesión");
        } else {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new SyncPresenter());
            HeaderItem headerItem = new HeaderItem(SYNC, "Sincronización");
            if (BUUtils.isLogedIn()) {
                adapter.add(new LogOutObject());
                BUUtils.silentRestoreAll();
            } else {
                adapter.add(new SyncObject(true));
                adapter.add(new SyncObject(false));
            }
            if (getAdapter().size() == 3)
                ((ArrayObjectAdapter) getAdapter()).add(SYNC, new ListRow(headerItem, adapter));
            else
                ((ArrayObjectAdapter) getAdapter()).replace(SYNC, new ListRow(headerItem, adapter));
        }
        waitingLogin = false;
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        /*String img=null;
        if (item instanceof RecentObject){
            img=PatternUtil.getCover(((RecentObject)item).aid);
        }else if (item instanceof RecordObject){
            img=PatternUtil.getCover(((RecordObject)item).aid);
        }else if (item instanceof FavoriteObject){
            img=PatternUtil.getCover(((FavoriteObject)item).aid);
        }
        if (img!=null){
            backgroundManager.cancelBackgroundChange();
            backgroundManager.loadImage(img);
        }else
            backgroundManager.setBackground(null);*/
    }
}
