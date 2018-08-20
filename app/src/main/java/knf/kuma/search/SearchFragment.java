package knf.kuma.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.SearchEvent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.recommended.RankType;
import knf.kuma.recommended.RecommendHelper;

public class SearchFragment extends BottomFragment {
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.progress)
    ProgressBar progressBar;
    @BindView(R.id.error)
    View errorView;

    private SearchViewModel model;
    private SearchAdapter adapter;
    private LinearLayoutManager manager;

    private boolean isFirst = true;

    private String query = "";

    private List<String> selected = new ArrayList<>();

    public SearchFragment() {
    }

    public static SearchFragment get() {
        return get("");
    }

    public static SearchFragment get(@Nullable String query) {
        SearchFragment fragment = new SearchFragment();
        fragment.query = query;
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model = ViewModelProviders.of(getActivity()).get(SearchViewModel.class);
        model.setSearch(query, "", this, animeObjects -> {
            adapter.submitList(animeObjects);
            errorView.setVisibility(animeObjects.size() == 0 ? View.VISIBLE : View.GONE);
            if (isFirst) {
                progressBar.setVisibility(View.GONE);
                isFirst = false;
                recyclerView.scheduleLayoutAnimation();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, view);
        manager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(manager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_fall_down));
        adapter = new SearchAdapter(this);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                if (toPosition == 0)
                    manager.smoothScrollToPosition(recyclerView, null, 0);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                if (positionStart == 0)
                    manager.smoothScrollToPosition(recyclerView, null, 0);
            }
        });
        recyclerView.setAdapter(adapter);
        fab.setOnClickListener(view1 -> {
            GenresDialog dialog = new GenresDialog();
            dialog.init(getGenres(), selected, s -> {
                selected = s;
                setFabIcon();
                setSearch(query);
            });
            dialog.show(getChildFragmentManager(), "genres");
        });
        return view;
    }

    public void setSearch(String q) {
        this.query = q.trim();
        if (model != null)
            model.setSearch(q.trim(), getGenresString(), this, animeObjects -> {
                if (animeObjects != null) {
                    adapter.submitList(animeObjects);
                    errorView.setVisibility(animeObjects.size() == 0 ? View.VISIBLE : View.GONE);
                    Answers.getInstance().logSearch(new SearchEvent().putQuery(query));
                    if (!getGenresString().equals(""))
                        Answers.getInstance().logSearch(new SearchEvent().putQuery(getGenresString()));
                }
                if (isFirst) {
                    progressBar.setVisibility(View.GONE);
                    isFirst = false;
                    recyclerView.scheduleLayoutAnimation();
                }
            });
    }

    private void setFabIcon() {
        fab.post(() -> fab.setImageResource(getFabIcon()));
    }

    @NonNull
    private String getGenresString() {
        if (selected.size() == 0) {
            return "";
        } else {
            RecommendHelper.registerAll(selected, RankType.SEARCH);
            StringBuilder builder = new StringBuilder("%");
            for (String genre : selected) {
                builder.append(genre)
                        .append("%");
            }
            return builder.toString();
        }
    }

    @DrawableRes
    private int getFabIcon() {
        switch (selected.size()) {
            case 0:
                return R.drawable.ic_genres_0;
            case 1:
                return R.drawable.ic_genres_1;
            case 2:
                return R.drawable.ic_genres_2;
            case 3:
                return R.drawable.ic_genres_3;
            case 4:
                return R.drawable.ic_genres_4;
            case 5:
                return R.drawable.ic_genres_5;
            case 6:
                return R.drawable.ic_genres_6;
            case 7:
                return R.drawable.ic_genres_7;
            case 8:
                return R.drawable.ic_genres_8;
            case 9:
                return R.drawable.ic_genres_9;
            default:
                return R.drawable.ic_genres_more;
        }
    }

    private List<String> getGenres() {
        return Arrays.asList(
                "Acción",
                "Artes Marciales",
                "Aventuras",
                "Carreras",
                "Comedia",
                "Demencia",
                "Demonios",
                "Deportes",
                "Drama",
                "Ecchi",
                "Escolares",
                "Espacial",
                "Fantasía",
                "Ciencia Ficción",
                "Harem",
                "Historico",
                "Infantil",
                "Josei",
                "Juegos",
                "Magia",
                "Mecha",
                "Militar",
                "Misterio",
                "Musica",
                "Parodia",
                "Policía",
                "Psicológico",
                "Recuentos de la vida",
                "Romance",
                "Samurai",
                "Seinen",
                "Shoujo",
                "Shounen",
                "Sin Generos",
                "Sobrenatural",
                "Superpoderes",
                "Suspenso",
                "Terror",
                "Vampiros",
                "Yaoi",
                "Yuri");
    }

    @Override
    public void onReselect() {

    }
}
