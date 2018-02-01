package knf.kuma.directory;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.BottomFragment;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.R;

/**
 * Created by Jordy on 06/01/2018.
 */

public class DirectoryPageFragment extends BottomFragment {
    public static DirectoryPageFragment get(DirType type){
        Bundle bundle=new Bundle();
        bundle.putInt("type",type.value);
        DirectoryPageFragment fragment=new DirectoryPageFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public DirectoryPageFragment() {
    }

    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.progress)
    ProgressBar progress;
    private RecyclerView.LayoutManager manager;
    private DirectorypageAdapter adapter;
    private boolean isFirst=true;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        DirectoryViewModel model= ViewModelProviders.of(getActivity()).get(DirectoryViewModel.class);
        adapter=new DirectorypageAdapter(this);
        switch (getArguments().getInt("type",0)){
            case 0:
                model.getAnimes(getContext()).observe(this, new Observer<PagedList<AnimeObject>>() {
                    @Override
                    public void onChanged(@Nullable PagedList<AnimeObject> animeObjects) {
                        hideProgress();
                        adapter.setList(animeObjects);
                        makeAnimation();
                    }
                });
                break;
            case 1:
                model.getOvas(getContext()).observe(this, new Observer<PagedList<AnimeObject>>() {
                    @Override
                    public void onChanged(@Nullable PagedList<AnimeObject> animeObjects) {
                        hideProgress();
                        adapter.setList(animeObjects);
                        makeAnimation();
                    }
                });
                break;
            case 2:
                model.getMovies(getContext()).observe(this, new Observer<PagedList<AnimeObject>>() {
                    @Override
                    public void onChanged(@Nullable PagedList<AnimeObject> animeObjects) {
                        hideProgress();
                        adapter.setList(animeObjects);
                        makeAnimation();
                    }
                });
                break;
        }
        recyclerView.setAdapter(adapter);
    }

    public void onChangeOrder(){
        if (getActivity()!=null){
            DirectoryViewModel model= ViewModelProviders.of(getActivity()).get(DirectoryViewModel.class);
            adapter=new DirectorypageAdapter(this);
            switch (getArguments().getInt("type",0)){
                case 0:
                    model.getAnimes(getContext()).observe(this, new Observer<PagedList<AnimeObject>>() {
                        @Override
                        public void onChanged(@Nullable PagedList<AnimeObject> animeObjects) {
                            hideProgress();
                            adapter.setList(animeObjects);
                            makeAnimation();
                        }
                    });
                    break;
                case 1:
                    model.getOvas(getContext()).observe(this, new Observer<PagedList<AnimeObject>>() {
                        @Override
                        public void onChanged(@Nullable PagedList<AnimeObject> animeObjects) {
                            hideProgress();
                            adapter.setList(animeObjects);
                            makeAnimation();
                        }
                    });
                    break;
                case 2:
                    model.getMovies(getContext()).observe(this, new Observer<PagedList<AnimeObject>>() {
                        @Override
                        public void onChanged(@Nullable PagedList<AnimeObject> animeObjects) {
                            hideProgress();
                            adapter.setList(animeObjects);
                            makeAnimation();
                        }
                    });
                    break;
            }
            recyclerView.setAdapter(adapter);
        }
    }

    private void hideProgress(){
        progress.post(new Runnable() {
            @Override
            public void run() {
                progress.setVisibility(View.GONE);
            }
        });
    }

    private void makeAnimation(){
        if (isFirst){
            recyclerView.scheduleLayoutAnimation();
            isFirst=false;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(getLayout(),container,false);
        ButterKnife.bind(this,view);
        manager=recyclerView.getLayoutManager();
        recyclerView.setLayoutManager(manager);
        isFirst=true;
        return view;
    }

    enum DirType{
        ANIMES(0),OVAS(1),MOVIES(2);
        public int value;
        DirType(int value){
            this.value=value;
        }
    }

    @LayoutRes
    private int getLayout(){
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("lay_type","0").equals("0")){
            return R.layout.recycler_dir;
        }else {
            return R.layout.recycler_dir_grid;
        }
    }

    @Override
    public void onReselect() {
        manager.smoothScrollToPosition(recyclerView,null,0);
    }
}
