package knf.kuma.favorite;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.pojos.FavoriteObject;

public class FavoriteFragment extends BottomFragment {

    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.error)
    LinearLayout error_layout;
    private RecyclerView.LayoutManager manager;
    private FavoriteAdapter adapter;
    private boolean isFirst=true;

    public FavoriteFragment() {
    }

    public static FavoriteFragment get() {
        return new FavoriteFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewModelProviders.of(getActivity()).get(FavoriteViewModel.class).getData(getContext()).observe(this, new Observer<List<FavoriteObject>>() {
            @Override
            public void onChanged(@Nullable List<FavoriteObject> favoriteObjects) {
                if (favoriteObjects==null || favoriteObjects.size()==0){
                    adapter.updateList(new ArrayList<FavoriteObject>());
                    error_layout.post(new Runnable() {
                        @Override
                        public void run() {
                            error_layout.setVisibility(View.VISIBLE);
                        }
                    });
                }else {
                    adapter.updateList(favoriteObjects);
                    if (isFirst) {
                        isFirst = false;
                        recyclerView.scheduleLayoutAnimation();
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(getLayout(),container,false);
        ButterKnife.bind(this,view);
        manager=recyclerView.getLayoutManager();
        adapter=new FavoriteAdapter(this,recyclerView);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @LayoutRes
    private int getLayout(){
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("lay_type","0").equals("0")){
            return R.layout.recycler_favs;
        }else {
            return R.layout.recycler_favs_grid;
        }
    }

    public void onChangeOrder(){
        if (getActivity()!=null)
            ViewModelProviders.of(getActivity()).get(FavoriteViewModel.class).getData(getContext()).observe(this, new Observer<List<FavoriteObject>>() {
                @Override
                public void onChanged(@Nullable List<FavoriteObject> favoriteObjects) {
                    if (favoriteObjects==null || favoriteObjects.size()==0){
                        adapter.updateList(new ArrayList<FavoriteObject>());
                        error_layout.post(new Runnable() {
                            @Override
                            public void run() {
                                error_layout.setVisibility(View.VISIBLE);
                            }
                        });
                    }else {
                        adapter.updateList(favoriteObjects);
                        if (isFirst) {
                            isFirst = false;
                            recyclerView.scheduleLayoutAnimation();
                        }
                    }
                }
            });
    }

    @Override
    public void onReselect() {
        if (manager!=null)
            manager.smoothScrollToPosition(recyclerView,null,0);
    }
}
