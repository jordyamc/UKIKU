package knf.kuma.recents;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.commons.Network;
import knf.kuma.pojos.RecentObject;
import knf.kuma.recents.viewholders.RecyclerRefreshHolder;

/**
 * Created by Jordy on 03/01/2018.
 */

public class RecentFragment extends BottomFragment implements SwipeRefreshLayout.OnRefreshListener {
    private RecentsViewModel viewModel;
    private RecyclerRefreshHolder holder;
    private RecentsAdapter adapter;

    private boolean isFisrt=Network.isConnected();

    @NonNull
    public static RecentFragment get() {
        return new RecentFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(RecentsViewModel.class);
        viewModel.getDBLiveData().observe(this, new Observer<List<RecentObject>>() {
            @Override
            public void onChanged(@Nullable List<RecentObject> objects) {
                if (objects!=null && !isFisrt){
                    holder.setError(objects.size() == 0);
                    holder.setRefreshing(false);
                    adapter.updateList(objects);
                }else if (isFisrt){
                    isFisrt=false;
                }
            }
        });
        updateList();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recycler_refresh_fragment, container, false);
        holder = new RecyclerRefreshHolder(view);
        holder.refreshLayout.setOnRefreshListener(this);
        adapter = new RecentsAdapter(this, holder.recyclerView);
        holder.recyclerView.setAdapter(adapter);
        holder.setRefreshing(true);
        return view;
    }

    @Override
    public void onRefresh() {
        updateList();
    }

    private void updateList() {
        if (!Network.isConnected()) {
            holder.setRefreshing(false);
        }else {
            viewModel.reload();
        }
    }

    @Override
    public void onReselect() {
        holder.scrollToTop();
    }
}
