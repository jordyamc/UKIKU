package knf.kuma.search;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.pojos.AnimeObject;
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

    private boolean isFirst=true;

    private String query="";

    private List<String> selected = new ArrayList<>();

    public SearchFragment() {
    }

    public static SearchFragment get() {
        return new SearchFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model=ViewModelProviders.of(getActivity()).get(SearchViewModel.class);
        model.getSearch("", "").observe(this, new Observer<PagedList<AnimeObject>>() {
            @Override
            public void onChanged(@Nullable PagedList<AnimeObject> animeObjects) {
                adapter.submitList(animeObjects);
                errorView.setVisibility(animeObjects.size()==0?View.VISIBLE:View.GONE);
                if (isFirst) {
                    progressBar.setVisibility(View.GONE);
                    isFirst=false;
                    recyclerView.scheduleLayoutAnimation();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, view);
        manager=new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(manager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_fall_down));
        adapter=new SearchAdapter(this);
        recyclerView.setAdapter(adapter);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GenresDialog dialog = new GenresDialog();
                dialog.init(getGenres(), selected, new GenresDialog.MultichoiseListener() {
                    @Override
                    public void onOkay(List<String> s) {
                        selected = s;
                        setFabIcon();
                        setSearch(query);
                    }
                });
                dialog.show(getChildFragmentManager(), "genres");
            }
        });
        return view;
    }

    public void setSearch(String query){
        this.query = query.trim();
        model.getSearch(query.trim(), getGenresString()).observe(this, new Observer<PagedList<AnimeObject>>() {
            @Override
            public void onChanged(@Nullable PagedList<AnimeObject> animeObjects) {
                if (animeObjects!=null) {
                    adapter.submitList(animeObjects);
                    errorView.setVisibility(animeObjects.size() == 0 ? View.VISIBLE : View.GONE);
                }
                if (isFirst) {
                    progressBar.setVisibility(View.GONE);
                    isFirst = false;
                    recyclerView.scheduleLayoutAnimation();
                }else {
                    manager.smoothScrollToPosition(recyclerView, null, 0);
                }
            }
        });
    }

    private void setFabIcon() {
        fab.post(new Runnable() {
            @Override
            public void run() {
                fab.setImageResource(getFabIcon());
            }
        });
    }

    @NonNull
    private String getGenresString(){
        if (selected.size()==0){
            return "";
        }else {
            RecommendHelper.registerAll(selected, RankType.SEARCH);
            StringBuilder builder=new StringBuilder("%");
            for (String genre:selected){
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
