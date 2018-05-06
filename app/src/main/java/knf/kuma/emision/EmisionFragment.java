package knf.kuma.emision;

import android.arch.lifecycle.Observer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.custom.GridRecyclerView;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.widgets.emision.WEmisionProvider;
import pl.droidsonroids.jspoon.Jspoon;

public class EmisionFragment extends Fragment {
    @BindView(R.id.recycler)
    GridRecyclerView recyclerView;
    @BindView(R.id.error)
    View error;
    @BindView(R.id.progress)
    ProgressBar progressBar;
    private AnimeDAO dao = CacheDB.INSTANCE.animeDAO();
    private EmisionAdapter adapter;
    private boolean isFirst = true;

    public EmisionFragment() {
    }

    public static EmisionFragment get(AnimeObject.Day day) {
        Bundle bundle = new Bundle();
        bundle.putInt("day", day.value);
        EmisionFragment fragment = new EmisionFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.recycler_emision, container, false);
        ButterKnife.bind(this, view);
        adapter = new EmisionAdapter(this);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getContext() != null)
            CacheDB.INSTANCE.animeDAO().getByDay(getArguments().getInt("day", 1), getBlacklist()).observe(this, new Observer<List<AnimeObject>>() {
                @Override
                public void onChanged(@Nullable List<AnimeObject> animeObjects) {
                    progressBar.setVisibility(View.GONE);
                    if (isFirst && animeObjects != null && animeObjects.size() != 0) {
                        isFirst = false;
                        adapter.update(animeObjects);
                        recyclerView.scheduleLayoutAnimation();
                        checkStates(animeObjects);
                    }
                    if (animeObjects == null || animeObjects.size() == 0)
                        error.setVisibility(View.VISIBLE);
                }
            });
    }

    private void checkStates(final List<AnimeObject> animeObjects) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    for (AnimeObject animeObject : animeObjects) {
                        try {
                            Document document = Jsoup.connect(animeObject.link).cookie("device", "computer").get();
                            AnimeObject object = new AnimeObject(animeObject.link, Jspoon.create().adapter(AnimeObject.WebInfo.class).fromHtml(document.outerHtml()));
                            if (!object.state.equals("En emisión")) {
                                dao.updateAnime(object);
                                adapter.remove(adapter.list.indexOf(animeObject));
                                WEmisionProvider.update(getContext());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void reloadList() {
        if (getContext() != null)
            CacheDB.INSTANCE.animeDAO().getByDay(getArguments().getInt("day", 1), getBlacklist()).observe(this, new Observer<List<AnimeObject>>() {
                @Override
                public void onChanged(@Nullable List<AnimeObject> animeObjects) {
                    error.setVisibility(View.GONE);
                    if (animeObjects != null && animeObjects.size() != 0)
                        adapter.update(animeObjects);
                    else adapter.update(new ArrayList<AnimeObject>());
                    if (animeObjects == null || animeObjects.size() == 0)
                        error.setVisibility(View.VISIBLE);
                }
            });
    }

    private Set<String> getBlacklist() {
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("show_hidden", false))
            return new LinkedHashSet<>();
        else
            return PreferenceManager.getDefaultSharedPreferences(getContext()).getStringSet("emision_blacklist", new LinkedHashSet<String>());
    }
}
