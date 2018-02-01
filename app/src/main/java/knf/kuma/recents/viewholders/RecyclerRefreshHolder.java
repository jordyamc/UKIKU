package knf.kuma.recents.viewholders;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.AnimationUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;

/**
 * Created by Jordy on 03/01/2018.
 */

public class RecyclerRefreshHolder {
    @BindView(R.id.recycler)
    public RecyclerView recyclerView;
    @BindView(R.id.refresh)
    public SwipeRefreshLayout refreshLayout;
    private LinearLayoutManager layoutManager;
    public RecyclerRefreshHolder(View view) {
        ButterKnife.bind(this,view);
        layoutManager=new LinearLayoutManager(view.getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(view.getContext(),R.anim.layout_fall_down));
    }

    public void setRecyclerAdapter(final RecyclerView.Adapter adapter){
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(adapter);
            }
        });
    }

    public void scrollToTop(){
        layoutManager.smoothScrollToPosition(recyclerView,null,0);
    }

    public void setRefreshing(final boolean refreshing){
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(refreshing);
            }
        });
    }
}
