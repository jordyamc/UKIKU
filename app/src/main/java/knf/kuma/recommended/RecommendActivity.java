package knf.kuma.recommended;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.FavsDAO;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.GenreStatusObject;
import knf.kuma.recommended.sections.MultipleSection;
import xdroid.toaster.Toaster;

/**
 * Created by jordy on 26/03/2018.
 */

public class RecommendActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.error)
    LinearLayout error;
    @BindView(R.id.loading)
    LinearLayout loading;
    @BindView(R.id.state)
    TextView state;
    private AnimeDAO dao = CacheDB.INSTANCE.animeDAO();
    private FavsDAO favsDAO = CacheDB.INSTANCE.favsDAO();
    private SeeingDAO seeingDAO = CacheDB.INSTANCE.seeingDAO();

    public static void open(Context context) {
        context.startActivity(new Intent(context, RecommendActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(getLayout());
        ButterKnife.bind(this);
        toolbar.setTitle("Sugeridos");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setAdapter();
    }

    @LayoutRes
    private int getLayout() {
        if (isGrid()) {
            return R.layout.recycler_recommends;
        } else {
            return R.layout.recycler_recommends_grid;
        }
    }

    private boolean isGrid() {
        return !PreferenceManager.getDefaultSharedPreferences(this).getString("lay_type", "0").equals("0");
    }

    private void setAdapter() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<GenreStatusObject> status = CacheDB.INSTANCE.genresDAO().getTop();
                    setState("Revisando generos");
                    if (status.size() == 3) {
                        setState("Buscando sugerencias");
                        final SectionedRecyclerViewAdapter sectionedAdapter = new SectionedRecyclerViewAdapter();
                        List<AnimeObject> abc = getList(status.get(0), status.get(1), status.get(2));
                        List<AnimeObject> ab = getList(status.get(0), status.get(1));
                        ab.removeAll(abc);
                        List<AnimeObject> ac = getList(status.get(0), status.get(2));
                        ac.removeAll(abc);
                        ac.removeAll(ab);
                        List<AnimeObject> bc = getList(status.get(1), status.get(2));
                        bc.removeAll(abc);
                        bc.removeAll(ab);
                        bc.removeAll(ac);
                        List<AnimeObject> a = getList(status.get(0));
                        a.removeAll(abc);
                        a.removeAll(ab);
                        a.removeAll(ac);
                        a.removeAll(bc);
                        List<AnimeObject> b = getList(status.get(1));
                        b.removeAll(abc);
                        b.removeAll(ab);
                        b.removeAll(ac);
                        b.removeAll(bc);
                        b.removeAll(a);
                        List<AnimeObject> c = getList(status.get(2));
                        c.removeAll(abc);
                        c.removeAll(ab);
                        c.removeAll(ac);
                        c.removeAll(bc);
                        c.removeAll(a);
                        c.removeAll(b);
                        setState("Filtrando lista");
                        removeFavs(abc, ab, ac, bc, a, b, c);
                        if (abc.size() > 0)
                            sectionedAdapter.addSection(new MultipleSection(RecommendActivity.this, getStringTitle(status.get(0), status.get(1), status.get(2)), abc, isGrid()));
                        if (ab.size() > 0)
                            sectionedAdapter.addSection(new MultipleSection(RecommendActivity.this, getStringTitle(status.get(0), status.get(1)), ab, isGrid()));
                        if (ac.size() > 0)
                            sectionedAdapter.addSection(new MultipleSection(RecommendActivity.this, getStringTitle(status.get(0), status.get(2)), ac, isGrid()));
                        if (bc.size() > 0)
                            sectionedAdapter.addSection(new MultipleSection(RecommendActivity.this, getStringTitle(status.get(1), status.get(2)), bc, isGrid()));
                        if (a.size() > 0)
                            sectionedAdapter.addSection(new MultipleSection(RecommendActivity.this, getStringTitle(status.get(0)), a, isGrid()));
                        if (b.size() > 0)
                            sectionedAdapter.addSection(new MultipleSection(RecommendActivity.this, getStringTitle(status.get(1)), b, isGrid()));
                        if (c.size() > 0)
                            sectionedAdapter.addSection(new MultipleSection(RecommendActivity.this, getStringTitle(status.get(2)), c, isGrid()));
                        final RecyclerView.LayoutManager layoutManager;
                        if (isGrid()) {
                            GridLayoutManager grid = new GridLayoutManager(RecommendActivity.this, getResources().getInteger(R.integer.span_count));
                            grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                                @Override
                                public int getSpanSize(int position) {
                                    switch (sectionedAdapter.getSectionItemViewType(position)) {
                                        case SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER:
                                            return getResources().getInteger(R.integer.span_count);
                                        default:
                                            return 1;
                                    }
                                }
                            });
                            layoutManager = grid;
                        } else {
                            layoutManager = new LinearLayoutManager(RecommendActivity.this);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (recyclerView != null) {
                                    loading.setVisibility(View.GONE);
                                    recyclerView.setLayoutManager(layoutManager);
                                    recyclerView.setAdapter(sectionedAdapter);
                                }
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (error != null) {
                                    loading.setVisibility(View.GONE);
                                    error.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Crashlytics.logException(e);
                    Toaster.toast("Error al cargar recomendados");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (loading != null)
                                loading.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    @SafeVarargs
    private final void removeFavs(List<AnimeObject>... lists) {
        for (List<AnimeObject> list : lists) {
            List<AnimeObject> removeList = new ArrayList<>();
            for (AnimeObject object : list)
                if (favsDAO.isFav(object.key) || seeingDAO.isSeeing(object.aid))
                    removeList.add(object);
            list.removeAll(removeList);
        }
    }

    private List<AnimeObject> getList(GenreStatusObject... status) {
        List<AnimeObject> objects = dao.getByGenres(getString(status));
        Collections.sort(objects);
        return objects;
    }

    private String getString(GenreStatusObject... status) {
        StringBuilder builder = new StringBuilder("%");
        for (GenreStatusObject s : status) {
            builder.append(s.getName())
                    .append("%");
        }
        return builder.toString();
    }

    private String getStringTitle(GenreStatusObject... status) {
        StringBuilder builder = new StringBuilder();
        for (GenreStatusObject s : status) {
            builder.append(s.getName())
                    .append(", ");
        }
        return builder.toString().substring(0, builder.length() - 2);
    }

    private void setState(final String stateString) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state != null)
                    state.setText(stateString);
            }
        });
    }

    private void showBlacklist() {
        List<String> blacklist = GenreStatusObject.getNames(CacheDB.INSTANCE.genresDAO().getBlacklist());
        BlacklistDialog dialog = new BlacklistDialog();
        dialog.init(blacklist, new BlacklistDialog.MultiChoiceListener() {
            @Override
            public void onOkay(List<String> selected) {
                setBlacklist(selected);
            }
        });
        dialog.show(getSupportFragmentManager(), "Blacklist");
    }

    private void setBlacklist(final List<String> selected) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                for (String s : selected)
                    RecommendHelper.block(s);
                for (GenreStatusObject object : CacheDB.INSTANCE.genresDAO().getAll())
                    if (object.isBlocked() && !selected.contains(object.name))
                        RecommendHelper.reset(object.name);
                resetSuggestions();
            }
        });
    }

    private void resetSuggestions() {
        setState("Iniciando búsqueda");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SectionedRecyclerViewAdapter adapter = ((SectionedRecyclerViewAdapter) recyclerView.getAdapter());
                if (adapter != null) {
                    adapter.removeAllSections();
                    recyclerView.setAdapter(adapter);
                    loading.setVisibility(View.VISIBLE);
                    error.setVisibility(View.GONE);
                    setAdapter();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_suggestions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.blacklist:
                showBlacklist();
                break;
            case R.id.rating:
                RankingActivity.open(this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 4321)
            resetSuggestions();
    }
}
